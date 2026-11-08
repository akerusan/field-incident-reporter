package com.akerusan.fieldincidentreporter.feature.incidentlist

import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus

/**
 * Everything the list screen shows.
 *
 * [pendingDelete] drives the undo snackbar: it is state, not an event, so the snackbar comes back
 * after navigation or process death for as long as the delete can still be undone.
 */
data class IncidentListUiState(
    val isLoading: Boolean = true,
    val incidents: List<Incident> = emptyList(),
    val statusFilter: IncidentStatus? = null,
    val isOnline: Boolean = true,
    val isRefreshing: Boolean = false,
    val pendingDelete: Incident? = null,
)
