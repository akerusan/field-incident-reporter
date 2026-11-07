package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.Clock
import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.result.DeleteIncidentResult
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import java.time.Duration
import javax.inject.Inject

/**
 * Deletes an incident, with a window during which the user can undo it. Nothing is sent to the
 * server until the undo snackbar is dismissed ([CommitPendingDeleteUseCase]) or the window ends.
 */
class DeleteIncidentUseCase @Inject constructor(private val repository: IncidentRepository, private val clock: Clock) {
    suspend operator fun invoke(id: String): DeleteIncidentResult {
        val incident = repository.getIncident(id) ?: return DeleteIncidentResult.NotFound
        if (incident.isReadOnly) return DeleteIncidentResult.ReadOnly
        if (incident.pendingOperation == PendingOperation.DELETE) return DeleteIncidentResult.Deleted // already
        val marked = repository.markDeleted(id, undoUntil = clock.now().plus(UNDO_WINDOW))
        return if (marked) DeleteIncidentResult.Deleted else DeleteIncidentResult.NotFound
    }

    companion object {
        /**
         * Upper bound of the undo window. The snackbar normally commits the delete earlier, when
         * it is dismissed; this bound only matters if it stays up (for example with TalkBack).
         */
        val UNDO_WINDOW: Duration = Duration.ofSeconds(30)
    }
}
