package com.akerusan.fieldincidentreporter.feature.incidenteditor

import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.domain.usecase.ValidateIncidentDraftUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft

data class IncidentEditorUiState(
    val isNewIncident: Boolean,
    val isLoading: Boolean = !isNewIncident,
    val draft: IncidentDraft = IncidentDraft(),
    /** Validation problems, shown once the user has tried to save. */
    val errors: Set<DraftError> = emptySet(),
    val isSaving: Boolean = false,
    val isImportingPhoto: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val showDiscardDialog: Boolean = false,
) {
    val remainingPhotoSlots: Int get() = (ValidateIncidentDraftUseCase.MAX_PHOTOS - draft.photos.size).coerceAtLeast(0)
    val canAddPhoto: Boolean get() = remainingPhotoSlots > 0 && !isImportingPhoto && !isSaving
}
