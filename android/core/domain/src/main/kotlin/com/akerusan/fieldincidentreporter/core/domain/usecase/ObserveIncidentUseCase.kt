package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.model.Incident
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** One incident, or null once it is gone (deleted here, or removed on the server). */
class ObserveIncidentUseCase @Inject constructor(private val repository: IncidentRepository) {
    operator fun invoke(id: String): Flow<Incident?> = repository.observeIncident(id)
}
