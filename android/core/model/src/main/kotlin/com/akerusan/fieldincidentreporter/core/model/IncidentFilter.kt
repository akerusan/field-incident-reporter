package com.akerusan.fieldincidentreporter.core.model

/** Which incidents the list shows. A null [status] means all statuses. */
data class IncidentFilter(val status: IncidentStatus? = null)
