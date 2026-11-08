package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.model.Incident
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Deleted incidents that can still be restored. The list screen shows its undo snackbar while one
 * exists, so the snackbar survives navigation and process death.
 */
class ObservePendingDeletesUseCase @Inject constructor(private val repository: IncidentRepository) {
    operator fun invoke(): Flow<List<Incident>> = repository.observeUndoableDeletes()
}
