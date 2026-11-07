package com.akerusan.fieldincidentreporter.core.data.repository

import app.cash.turbine.test
import com.akerusan.fieldincidentreporter.core.data.testing.DataTestEnvironment
import com.akerusan.fieldincidentreporter.core.data.testing.entity
import com.akerusan.fieldincidentreporter.core.data.testing.incidentJson
import com.akerusan.fieldincidentreporter.core.data.testing.photoEntity
import com.akerusan.fieldincidentreporter.core.domain.result.DiscardChangesResult
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import com.akerusan.fieldincidentreporter.core.model.IncidentFilter
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.testing.testDraft
import com.akerusan.fieldincidentreporter.core.testing.testIncident
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstIncidentRepositoryTest {
    private val env = DataTestEnvironment()
    private val dao = env.dao
    private val repository = env.repository

    @After
    fun tearDown() = env.close()

    @Test
    fun `creating an incident stores it as a pending create and schedules a sync`() = runTest {
        val photo = IncidentPhoto(id = "p1", localPath = "/photos/p1.jpg")
        repository.createIncident(
            testIncident(
                syncState = SyncState.PENDING,
                pendingOperation = PendingOperation.CREATE,
                photos = listOf(photo),
            ),
        )

        val row = checkNotNull(dao.getById("incident-1"))
        assertEquals(PendingOperation.CREATE, row.incident.pendingOperation)
        assertEquals(1, row.incident.localVersion)
        assertEquals(0, row.incident.syncedVersion)
        assertEquals("/photos/p1.jpg", row.photos.single().localPath)
        assertEquals(1, env.syncScheduler.requestCount)
    }

    @Test
    fun `editing keeps the server path of photos that stay and schedules a sync`() = runTest {
        dao.insert(entity(localVersion = 3, syncedVersion = 3))
        dao.insertPhotos(listOf(photoEntity("kept", remotePath = "/uploads/kept.jpg")))
        val kept = checkNotNull(repository.getIncident("incident-1")).photos.single()
        val added = IncidentPhoto(id = "added", localPath = "/photos/added.jpg")

        repository.updateIncident("incident-1", testDraft(photos = listOf(added, kept)), env.clock.now())

        val row = checkNotNull(dao.getById("incident-1"))
        assertEquals(4, row.incident.localVersion)
        assertEquals(PendingOperation.UPDATE, row.incident.pendingOperation)
        assertEquals(listOf("added", "kept"), row.orderedPhotos.map { it.id })
        assertEquals(listOf(null, "/uploads/kept.jpg"), row.orderedPhotos.map { it.remotePath })
        assertEquals(1, env.syncScheduler.requestCount)
    }

    @Test
    fun `photos are exposed with absolute URLs`() = runTest {
        dao.insert(entity())
        dao.insertPhotos(listOf(photoEntity("p1", remotePath = "/uploads/p1.jpg")))

        val url = checkNotNull(repository.getIncident("incident-1")).photos.single().remoteUrl

        assertEquals(env.server.url("/uploads/p1.jpg").toString(), url)
    }

    @Test
    fun `a deleted incident is hidden, undoable, and nothing is sent yet`() = runTest {
        dao.insert(entity())

        repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30))

        assertTrue(repository.observeIncidents(IncidentFilter()).first().isEmpty())
        assertNull(repository.observeIncident("incident-1").first())
        assertEquals(listOf("incident-1"), repository.observeUndoableDeletes().first().map { it.id })
        assertEquals(0, env.syncScheduler.requestCount)
    }

    @Test
    fun `undo brings the incident back and schedules a sync`() = runTest {
        dao.insert(entity())
        repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30))

        repository.observeIncidents(IncidentFilter()).test {
            assertTrue(awaitItem().isEmpty())
            assertEquals(RestoreResult.RESTORED, repository.restoreDeleted("incident-1"))
            assertEquals(listOf("incident-1"), awaitItem().map { it.id })
        }
        assertEquals(1, env.syncScheduler.requestCount)
    }

    @Test
    fun `undo after the delete reached the server is too late`() = runTest {
        assertEquals(RestoreResult.TOO_LATE, repository.restoreDeleted("incident-1"))
    }

    @Test
    fun `committing a delete closes the undo window and schedules a sync`() = runTest {
        dao.insert(entity())
        repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30))

        repository.commitDeletion("incident-1")

        assertTrue(repository.observeUndoableDeletes().first().isEmpty())
        assertNull(checkNotNull(dao.getById("incident-1")).incident.deleteAfter)
        assertEquals(1, env.syncScheduler.requestCount)
    }

    @Test
    fun `the status filter applies`() = runTest {
        dao.insert(entity(id = "open"))
        dao.insert(entity(id = "resolved", status = IncidentStatus.RESOLVED))

        val resolved = repository.observeIncidents(IncidentFilter(status = IncidentStatus.RESOLVED)).first()

        assertEquals(listOf("resolved"), resolved.map { it.id })
    }

    @Test
    fun `retry queues a failed change again`() = runTest {
        dao.insert(
            entity(
                syncState = SyncState.FAILED,
                pendingOperation = PendingOperation.UPDATE,
                syncErrorReason = SyncError.Reason.REJECTED_BY_SERVER,
            ),
        )

        repository.retrySync("incident-1")

        assertEquals(SyncState.PENDING, checkNotNull(dao.getById("incident-1")).incident.syncState)
        assertEquals(1, env.syncScheduler.requestCount)
    }

    // --- Discard local changes -------------------------------------------------------------------

    @Test
    fun `discarding a failed create asks the server, and removes it if the server doesn't have it`() = runTest {
        dao.insert(failed(syncedVersion = 0, operation = PendingOperation.CREATE))
        env.respond("GET", "/incidents/incident-1", 404)

        assertEquals(DiscardChangesResult.DISCARDED, repository.discardLocalChanges("incident-1"))

        assertNull(dao.getById("incident-1"))
        assertEquals(listOf("GET /incidents/incident-1"), env.requests)
    }

    @Test
    fun `discarding a failed create the server did receive keeps the server's copy`() = runTest {
        dao.insert(failed(syncedVersion = 0, operation = PendingOperation.CREATE))
        env.respond("GET", "/incidents/incident-1", 200, incidentJson())

        assertEquals(DiscardChangesResult.DISCARDED, repository.discardLocalChanges("incident-1"))

        assertEquals(SyncState.SYNCED, checkNotNull(dao.getById("incident-1")).incident.syncState)
    }

    @Test
    fun `deleting twice, or editing an incident being deleted, does not crash`() = runTest {
        dao.insert(entity())
        repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30))

        assertTrue(repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30)))
        assertEquals(false, repository.updateIncident("incident-1", testDraft(), env.clock.now()))
        assertEquals(PendingOperation.DELETE, checkNotNull(dao.getById("incident-1")).incident.pendingOperation)
    }

    @Test
    fun `discarding restores the server's copy and the version keeps growing`() = runTest {
        dao.insert(failed(title = "My edit", localVersion = 5, syncedVersion = 3))
        dao.insertPhotos(listOf(photoEntity("mine", localPath = "/photos/mine.jpg")))
        env.respond("GET", "/incidents/incident-1", 200, incidentJson(title = "Server title", status = "RESOLVED"))

        assertEquals(DiscardChangesResult.DISCARDED, repository.discardLocalChanges("incident-1"))

        val row = checkNotNull(dao.getById("incident-1"))
        assertEquals("Server title", row.incident.title)
        assertEquals(IncidentStatus.RESOLVED, row.incident.status)
        assertEquals(SyncState.SYNCED, row.incident.syncState)
        assertNull(row.incident.syncErrorReason)
        assertEquals(6, row.incident.localVersion)
        assertEquals(6, row.incident.syncedVersion)
        assertTrue(row.photos.isEmpty())
        assertEquals(listOf("mine"), env.photoStore.deletedPhotos.map { it.id })
    }

    @Test
    fun `discarding when the server no longer has the incident removes it`() = runTest {
        dao.insert(failed())
        env.respond("GET", "/incidents/incident-1", 404)

        assertEquals(DiscardChangesResult.DISCARDED, repository.discardLocalChanges("incident-1"))

        assertNull(dao.getById("incident-1"))
    }

    @Test
    fun `discarding without a connection changes nothing`() = runTest {
        dao.insert(failed(title = "My edit"))
        env.goOffline()

        assertEquals(DiscardChangesResult.NEEDS_CONNECTION, repository.discardLocalChanges("incident-1"))

        assertEquals("My edit", checkNotNull(dao.getById("incident-1")).incident.title)
    }

    @Test
    fun `there is nothing to discard on a row that didn't fail`() = runTest {
        dao.insert(entity())

        assertEquals(DiscardChangesResult.NOTHING_TO_DISCARD, repository.discardLocalChanges("incident-1"))
    }

    private fun failed(
        title: String = "Broken pump",
        localVersion: Long = 2,
        syncedVersion: Long = 1,
        operation: PendingOperation = PendingOperation.UPDATE,
    ) = entity(
        title = title,
        syncState = SyncState.FAILED,
        pendingOperation = operation,
        localVersion = localVersion,
        syncedVersion = syncedVersion,
        syncErrorReason = SyncError.Reason.RESOLVED_BY_OPERATOR,
    )
}
