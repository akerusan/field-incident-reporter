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
 * Every user write increments `localVersion` (it never goes back) and clears the error state,
 * including the server-error count. The sync confirms a write only if `localVersion` still matches
 * what it sent; see IncidentDao.markSynced.
 *
 * | Current (state / op)        | Action          | Result                                        |
 * |-----------------------------|-----------------|-----------------------------------------------|
 * | SYNCED                      | edit            | PENDING / UPDATE                              |
 * | PENDING / CREATE or UPDATE  | edit            | unchanged operation                           |
 * | FAILED / CREATE or UPDATE   | edit            | PENDING / CREATE if never synced, else UPDATE |
 * | operation DELETE            | edit            | not allowed (the repository refuses it)       |
 * | operation not DELETE        | delete          | PENDING / DELETE, undoable until `undoUntil`  |
 * | PENDING / DELETE            | undo            | PENDING / CREATE if never synced, else UPDATE |
 * | PENDING / DELETE            | commit          | undo window closed (`deleteAfter = null`)     |
 * | FAILED / any                | retry           | PENDING / same operation                      |
 * | FAILED / any                | discard (200)   | SYNCED with the server's copy                 |
 * | FAILED / any                | discard (404)   | removed locally (the server doesn't have it)  |
 * | SYNCED                      | pull            | the server's content; versions unchanged      |
 */

/** The operation that recreates this row's content on the server. Both are the same idempotent PUT. */
private val IncidentEntity.createOrUpdate: PendingOperation
    get() = if (syncedVersion == 0L) PendingOperation.CREATE else PendingOperation.UPDATE

/** Applies the user's edit: new content, then the pending operation it requires. */
internal fun IncidentEntity.edited(draft: IncidentDraft, updatedAt: Instant): IncidentEntity {
    check(!isBeingDeleted) { "Incident $id is being deleted and can't be edited" }
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
    ).userWrite()
}

/** Marks the row deleted; it stays hidden but undoable until [undoUntil] or until committed. */
internal fun IncidentEntity.deleted(undoUntil: Instant): IncidentEntity {
    check(!isBeingDeleted) { "Incident $id is already being deleted" }
    return copy(
        syncState = SyncState.PENDING,
        pendingOperation = PendingOperation.DELETE,
        deleteAfter = undoUntil,
    ).userWrite()
}

/**
 * Undoes a pending delete by sending the incident again (a PUT): harmless if the delete never went
 * out, and it recreates the incident if it did.
 */
internal fun IncidentEntity.restored(): IncidentEntity {
    check(isPendingDelete) { "Incident $id has no pending delete to undo" }
    return copy(pendingOperation = createOrUpdate, deleteAfter = null).userWrite()
}

/** Closes the undo window so the next sync sends the delete. Not a content change: no version bump. */
internal fun IncidentEntity.deletionCommitted(): IncidentEntity {
    check(isPendingDelete) { "Incident $id has no pending delete to commit" }
    return copy(deleteAfter = null)
}

/** Queues a failed change again. */
internal fun IncidentEntity.retried(): IncidentEntity {
    check(syncState == SyncState.FAILED) { "Incident $id has no failed change to retry" }
    return copy(syncState = SyncState.PENDING).userWrite()
}

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

/** A user write: a new version, and no leftover error from the previous attempt. */
private fun IncidentEntity.userWrite(): IncidentEntity = copy(
    localVersion = localVersion + 1,
    syncErrorReason = null,
    syncErrorDetail = null,
    serverErrorCount = 0,
)

/** Deleted by the user, whether the delete is still waiting or was refused (as `Incident.isBeingDeleted`). */
internal val IncidentEntity.isBeingDeleted: Boolean
    get() = pendingOperation == PendingOperation.DELETE

/** A delete waiting to be sent (and, while `deleteAfter` is set, still undoable). */
internal val IncidentEntity.isPendingDelete: Boolean
    get() = syncState == SyncState.PENDING && isBeingDeleted
