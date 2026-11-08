package com.akerusan.fieldincidentreporter.feature.incidenteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akerusan.fieldincidentreporter.core.domain.result.SaveIncidentResult
import com.akerusan.fieldincidentreporter.core.domain.usecase.AddPhotoUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.CreateCaptureTargetUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.DiscardDraftUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ReportIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.UpdateIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ValidateIncidentDraftUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Creates or edits an incident. The draft, the version it started from and a pending camera
 * capture are kept in [SavedStateHandle], so nothing is lost if the system kills the app while the
 * camera is open or the app is in the background.
 */
@HiltViewModel(assistedFactory = IncidentEditorViewModel.Factory::class)
class IncidentEditorViewModel @AssistedInject constructor(
    @Assisted private val incidentId: String?,
    private val savedStateHandle: SavedStateHandle,
    private val observeIncident: ObserveIncidentUseCase,
    private val reportIncident: ReportIncidentUseCase,
    private val updateIncident: UpdateIncidentUseCase,
    private val validateDraft: ValidateIncidentDraftUseCase,
    private val addPhoto: AddPhotoUseCase,
    private val createCaptureTarget: CreateCaptureTargetUseCase,
    private val discardDraft: DiscardDraftUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(incidentId: String?): IncidentEditorViewModel
    }

    private val _uiState = MutableStateFlow(IncidentEditorUiState(isNewIncident = incidentId == null))
    val uiState: StateFlow<IncidentEditorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<IncidentEditorEvent>(Channel.BUFFERED)
    val events: Flow<IncidentEditorEvent> = eventChannel.receiveAsFlow()

    /**
     * The content the editor started from: empty for a new report, the saved incident for an edit.
     * Read from saved state once; every change is written back for process death.
     */
    private var original: IncidentDraft? =
        savedStateHandle.get<String>(KEY_ORIGINAL)?.let { Json.decodeFromString<IncidentDraft>(it) }
        set(value) {
            field = value
            savedStateHandle[KEY_ORIGINAL] = value?.let { Json.encodeToString(it) }
        }

    private var showErrors: Boolean
        get() = savedStateHandle[KEY_SHOW_ERRORS] ?: false
        set(value) {
            savedStateHandle[KEY_SHOW_ERRORS] = value
        }

    init {
        val restoredDraft = savedStateHandle.get<String>(KEY_DRAFT)?.let { Json.decodeFromString<IncidentDraft>(it) }
        when {
            restoredDraft != null -> setDraft(restoredDraft)

            incidentId == null -> {
                original = IncidentDraft()
                setDraft(IncidentDraft())
            }

            else -> loadIncident(incidentId)
        }
    }

    private fun loadIncident(id: String) {
        viewModelScope.launch {
            val incident = observeIncident(id).first()
            when {
                incident == null -> eventChannel.send(IncidentEditorEvent.Close)

                incident.isReadOnly -> {
                    eventChannel.send(IncidentEditorEvent.ReadOnly)
                    eventChannel.send(IncidentEditorEvent.Close)
                }

                else -> {
                    val draft = IncidentDraft.from(incident)
                    original = draft
                    setDraft(draft)
                }
            }
        }
    }

    // --- Field changes ---------------------------------------------------------------------------

    fun onTitleChange(title: String) = updateDraft { it.copy(title = title) }

    fun onDescriptionChange(description: String) = updateDraft { it.copy(description = description) }

    fun onLocationChange(locationNote: String) = updateDraft { it.copy(locationNote = locationNote) }

    fun onCategorySelect(category: IncidentCategory) = updateDraft { it.copy(category = category) }

    fun onSeveritySelect(severity: IncidentSeverity) = updateDraft { it.copy(severity = severity) }

    fun onStatusSelect(status: IncidentStatus) = updateDraft { it.copy(status = status) }

    // --- Photos ----------------------------------------------------------------------------------

    fun onTakePhoto() {
        if (!_uiState.value.canAddPhoto) return
        viewModelScope.launch {
            val captureUri = createCaptureTarget()
            savedStateHandle[KEY_CAPTURE_URI] = captureUri
            eventChannel.send(IncidentEditorEvent.LaunchCamera(captureUri))
        }
    }

    /** The camera app returned. The URI comes from saved state: the app may have been recreated meanwhile. */
    fun onPhotoCaptured(success: Boolean) {
        val captureUri = savedStateHandle.get<String>(KEY_CAPTURE_URI) ?: return
        savedStateHandle.remove<String>(KEY_CAPTURE_URI)
        if (success) importPhotos(listOf(captureUri))
    }

    /** The picker allows the maximum; anything beyond the slots left is dropped, and the user is told. */
    fun onPhotosPicked(uris: List<String>) {
        val remaining = _uiState.value.remainingPhotoSlots
        if (uris.size > remaining) viewModelScope.launch { eventChannel.send(IncidentEditorEvent.PhotoLimitReached) }
        importPhotos(uris.take(remaining))
    }

    private fun importPhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingPhoto = true) }
            try {
                for (uri in uris) {
                    val photo = addPhoto(uri)
                    if (photo == null) {
                        eventChannel.send(IncidentEditorEvent.PhotoUnreadable)
                    } else {
                        updateDraft { it.copy(photos = it.photos + photo) }
                    }
                }
            } finally {
                _uiState.update { it.copy(isImportingPhoto = false) }
            }
        }
    }

    /** Removes a photo from the draft. A photo added in this session is deleted right away. */
    fun onRemovePhoto(photo: IncidentPhoto) {
        updateDraft { draft -> draft.copy(photos = draft.photos.filterNot { it.id == photo.id }) }
        viewModelScope.launch {
            discardDraft(IncidentDraft(photos = listOf(photo)), savedPhotoIds = savedPhotoIds())
        }
    }

    // --- Save and leave --------------------------------------------------------------------------

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isImportingPhoto) return
        showErrors = true
        val errors = validateDraft(state.draft)
        _uiState.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = when (incidentId) {
                null -> reportIncident(state.draft)
                else -> updateIncident(incidentId, state.draft)
            }
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is SaveIncidentResult.Saved -> eventChannel.send(IncidentEditorEvent.Close)

                is SaveIncidentResult.Invalid -> _uiState.update { it.copy(errors = result.errors) }

                // The incident was deleted or resolved while the editor was open: the changes can't be
                // saved, so the photos added in this session are deleted and the user is told why.
                SaveIncidentResult.NotFound -> leaveUnsaved(state.draft, IncidentEditorEvent.Deleted)

                SaveIncidentResult.ReadOnly -> leaveUnsaved(state.draft, IncidentEditorEvent.ReadOnly)
            }
        }
    }

    /** Back or close: leave right away, or ask first if there are unsaved changes. */
    fun onBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { eventChannel.send(IncidentEditorEvent.Close) }
        }
    }

    fun onDiscardDismiss() = _uiState.update { it.copy(showDiscardDialog = false) }

    fun onDiscardConfirm() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch {
            discardDraft(_uiState.value.draft, savedPhotoIds = savedPhotoIds())
            eventChannel.send(IncidentEditorEvent.Close)
        }
    }

    private suspend fun leaveUnsaved(draft: IncidentDraft, reason: IncidentEditorEvent) {
        discardDraft(draft, savedPhotoIds = savedPhotoIds())
        eventChannel.send(reason)
        eventChannel.send(IncidentEditorEvent.Close)
    }

    // --- State -----------------------------------------------------------------------------------

    private fun savedPhotoIds(): Set<String> = original?.photos.orEmpty().map { it.id }.toSet()

    private fun updateDraft(transform: (IncidentDraft) -> IncidentDraft) = setDraft(transform(_uiState.value.draft))

    private fun setDraft(draft: IncidentDraft) {
        savedStateHandle[KEY_DRAFT] = Json.encodeToString(draft)
        _uiState.update {
            it.copy(
                isLoading = false,
                draft = draft,
                errors = if (showErrors) validateDraft(draft) else emptySet(),
                hasUnsavedChanges = draft != original,
            )
        }
    }

    private companion object {
        const val KEY_DRAFT = "draft"
        const val KEY_ORIGINAL = "original"
        const val KEY_SHOW_ERRORS = "showErrors"
        const val KEY_CAPTURE_URI = "captureUri"
    }
}
