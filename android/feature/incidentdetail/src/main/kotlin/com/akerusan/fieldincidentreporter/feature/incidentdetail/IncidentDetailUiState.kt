package com.akerusan.fieldincidentreporter.feature.incidentdetail

import com.akerusan.fieldincidentreporter.core.model.Incident

sealed interface IncidentDetailUiState {
    data object Loading : IncidentDetailUiState

    data class Content(val incident: Incident, val isDiscarding: Boolean = false) : IncidentDetailUiState {
        /** Edit and delete: not for resolved incidents, nor for one already being deleted. */
        val canModify: Boolean get() = !incident.isReadOnly && !incident.isBeingDeleted
    }

    /**
     * The incident no longer exists here: deleted (by this user, or on the server) or never found.
     * The screen navigates back; this is expected, not an error.
     */
    data object Gone : IncidentDetailUiState
}
