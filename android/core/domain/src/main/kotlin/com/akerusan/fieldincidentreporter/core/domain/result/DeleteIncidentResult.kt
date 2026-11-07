package com.akerusan.fieldincidentreporter.core.domain.result

/** The outcome of deleting an incident. */
sealed interface DeleteIncidentResult {
    data object Deleted : DeleteIncidentResult

    /** The incident was resolved by an operator and can't be deleted. */
    data object ReadOnly : DeleteIncidentResult

    data object NotFound : DeleteIncidentResult
}
