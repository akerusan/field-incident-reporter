package com.akerusan.fieldincidentreporter.core.domain.result

/** The outcome of reporting or editing an incident. */
sealed interface SaveIncidentResult {
    data class Saved(val incidentId: String) : SaveIncidentResult

    data class Invalid(val errors: Set<DraftError>) : SaveIncidentResult

    /** The incident was resolved by an operator and can't be changed. */
    data object ReadOnly : SaveIncidentResult

    data object NotFound : SaveIncidentResult
}
