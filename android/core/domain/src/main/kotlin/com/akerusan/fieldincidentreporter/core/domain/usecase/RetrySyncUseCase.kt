package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import javax.inject.Inject

/** Sends a change the server rejected again. */
class RetrySyncUseCase @Inject constructor(private val repository: IncidentRepository) {
    suspend operator fun invoke(id: String) = repository.retrySync(id)
}
