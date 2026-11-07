package com.akerusan.fieldincidentreporter.core.data.sync

import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncState
import java.time.Instant

/*
 * The sync state machine: how each user action changes a row's sync state. These are pure
 * functions, applied by the repository inside a transaction, and unit-tested row by row.
 *
 * Every user write increments `localVersion` (it never goes back). The sync confirms a write only
 * if `localVersion` still matches what it sent; see IncidentDao.markSynced.
 *
 * | Current (state / op)        | Action    | Result                                              |
 * |-----------------------------|-----------|-----------------------------------------------------|
 * | SYNCED                      | edit      | PENDING / UPDATE                                    |
 * | PENDING / CREATE or UPDATE  | edit      | unchanged operation                                 |
 * | FAILED / CREATE or UPDATE   | edit      | PENDING / CREATE if never synced, else UPDATE       |
 * | operation DELETE            | edit      | rejected                                            |
 * | operation not DELETE        | delete    | PENDING / DELETE, undoable until `undoUntil`        |
 * | PENDING / DELETE            | undo      | PENDING / CREATE if never synced, else UPDATE       |
 * | PENDING / DELETE            | commit    | undo window closed (`deleteAfter = null`)           |
 * | FAILED / any                | retry     | PENDING / same operation                            |
 * | FAILED / any                | discard   | SYNCED with the server's copy; version still grows  |
 * | SYNCED                      | pull      | the server's content; versions unchanged            |
 */

/** The operation that recreates this row's content on the server. */
private val IncidentEntity.createOrUpdate: PendingOperation
    get() = if (syncedVersion == 0L) PendingOperation.CREATE else PendingOperation.UPDATE

/** Applies the user's edit: new content, then the pending operation it requires. */
internal fun IncidentEntity.edited(draft: IncidentDraft, updatedAt: Instant): IncidentEntity {
    check(pendingOperation != PendingOperation.DELETE) { "Incident $id is being deleted and can't be edited" }
    val operation = when (syncState) {
        SyncState.SYNCED -> PendingOperation.UPDATE
        SyncState.PENDING -> checkNotNull(pendingOperation)
        SyncState.FAILED -> createOrUpdate
    }
    return copy(
        title = draft.title,
        description = draft.description,
        category = checkNotNull(draft.category),
        severity = checkNotNull(draft.severity),
        status = draft.status,
        locationNote = draft.locationNote,
        updatedAt = updatedAt,
        syncState = SyncState.PENDING,
        pendingOperation = operation,
        localVersion = localVersion + 1,
        syncErrorReason = null,
        syncErrorDetail = null,
    )
}

/** Marks the row deleted; it stays hidden but undoable until [undoUntil] or until committed. */
internal fun IncidentEntity.deleted(undoUntil: Instant): IncidentEntity {
    check(pendingOperation != PendingOperation.DELETE) { "Incident $id is already being deleted" }
    return copy(
        syncState = SyncState.PENDING,
        pendingOperation = PendingOperation.DELETE,
        deleteAfter = undoUntil,
        localVersion = localVersion + 1,
        syncErrorReason = null,
        syncErrorDetail = null,
    )
}

/**
 * Undoes a pending delete. If the server already saw the row, it is re-sent as an UPDATE: harmless
 * if the delete never went out (PUT is idempotent), and it recreates the incident if it did.
 */
internal fun IncidentEntity.restored(): IncidentEntity {
    check(isPendingDelete) { "Incident $id has no pending delete to undo" }
    return copy(
        pendingOperation = createOrUpdate,
        deleteAfter = null,
        localVersion = localVersion + 1,
    )
}

/** Closes the undo window so the next sync sends the delete. Not a content change: no version bump. */
internal fun IncidentEntity.deletionCommitted(): IncidentEntity {
    check(isPendingDelete) { "Incident $id has no pending delete to commit" }
    return copy(deleteAfter = null)
}

/** Queues a failed change again. */
internal fun IncidentEntity.retried(): IncidentEntity {
    check(syncState == SyncState.FAILED) { "Incident $id has no failed change to retry" }
    return copy(
        syncState = SyncState.PENDING,
        localVersion = localVersion + 1,
        syncErrorReason = null,
        syncErrorDetail = null,
    )
}

internal val IncidentEntity.isPendingDelete: Boolean
    get() = syncState == SyncState.PENDING && pendingOperation == PendingOperation.DELETE

internal val IncidentEntity.isUndoableDelete: Boolean
    get() = isPendingDelete && deleteAfter != null

/**
 * Discards a failed local change: the row becomes the server's copy [server], confirmed. The
 * version keeps growing rather than going back, so a confirmation still in flight for an older
 * version can never match it.
 */
internal fun IncidentEntity.replacedByServerCopy(server: IncidentEntity): IncidentEntity {
    check(syncState == SyncState.FAILED) { "Incident $id has no failed change to discard" }
    val newVersion = localVersion + 1
    return server.copy(localVersion = newVersion, syncedVersion = newVersion)
}

/** A synced row takes the server's latest content. Nothing local is pending, so versions stay. */
internal fun IncidentEntity.updatedFromServer(server: IncidentEntity): IncidentEntity {
    check(syncState == SyncState.SYNCED) { "Incident $id has local changes the pull must not overwrite" }
    return server.copy(localVersion = localVersion, syncedVersion = syncedVersion)
}
