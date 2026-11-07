package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentFilter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Incidents matching a filter, newest first, updated as local data changes. */
class ObserveIncidentsUseCase @Inject constructor(private val repository: IncidentRepository) {
    operator fun invoke(filter: IncidentFilter): Flow<List<Incident>> = repository.observeIncidents(filter)
}
