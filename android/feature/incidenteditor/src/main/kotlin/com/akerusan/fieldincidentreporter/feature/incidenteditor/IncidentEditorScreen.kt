package com.akerusan.fieldincidentreporter.feature.incidenteditor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akerusan.fieldincidentreporter.core.designsystem.label.labelRes
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.designsystem.util.ObserveAsEvents
import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.domain.usecase.ValidateIncidentDraftUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus

/** The editor with its ViewModel. A null [incidentId] reports a new incident. */
@Composable
fun IncidentEditorRoute(
    incidentId: String?,
    onClose: () -> Unit,
    viewModel: IncidentEditorViewModel = hiltViewModel<IncidentEditorViewModel, IncidentEditorViewModel.Factory>(
        key = incidentId ?: "new",
        creationCallback = { factory -> factory.create(incidentId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // The system camera writes to a URI we provide, so no camera permission is needed.
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onPhotoCaptured(success)
    }
    // The system photo picker: no storage permission, and the user picks only what they share.
    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ValidateIncidentDraftUseCase.MAX_PHOTOS),
    ) { uris -> viewModel.onPhotosPicked(uris.map { it.toString() }) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is IncidentEditorEvent.LaunchCamera -> takePicture.launch(event.captureUri.toUri())

            IncidentEditorEvent.Close -> onClose()

            IncidentEditorEvent.PhotoUnreadable ->
                snackbarHostState.showSnackbar(resources.getString(R.string.incident_editor_message_photo_unreadable))

            IncidentEditorEvent.PhotoLimitReached ->
                snackbarHostState.showSnackbar(
                    resources.getString(
                        R.string.incident_editor_message_photo_limit,
                        ValidateIncidentDraftUseCase.MAX_PHOTOS,
                    ),
                )

            IncidentEditorEvent.ReadOnly ->
                snackbarHostState.showSnackbar(resources.getString(R.string.incident_editor_message_read_only))

            IncidentEditorEvent.Deleted ->
                snackbarHostState.showSnackbar(resources.getString(R.string.incident_editor_message_deleted))
        }
    }

    // Intercepts back (including the predictive back gesture) only when there is something to lose.
    BackHandler(enabled = uiState.hasUnsavedChanges, onBack = viewModel::onBack)

    IncidentEditorScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = IncidentEditorActions(
            onClose = viewModel::onBack,
            onSave = viewModel::onSave,
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onLocationChange = viewModel::onLocationChange,
            onCategorySelect = viewModel::onCategorySelect,
            onSeveritySelect = viewModel::onSeveritySelect,
            onStatusSelect = viewModel::onStatusSelect,
            onTakePhoto = viewModel::onTakePhoto,
            onChoosePhotos = {
                pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = viewModel::onRemovePhoto,
            onDiscardConfirm = viewModel::onDiscardConfirm,
            onDiscardDismiss = viewModel::onDiscardDismiss,
        ),
    )
}

