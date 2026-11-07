package com.akerusan.fieldincidentreporter.core.model

/** What the next sync will do with a [SyncState.PENDING] or [SyncState.FAILED] incident. */
enum class PendingOperation {
    CREATE,
    UPDATE,
    DELETE,
}
