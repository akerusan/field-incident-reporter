package com.akerusan.fieldincidentreporter.core.model

/** Why the last sync of an incident failed. [detail] is the server's message, for diagnostics. */
data class SyncError(val reason: Reason, val detail: String? = null) {
    enum class Reason {
        /** An operator resolved the incident, so it can no longer be changed or deleted. */
        RESOLVED_BY_OPERATOR,

        /** The server rejected the request (for example, a validation error). */
        REJECTED_BY_SERVER,

        /** The server kept failing (5xx) after several attempts. */
        SERVER_ERROR,
    }
}
