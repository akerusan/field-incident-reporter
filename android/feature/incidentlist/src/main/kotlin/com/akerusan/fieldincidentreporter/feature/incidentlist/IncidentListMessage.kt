package com.akerusan.fieldincidentreporter.feature.incidentlist

/** One-off messages shown in a snackbar. */
enum class IncidentListMessage {
    /** Undo was pressed after the delete had already reached the server. */
    ALREADY_DELETED,

    /** Pull-to-refresh while offline: the refresh is queued until a connection is back. */
    WILL_SYNC_WHEN_ONLINE,
}
