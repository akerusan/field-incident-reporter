package com.akerusan.fieldincidentreporter.feature.incidentdetail

import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.PendingOperation

sealed interface IncidentDetailUiState {
    data object Loading : IncidentDetailUiState

    data class Content(val incident: Incident, val isDiscarding: Boolean = false) : IncidentDetailUiState {
        private val isBeingDeleted: Boolean get() = incident.pendingOperation == PendingOperation.DELETE

        val canEdit: Boolean get() = !incident.isReadOnly && !isBeingDeleted
        val canDelete: Boolean get() = !incident.isReadOnly && !isBeingDeleted
    }

    /**
     * The incident no longer exists here: deleted (by this user, or on the server) or never found.
     * The screen navigates back; this is expected, not an error.
     */
    data object Gone : IncidentDetailUiState
}
