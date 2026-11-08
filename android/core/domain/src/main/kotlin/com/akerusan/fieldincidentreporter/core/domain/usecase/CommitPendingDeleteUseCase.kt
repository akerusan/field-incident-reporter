package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import javax.inject.Inject

/** The undo snackbar was dismissed without undo: send the delete now. */
class CommitPendingDeleteUseCase @Inject constructor(private val repository: IncidentRepository) {
    suspend operator fun invoke(id: String) = repository.commitDeletion(id)
}
