package com.akerusan.fieldincidentreporter.core.data.sync

import com.akerusan.fieldincidentreporter.core.data.testing.entity
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.testing.testDraft
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** One test per row of the table in SyncTransitions.kt. */
class SyncTransitionsTest {
    private val now = Instant.parse("2026-11-07T09:00:00Z")
    private val undoUntil = now.plusSeconds(30)

    @Test
    fun `edit of a synced row becomes a pending update`() {
        val result = entity(
            SyncState.SYNCED,
            null,
            localVersion = 3,
            syncedVersion = 3,
        ).edited(testDraft(title = "New"), now)

        assertEquals(SyncState.PENDING, result.syncState)
        assertEquals(PendingOperation.UPDATE, result.pendingOperation)
        assertEquals(4, result.localVersion)
        assertEquals("New", result.title)
        assertEquals(now, result.updatedAt)
    }

    @Test
    fun `edit of a pending create stays a create`() {
        val result = entity(SyncState.PENDING, PendingOperation.CREATE, localVersion = 1, syncedVersion = 0)
            .edited(testDraft(), now)

        assertEquals(PendingOperation.CREATE, result.pendingOperation)
        assertEquals(2, result.localVersion)
    }

    @Test
    fun `edit of a pending update stays an update`() {
        val result = entity(SyncState.PENDING, PendingOperation.UPDATE, localVersion = 4, syncedVersion = 3)
            .edited(testDraft(), now)

        assertEquals(PendingOperation.UPDATE, result.pendingOperation)
    }

    @Test
    fun `edit of a failed create that never reached the server is a create again`() {
        val failed =
            entity(SyncState.FAILED, PendingOperation.CREATE, localVersion = 1, syncedVersion = 0, error = true)

        val result = failed.edited(testDraft(), now)

        assertEquals(SyncState.PENDING, result.syncState)
        assertEquals(PendingOperation.CREATE, result.pendingOperation)
        assertNull(result.syncErrorReason)
    }

    @Test
    fun `edit of a failed update is an update again`() {
        val failed =
            entity(SyncState.FAILED, PendingOperation.UPDATE, localVersion = 5, syncedVersion = 3, error = true)

        assertEquals(PendingOperation.UPDATE, failed.edited(testDraft(), now).pendingOperation)
    }

    @Test(expected = IllegalStateException::class)
    fun `edit of a row being deleted is rejected`() {
        entity(SyncState.PENDING, PendingOperation.DELETE).edited(testDraft(), now)
    }

    @Test(expected = IllegalStateException::class)
    fun `edit of a failed delete is rejected`() {
        entity(SyncState.FAILED, PendingOperation.DELETE, error = true).edited(testDraft(), now)
    }

    @Test
    fun `delete makes the row a pending delete that can be undone`() {
        val result = entity(SyncState.SYNCED, null, localVersion = 3, syncedVersion = 3).deleted(undoUntil)

        assertEquals(SyncState.PENDING, result.syncState)
        assertEquals(PendingOperation.DELETE, result.pendingOperation)
        assertEquals(undoUntil, result.deleteAfter)
        assertEquals(4, result.localVersion)
    }

    @Test
    fun `delete of a failed update clears the error`() {
        val result = entity(SyncState.FAILED, PendingOperation.UPDATE, error = true).deleted(undoUntil)

        assertEquals(PendingOperation.DELETE, result.pendingOperation)
        assertNull(result.syncErrorReason)
    }

    @Test
    fun `undo of a delete the server never saw is a create`() {
        val pendingDelete = entity(SyncState.PENDING, PendingOperation.DELETE, localVersion = 2, syncedVersion = 0)
            .copy(deleteAfter = undoUntil)

        val result = pendingDelete.restored()

        assertEquals(PendingOperation.CREATE, result.pendingOperation)
        assertNull(result.deleteAfter)
        assertEquals(3, result.localVersion)
    }

    @Test
    fun `undo of a delete of a synced row is an update`() {
        val pendingDelete = entity(SyncState.PENDING, PendingOperation.DELETE, localVersion = 4, syncedVersion = 3)
            .copy(deleteAfter = undoUntil)

        assertEquals(PendingOperation.UPDATE, pendingDelete.restored().pendingOperation)
    }

    @Test
    fun `commit closes the undo window without a version bump`() {
        val pendingDelete = entity(
            SyncState.PENDING,
            PendingOperation.DELETE,
            localVersion = 4,
        ).copy(deleteAfter = undoUntil)

        val result = pendingDelete.deletionCommitted()

        assertNull(result.deleteAfter)
        assertEquals(4, result.localVersion)
        assertEquals(PendingOperation.DELETE, result.pendingOperation)
    }

    @Test
    fun `retry queues the failed operation again`() {
        val failed = entity(SyncState.FAILED, PendingOperation.DELETE, localVersion = 4, error = true)

        val result = failed.retried()

        assertEquals(SyncState.PENDING, result.syncState)
        assertEquals(PendingOperation.DELETE, result.pendingOperation)
        assertEquals(5, result.localVersion)
        assertNull(result.syncErrorReason)
    }

    @Test
    fun `discard replaces a failed row with the server's copy, confirmed, and the version still grows`() {
        val failed =
            entity(SyncState.FAILED, PendingOperation.UPDATE, localVersion = 5, syncedVersion = 3, error = true)
        val server = entity(SyncState.SYNCED, null, localVersion = 1, syncedVersion = 1).copy(title = "Server")

        val result = failed.replacedByServerCopy(server)

        assertEquals("Server", result.title)
        assertEquals(SyncState.SYNCED, result.syncState)
        assertNull(result.pendingOperation)
        assertEquals(6, result.localVersion)
        assertEquals(6, result.syncedVersion)
    }

    @Test
    fun `a pull update takes the server's content and keeps the versions`() {
        val synced = entity(SyncState.SYNCED, null, localVersion = 3, syncedVersion = 3)
        val server = entity(SyncState.SYNCED, null, localVersion = 1, syncedVersion = 1).copy(title = "Server")

        val result = synced.updatedFromServer(server)

        assertEquals("Server", result.title)
        assertEquals(3, result.localVersion)
        assertEquals(3, result.syncedVersion)
    }

    @Test(expected = IllegalStateException::class)
    fun `a pull never overwrites a row with local changes`() {
        entity(SyncState.PENDING, PendingOperation.UPDATE).updatedFromServer(entity(SyncState.SYNCED, null))
    }

    @Test(expected = IllegalStateException::class)
    fun `retry of a row that didn't fail is rejected`() {
        entity(SyncState.PENDING, PendingOperation.UPDATE).retried()
    }

    private fun entity(
        syncState: SyncState,
        pendingOperation: PendingOperation?,
        localVersion: Long = 1,
        syncedVersion: Long = 1,
        error: Boolean = false,
    ) = entity(
        syncState = syncState,
        pendingOperation = pendingOperation,
        localVersion = localVersion,
        syncedVersion = syncedVersion,
        syncErrorReason = if (error) SyncError.Reason.REJECTED_BY_SERVER else null,
    )
}
