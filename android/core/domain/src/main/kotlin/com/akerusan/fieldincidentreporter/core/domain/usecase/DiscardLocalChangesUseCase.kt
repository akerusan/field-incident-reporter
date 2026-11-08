package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.result.DiscardChangesResult
import javax.inject.Inject

/** Drops a change the server rejected and restores the server's version of the incident. */
class DiscardLocalChangesUseCase @Inject constructor(private val repository: IncidentRepository) {
    suspend operator fun invoke(id: String): DiscardChangesResult = repository.discardLocalChanges(id)
}
