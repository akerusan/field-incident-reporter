package com.akerusan.fieldincidentreporter.feature.incidentdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akerusan.fieldincidentreporter.core.designsystem.component.IncidentPhotoImage
import com.akerusan.fieldincidentreporter.core.designsystem.component.SeverityPill
import com.akerusan.fieldincidentreporter.core.designsystem.component.StatusPill
import com.akerusan.fieldincidentreporter.core.designsystem.component.SyncPill
import com.akerusan.fieldincidentreporter.core.designsystem.format.toDateTimeText
import com.akerusan.fieldincidentreporter.core.designsystem.label.labelRes
import com.akerusan.fieldincidentreporter.core.designsystem.preview.PreviewIncidents
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.designsystem.util.ObserveAsEvents
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto

/** The incident detail with its ViewModel. Closes itself once the incident is gone (e.g. deleted). */
@Composable
fun IncidentDetailRoute(
    incidentId: String,
    onBack: () -> Unit,
    onEdit: (incidentId: String) -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel<IncidentDetailViewModel, IncidentDetailViewModel.Factory>(
        key = incidentId,
        creationCallback = { factory -> factory.create(incidentId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    ObserveAsEvents(viewModel.messages) { message ->
        val text = when (message) {
            IncidentDetailMessage.CANNOT_DELETE_RESOLVED -> R.string.incident_detail_message_read_only
            IncidentDetailMessage.DISCARD_NEEDS_CONNECTION -> R.string.incident_detail_message_needs_connection
            IncidentDetailMessage.CHANGES_DISCARDED -> R.string.incident_detail_message_discarded
        }
        snackbarHostState.showSnackbar(resources.getString(text))
    }

    if (uiState == IncidentDetailUiState.Gone) {
        LaunchedEffect(Unit) { onBack() }
    }

    IncidentDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = { onEdit(incidentId) },
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onRetry = viewModel::onRetry,
        onDiscardChanges = viewModel::onDiscardChanges,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IncidentDetailScreen(
    uiState: IncidentDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onRetry: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val content = uiState as? IncidentDetailUiState.Content

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.incident_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.incident_detail_back),
                        )
                    }
                },
                actions = {
                    if (content?.canModify == true) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.incident_detail_edit),
                            )
                        }
                    }
                    if (content?.canModify == true) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.incident_detail_delete),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (uiState) {
                IncidentDetailUiState.Loading, IncidentDetailUiState.Gone ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is IncidentDetailUiState.Content -> IncidentDetails(
                    content = uiState,
                    onRetry = onRetry,
                    onDiscardChanges = onDiscardChanges,
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.incident_detail_delete_dialog_title)) },
            text = { Text(stringResource(R.string.incident_detail_delete_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteConfirmed()
                }) { Text(stringResource(R.string.incident_detail_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.incident_detail_cancel))
                }
            },
        )
    }
}

@Composable
private fun IncidentDetails(
    content: IncidentDetailUiState.Content,
    onRetry: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    val incident = content.incident
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (incident.photos.isNotEmpty()) PhotoPager(incident.photos)

        Text(text = incident.title, style = MaterialTheme.typography.headlineSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SeverityPill(incident.severity)
            StatusPill(incident.status)
            SyncPill(incident)
        }

        SyncStatusCard(
            incident = incident,
            isDiscarding = content.isDiscarding,
            onRetry = onRetry,
            onDiscardChanges = onDiscardChanges,
        )
        if (incident.isReadOnly) {
            NoticeCard(text = stringResource(R.string.incident_detail_read_only))
        }

        Field(
            label = stringResource(R.string.incident_detail_description),
            value = incident.description.ifBlank { stringResource(R.string.incident_detail_no_description) },
        )
        if (incident.locationNote.isNotBlank()) {
            Field(label = stringResource(R.string.incident_detail_location), value = incident.locationNote)
        }
        Field(
            label = stringResource(R.string.incident_detail_category),
            value = stringResource(incident.category.labelRes),
        )
        Field(label = stringResource(R.string.incident_detail_reported), value = incident.reportedAt.toDateTimeText())
        if (incident.updatedAt != incident.reportedAt) {
            Field(label = stringResource(R.string.incident_detail_updated), value = incident.updatedAt.toDateTimeText())
        }
    }
}

@Composable
private fun PhotoPager(photos: List<IncidentPhoto>) {
    val pagerState = rememberPagerState { photos.size }
    Box {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) { page ->
            IncidentPhotoImage(photo = photos[page], modifier = Modifier.fillMaxSize())
        }
        if (photos.size > 1) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(
                    text = stringResource(
                        R.string.incident_detail_photo_position,
                        pagerState.currentPage + 1,
                        photos.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NoticeCard(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "Failed")
@Preview(name = "Failed, dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncidentDetailFailedPreview() {
    DetailPreview(PreviewIncidents.failed)
}

@Preview(name = "Resolved")
@Composable
private fun IncidentDetailResolvedPreview() {
    DetailPreview(PreviewIncidents.resolved)
}

@Composable
private fun DetailPreview(incident: Incident) {
    FieldIncidentReporterTheme(dynamicColor = false) {
        IncidentDetailScreen(
            uiState = IncidentDetailUiState.Content(incident),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onEdit = {},
            onDeleteConfirmed = {},
            onRetry = {},
            onDiscardChanges = {},
        )
    }
}