/** The editor's callbacks, grouped to keep the screen's signature readable. */
internal data class IncidentEditorActions(
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onLocationChange: (String) -> Unit = {},
    val onCategorySelect: (IncidentCategory) -> Unit = {},
    val onSeveritySelect: (IncidentSeverity) -> Unit = {},
    val onStatusSelect: (IncidentStatus) -> Unit = {},
    val onTakePhoto: () -> Unit = {},
    val onChoosePhotos: () -> Unit = {},
    val onRemovePhoto: (IncidentPhoto) -> Unit = {},
    val onDiscardConfirm: () -> Unit = {},
    val onDiscardDismiss: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IncidentEditorScreen(
    uiState: IncidentEditorUiState,
    snackbarHostState: SnackbarHostState,
    actions: IncidentEditorActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isNewIncident) {
                                R.string.incident_editor_title_new
                            } else {
                                R.string.incident_editor_title_edit
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.incident_editor_close))
                    }
                },
                actions = {
                    TextButton(onClick = actions.onSave, enabled = !uiState.isSaving && !uiState.isLoading) {
                        Text(stringResource(R.string.incident_editor_save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                EditorForm(uiState = uiState, actions = actions)
            }
        }
    }

    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = actions.onDiscardDismiss,
            title = { Text(stringResource(R.string.incident_editor_discard_title)) },
            text = { Text(stringResource(R.string.incident_editor_discard_message)) },
            confirmButton = {
                TextButton(onClick = actions.onDiscardConfirm) {
                    Text(stringResource(R.string.incident_editor_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDiscardDismiss) {
                    Text(stringResource(R.string.incident_editor_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun EditorForm(uiState: IncidentEditorUiState, actions: IncidentEditorActions) {
    val draft = uiState.draft
    val errors = uiState.errors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = actions.onTitleChange,
            label = { Text(stringResource(R.string.incident_editor_field_title)) },
            singleLine = true,
            isError = DraftError.TITLE_MISSING in errors || DraftError.TITLE_TOO_LONG in errors,
            supportingText = {
                FieldSupportingText(
                    error = errors.firstOrNull { it == DraftError.TITLE_MISSING || it == DraftError.TITLE_TOO_LONG },
                    length = draft.title.length,
                    maxLength = ValidateIncidentDraftUseCase.MAX_TITLE_LENGTH,
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        CategoryField(
            selected = draft.category,
            onSelect = actions.onCategorySelect,
            error = errors.textFor(DraftError.CATEGORY_MISSING),
        )

        ChoiceField(
            label = stringResource(R.string.incident_editor_field_severity),
            options = IncidentSeverity.entries,
            selected = draft.severity,
            onSelect = actions.onSeveritySelect,
            optionLabel = { stringResource(it.labelRes) },
            error = errors.textFor(DraftError.SEVERITY_MISSING),
        )

        if (!uiState.isNewIncident) {
            ChoiceField(
                label = stringResource(R.string.incident_editor_field_status),
                options = IncidentStatus.technicianSelectable,
                selected = draft.status,
                onSelect = actions.onStatusSelect,
                optionLabel = { stringResource(it.labelRes) },
                error = errors.textFor(DraftError.STATUS_NOT_ALLOWED),
            )
        }

        OutlinedTextField(
            value = draft.locationNote,
            onValueChange = actions.onLocationChange,
            label = { Text(stringResource(R.string.incident_editor_field_location)) },
            placeholder = { Text(stringResource(R.string.incident_editor_field_location_hint)) },
            singleLine = true,
            isError = DraftError.LOCATION_TOO_LONG in errors,
            supportingText = errors.textFor(DraftError.LOCATION_TOO_LONG)?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = draft.description,
            onValueChange = actions.onDescriptionChange,
            label = { Text(stringResource(R.string.incident_editor_field_description)) },
            minLines = 3,
            isError = DraftError.DESCRIPTION_TOO_LONG in errors,
            supportingText = {
                FieldSupportingText(
                    error = DraftError.DESCRIPTION_TOO_LONG.takeIf { it in errors },
                    length = draft.description.length,
                    maxLength = ValidateIncidentDraftUseCase.MAX_DESCRIPTION_LENGTH,
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        PhotoSection(
            photos = draft.photos,
            canAddPhoto = uiState.canAddPhoto,
            isImporting = uiState.isImportingPhoto,
            error = errors.textFor(DraftError.TOO_MANY_PHOTOS),
            onTakePhoto = actions.onTakePhoto,
            onChoosePhotos = actions.onChoosePhotos,
            onRemovePhoto = actions.onRemovePhoto,
        )
    }
}

@Composable
private fun FieldSupportingText(error: DraftError?, length: Int, maxLength: Int) {
    if (error != null) {
        Text(errorText(error))
    } else {
        Text(
            text = stringResource(R.string.incident_editor_character_count, length, maxLength),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun <T> ChoiceField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) { Text(optionLabel(option), maxLines = 1) }
            }
        }
        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The message for [error] if it is one of these errors, else null. */
@Composable
private fun Set<DraftError>.textFor(error: DraftError): String? = if (error in this) errorText(error) else null

@Composable
internal fun errorText(error: DraftError): String = when (error) {
    DraftError.TITLE_MISSING -> stringResource(R.string.incident_editor_error_title_missing)

    DraftError.TITLE_TOO_LONG ->
        limitText(R.plurals.incident_editor_error_title_too_long, ValidateIncidentDraftUseCase.MAX_TITLE_LENGTH)

    DraftError.DESCRIPTION_TOO_LONG ->
        limitText(
            R.plurals.incident_editor_error_description_too_long,
            ValidateIncidentDraftUseCase.MAX_DESCRIPTION_LENGTH,
        )

    DraftError.LOCATION_TOO_LONG ->
        limitText(R.plurals.incident_editor_error_location_too_long, ValidateIncidentDraftUseCase.MAX_LOCATION_LENGTH)

    DraftError.CATEGORY_MISSING -> stringResource(R.string.incident_editor_error_category_missing)

    DraftError.SEVERITY_MISSING -> stringResource(R.string.incident_editor_error_severity_missing)

    DraftError.STATUS_NOT_ALLOWED -> stringResource(R.string.incident_editor_error_status_not_allowed)

    DraftError.TOO_MANY_PHOTOS ->
        limitText(R.plurals.incident_editor_error_too_many_photos, ValidateIncidentDraftUseCase.MAX_PHOTOS)
}

@Composable
private fun limitText(@PluralsRes id: Int, limit: Int): String = pluralStringResource(id, limit, limit)

@Preview(name = "New, with errors")
@Preview(name = "New, with errors, dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncidentEditorNewPreview() {
    FieldIncidentReporterTheme(dynamicColor = false) {
        IncidentEditorScreen(
            uiState = IncidentEditorUiState(
                isNewIncident = true,
                errors = setOf(DraftError.TITLE_MISSING, DraftError.CATEGORY_MISSING, DraftError.SEVERITY_MISSING),
            ),
            snackbarHostState = SnackbarHostState(),
            actions = IncidentEditorActions(),
        )
    }
}
