package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import javax.inject.Inject

/** Undoes a delete, unless it already reached the server. */
class RestoreIncidentUseCase @Inject constructor(private val repository: IncidentRepository) {
    suspend operator fun invoke(id: String): RestoreResult = repository.restoreDeleted(id)
}
