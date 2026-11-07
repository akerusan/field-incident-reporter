package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.Clock
import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.domain.result.SaveIncidentResult
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import javax.inject.Inject

/**
 * Saves changes to an existing incident. Resolved incidents can't be changed. Local files of photos
 * the user removed are deleted once the change is saved.
 */
class UpdateIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository,
    private val validateDraft: ValidateIncidentDraftUseCase,
    private val photoStore: PhotoStore,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: String, draft: IncidentDraft): SaveIncidentResult {
        val current = repository.getIncident(id) ?: return SaveIncidentResult.NotFound
        if (current.isReadOnly) return SaveIncidentResult.ReadOnly
        if (current.pendingOperation == PendingOperation.DELETE) return SaveIncidentResult.NotFound // being deleted

        val errors = validateDraft(draft)
        if (errors.isNotEmpty()) return SaveIncidentResult.Invalid(errors)

        val trimmed = draft.copy(
            title = draft.title.trim(),
            description = draft.description.trim(),
            locationNote = draft.locationNote.trim(),
        )
        if (!repository.updateIncident(id, trimmed, updatedAt = clock.now())) {
            return SaveIncidentResult.NotFound
        }

        val keptPhotoIds = trimmed.photos.map { it.id }.toSet()
        photoStore.deleteLocalFiles(current.photos.filterNot { it.id in keptPhotoIds })
        return SaveIncidentResult.Saved(id)
    }
}
