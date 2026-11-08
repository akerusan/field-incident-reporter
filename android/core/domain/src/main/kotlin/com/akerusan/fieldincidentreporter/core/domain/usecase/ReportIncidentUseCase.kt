package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.Clock
import com.akerusan.fieldincidentreporter.core.domain.IdGenerator
import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.result.SaveIncidentResult
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncState
import javax.inject.Inject

/**
 * Reports a new incident. The id is generated on the device so the report can be created offline;
 * it is saved locally and uploaded in the background.
 */
class ReportIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository,
    private val validateDraft: ValidateIncidentDraftUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: IncidentDraft): SaveIncidentResult {
        val errors = validateDraft(draft)
        if (errors.isNotEmpty()) return SaveIncidentResult.Invalid(errors)

        val now = clock.now()
        val incident = Incident(
            id = idGenerator.newId(),
            title = draft.title.trim(),
            description = draft.description.trim(),
            category = checkNotNull(draft.category),
            severity = checkNotNull(draft.severity),
            status = draft.status,
            locationNote = draft.locationNote.trim(),
            photos = draft.photos,
            reportedAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
            pendingOperation = PendingOperation.CREATE,
            syncError = null,
        )
        repository.createIncident(incident)
        return SaveIncidentResult.Saved(incident.id)
    }
}
