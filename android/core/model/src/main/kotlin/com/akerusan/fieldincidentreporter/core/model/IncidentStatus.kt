package com.akerusan.fieldincidentreporter.core.model

/**
 * Where the incident is in its lifecycle. Technicians set [OPEN] or [IN_PROGRESS]; only an operator
 * (from the dashboard) sets [RESOLVED], after which the incident is read-only in the app.
 */
enum class IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    ;

    companion object {
        /** The statuses a technician may choose in the app. */
        val technicianSelectable: List<IncidentStatus> = listOf(OPEN, IN_PROGRESS)
    }
}
