package com.akerusan.fieldincidentreporter.core.model

import kotlinx.serialization.Serializable

/**
 * The editable content of an incident, as the editor holds it. Category and severity start empty so
 * the technician makes a deliberate choice. Serializable so the editor can keep it in
 * `SavedStateHandle` across process death.
 */
@Serializable
data class IncidentDraft(
    val title: String = "",
    val description: String = "",
    val category: IncidentCategory? = null,
    val severity: IncidentSeverity? = null,
    val status: IncidentStatus = IncidentStatus.OPEN,
    val locationNote: String = "",
    val photos: List<IncidentPhoto> = emptyList(),
) {
    companion object {
        fun from(incident: Incident): IncidentDraft = IncidentDraft(
            title = incident.title,
            description = incident.description,
            category = incident.category,
            severity = incident.severity,
            status = incident.status,
            locationNote = incident.locationNote,
            photos = incident.photos,
        )
    }
}
