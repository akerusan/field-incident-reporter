package com.akerusan.fieldincidentreporter.core.model

/** Whether the server has the latest version of an incident. */
enum class SyncState {
    /** The server has this version. */
    SYNCED,

    /** A local change is waiting to be sent; see [PendingOperation]. */
    PENDING,

    /** The server rejected the change; the user can retry or discard it. See [SyncError]. */
    FAILED,
}
