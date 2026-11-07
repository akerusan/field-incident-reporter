package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.Clock
import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.domain.result.SaveIncidentResult
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
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

        val errors = validateDraft(draft)
        if (errors.isNotEmpty()) return SaveIncidentResult.Invalid(errors)

        val normalized = draft.normalized()
        // False also when the incident is being deleted: there is nothing left to edit.
        if (!repository.updateIncident(id, normalized, updatedAt = clock.now())) {
            return SaveIncidentResult.NotFound
        }

        val keptPhotoIds = normalized.photos.map { it.id }.toSet()
        photoStore.deleteLocalFiles(current.photos.filterNot { it.id in keptPhotoIds })
        return SaveIncidentResult.Saved(id)
    }
}
