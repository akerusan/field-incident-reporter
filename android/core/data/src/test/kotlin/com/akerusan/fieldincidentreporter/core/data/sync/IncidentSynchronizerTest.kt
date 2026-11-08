package com.akerusan.fieldincidentreporter.core.data.sync

import com.akerusan.fieldincidentreporter.core.data.sync.IncidentSynchronizer.Outcome
import com.akerusan.fieldincidentreporter.core.data.testing.DataTestEnvironment
import com.akerusan.fieldincidentreporter.core.data.testing.entity
import com.akerusan.fieldincidentreporter.core.data.testing.incidentJson
import com.akerusan.fieldincidentreporter.core.data.testing.photoEntity
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.testing.testDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncidentSynchronizerTest {
    @get:Rule val tempFolder = TemporaryFolder()

    private val env = DataTestEnvironment()
    private val dao = env.dao

    @After
    fun tearDown() = env.close()

    // --- Push: create and update -----------------------------------------------------------------

    @Test
    fun `a new incident uploads its photos, then is PUT and marked synced`() = runTest {
        val photoFile = tempFolder.newFile("p1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.CREATE, syncedVersion = 0))
        dao.insertPhotos(listOf(photoEntity("p1", localPath = photoFile.absolutePath)))
        env.respond("POST", "/uploads", 201, """{"path":"/uploads/p1.jpg"}""")
        env.respond("PUT", "/incidents/incident-1", 201)
        env.respond(
            "GET",
            "/incidents",
            200,
            "[${incidentJson(photos = """[{"id":"p1","path":"/uploads/p1.jpg"}]""")}]",
        )

        assertEquals(Outcome.SUCCESS, env.synchronizer.sync())

        assertEquals(listOf("POST /uploads", "PUT /incidents/incident-1", "GET /incidents"), env.requests)
        val row = checkNotNull(dao.getById("incident-1"))
        assertEquals(SyncState.SYNCED, row.incident.syncState)
        assertNull(row.incident.pendingOperation)
        assertEquals(1, row.incident.syncedVersion)
        assertEquals("/uploads/p1.jpg", row.photos.single().remotePath)
    }

    @Test
    fun `an edit made while the PUT is in flight stays pending and goes out on the next sync`() = runTest {
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.CREATE, syncedVersion = 0))
        env.respond("GET", "/incidents", 200, "[]")
        env.respond("PUT", "/incidents/incident-1") {
            // The user saves an edit while the server is handling the first PUT.
            runBlocking { env.repository.updateIncident("incident-1", testDraft(title = "Edited"), env.clock.now()) }
            MockResponse.Builder().code(201).build()
        }

        env.synchronizer.sync()

        val afterFirstSync = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncState.PENDING, afterFirstSync.syncState)
        assertEquals(2, afterFirstSync.localVersion)
        assertEquals(0, afterFirstSync.syncedVersion)

        env.respond("PUT", "/incidents/incident-1", 200)
        env.respond("GET", "/incidents", 200, "[${incidentJson(title = "Edited")}]")
        env.synchronizer.sync()

        val afterSecondSync = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncState.SYNCED, afterSecondSync.syncState)
        assertEquals(2, afterSecondSync.syncedVersion)
        assertEquals("Edited", afterSecondSync.title)
    }

    @Test
    fun `a 409 for a resolved incident marks the change failed and keeps the operation`() = runTest {
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2))
        env.respond("PUT", "/incidents/incident-1", 409, """{"code":"INCIDENT_RESOLVED","message":"Resolved"}""")
        env.respond("GET", "/incidents", 200, "[]")

        assertEquals(Outcome.SUCCESS, env.synchronizer.sync())

        val row = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncState.FAILED, row.syncState)
        assertEquals(PendingOperation.UPDATE, row.pendingOperation)
        assertEquals(SyncError.Reason.RESOLVED_BY_OPERATOR, row.syncErrorReason)
    }

    @Test
    fun `any other 4xx marks the change rejected with the server's message`() = runTest {
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2))
        env.respond(
            "PUT",
            "/incidents/incident-1",
            400,
            """{"code":"VALIDATION_ERROR","message":"title is required"}""",
        )
        env.respond("GET", "/incidents", 200, "[]")

        env.synchronizer.sync()

        val row = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncError.Reason.REJECTED_BY_SERVER, row.syncErrorReason)
        assertEquals("title is required", row.syncErrorDetail)
    }

    @Test
    fun `a server error is retried, and the change marked failed after repeated server errors`() = runTest {
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2))
        env.respond("PUT", "/incidents/incident-1", 503)
        env.respond("GET", "/incidents", 200, "[]")

        repeat(IncidentSynchronizer.MAX_SERVER_ERRORS - 1) {
            assertEquals(Outcome.RETRY, env.synchronizer.sync())
            assertEquals(SyncState.PENDING, checkNotNull(dao.getById("incident-1")).incident.syncState)
        }

        assertEquals(Outcome.SUCCESS, env.synchronizer.sync())
        val row = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncState.FAILED, row.syncState)
        assertEquals(SyncError.Reason.SERVER_ERROR, row.syncErrorReason)
    }

    @Test
    fun `a user edit starts the server-error count again`() = runTest {
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2))
        env.respond("PUT", "/incidents/incident-1", 503)
        env.respond("GET", "/incidents", 200, "[]")
        repeat(IncidentSynchronizer.MAX_SERVER_ERRORS - 1) { env.synchronizer.sync() }

        env.repository.updateIncident("incident-1", testDraft(title = "Edited"), env.clock.now())
        env.synchronizer.sync()

        val row = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncState.PENDING, row.syncState)
        assertEquals(1, row.serverErrorCount)
    }

    @Test
    fun `one failing incident doesn't stop the others`() = runTest {
        dao.insert(
            entity(
                id = "a",
                syncState = SyncState.PENDING,
                pendingOperation = PendingOperation.UPDATE,
                localVersion = 2,
            ),
        )
        dao.insert(
            entity(
                id = "b",
                syncState = SyncState.PENDING,
                pendingOperation = PendingOperation.UPDATE,
                localVersion = 2,
            ),
        )
        env.respond("PUT", "/incidents/a", 500)
        env.respond("PUT", "/incidents/b", 200)
        env.respond("GET", "/incidents", 200, "[${incidentJson(id = "a")},${incidentJson(id = "b")}]")

        assertEquals(Outcome.RETRY, env.synchronizer.sync())

        assertEquals(SyncState.PENDING, checkNotNull(dao.getById("a")).incident.syncState)
        assertEquals(SyncState.SYNCED, checkNotNull(dao.getById("b")).incident.syncState)
    }

    @Test
    fun `without a connection the change stays pending and the sync is retried`() = runTest {
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2))
        env.goOffline()

        assertEquals(Outcome.RETRY, env.synchronizer.sync())

        val row = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncState.PENDING, row.syncState) // network errors never mark a change failed
    }

    // --- Push: delete ----------------------------------------------------------------------------

    @Test
    fun `a delete is not sent while it can still be undone`() = runTest {
        dao.insert(entity())
        env.repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30))
        env.respond("GET", "/incidents", 200, "[${incidentJson()}]")

        env.synchronizer.sync()

        assertEquals(listOf("GET /incidents"), env.requests)
        assertEquals(PendingOperation.DELETE, checkNotNull(dao.getById("incident-1")).incident.pendingOperation)
    }

    @Test
    fun `a committed delete is sent, then the row and its photo files are removed`() = runTest {
        dao.insert(entity())
        dao.insertPhotos(listOf(photoEntity("p1", localPath = "/photos/p1.jpg", remotePath = "/uploads/p1.jpg")))
        env.repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30))
        env.repository.commitDeletion("incident-1")
        env.respond("DELETE", "/incidents/incident-1", 200)
        env.respond("GET", "/incidents", 200, "[]")

        env.synchronizer.sync()

        assertTrue("DELETE /incidents/incident-1" in env.requests)
        assertNull(dao.getById("incident-1"))
        assertEquals(listOf("p1"), env.photoStore.deletedPhotos.map { it.id })
    }

    @Test
    fun `a delete is sent once the undo window has passed, even if never committed`() = runTest {
        dao.insert(entity())
        env.repository.markDeleted("incident-1", undoUntil = env.clock.now().plusSeconds(30))
        env.clock.now = env.clock.now.plusSeconds(31)
        env.respond("DELETE", "/incidents/incident-1", 404) // already gone counts as done
        env.respond("GET", "/incidents", 200, "[]")

        env.synchronizer.sync()

        assertNull(dao.getById("incident-1"))
    }

    @Test
    fun `deleting a create the server never confirmed still asks the server, since the PUT may have arrived`() =
        runTest {
            dao.insert(
                entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.CREATE, syncedVersion = 0),
            )
            env.repository.markDeleted("incident-1", undoUntil = env.clock.now())
            env.repository.commitDeletion("incident-1")
            env.respond("DELETE", "/incidents/incident-1", 200) // it did arrive: the response had been lost
            env.respond("GET", "/incidents", 200, "[]")

            env.synchronizer.sync()

            assertEquals(listOf("DELETE /incidents/incident-1", "GET /incidents"), env.requests)
            assertNull(dao.getById("incident-1"))
        }

    @Test
    fun `an unuploaded photo whose file is gone marks the change failed instead of dropping the photo`() = runTest {
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2))
        dao.insertPhotos(listOf(photoEntity("lost", localPath = "/no/such/file.jpg")))
        env.respond("GET", "/incidents", 200, "[]")

        env.synchronizer.sync()

        val row = checkNotNull(dao.getById("incident-1"))
        assertEquals(SyncState.FAILED, row.incident.syncState)
        assertEquals(SyncError.Reason.PHOTO_MISSING, row.incident.syncErrorReason)
        assertEquals(1, row.photos.size)
        assertTrue("PUT /incidents/incident-1" !in env.requests)
    }

    @Test
    fun `a malformed success response is a server error, retried rather than crashing the sync`() = runTest {
        val photoFile = tempFolder.newFile("p1.jpg").apply { writeBytes(byteArrayOf(1)) }
        dao.insert(entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2))
        dao.insertPhotos(listOf(photoEntity("p1", localPath = photoFile.absolutePath)))
        env.respond("POST", "/uploads", 201, "<html>proxy error</html>")
        env.respond("GET", "/incidents", 200, "[]")

        assertEquals(Outcome.RETRY, env.synchronizer.sync())
        assertEquals(SyncState.PENDING, checkNotNull(dao.getById("incident-1")).incident.syncState)
    }

    @Test
    fun `a failing pull is retried`() = runTest {
        env.respond("GET", "/incidents", 503)

        assertEquals(Outcome.RETRY, env.synchronizer.sync())
    }

    @Test
    fun `an undo while the server is deleting keeps the incident and recreates it on the next sync`() = runTest {
        dao.insert(entity())
        env.repository.markDeleted("incident-1", undoUntil = env.clock.now())
        env.repository.commitDeletion("incident-1")
        env.respond("GET", "/incidents", 200, "[]")
        env.respond("DELETE", "/incidents/incident-1") {
            val result = runBlocking { env.repository.restoreDeleted("incident-1") }
            assertEquals(RestoreResult.RESTORED, result)
            MockResponse.Builder().code(200).build()
        }

        env.synchronizer.sync()

        val restored = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(PendingOperation.UPDATE, restored.pendingOperation)

        env.respond("PUT", "/incidents/incident-1", 201)
        env.respond("GET", "/incidents", 200, "[${incidentJson()}]")
        env.synchronizer.sync()

        assertTrue("PUT /incidents/incident-1" in env.requests)
        assertEquals(SyncState.SYNCED, checkNotNull(dao.getById("incident-1")).incident.syncState)
    }

    @Test
    fun `a delete refused because the incident was resolved becomes visible again as failed`() = runTest {
        dao.insert(entity())
        env.repository.markDeleted("incident-1", undoUntil = env.clock.now())
        env.repository.commitDeletion("incident-1")
        env.respond("DELETE", "/incidents/incident-1", 409, """{"code":"INCIDENT_RESOLVED","message":"Resolved"}""")
        env.respond("GET", "/incidents", 200, "[]")

        env.synchronizer.sync()

        val row = checkNotNull(dao.getById("incident-1")).incident
        assertEquals(SyncState.FAILED, row.syncState)
        assertEquals(PendingOperation.DELETE, row.pendingOperation)
        assertEquals(listOf("incident-1"), dao.observeVisible(status = null).first().map { it.incident.id })
    }

    // --- Pull ------------------------------------------------------------------------------------

    @Test
    fun `pull inserts new incidents as synced at version 1, so deleting one reaches the server`() = runTest {
        env.respond("GET", "/incidents", 200, "[${incidentJson(id = "seeded")}]")

        env.synchronizer.sync()

        val pulled = checkNotNull(dao.getById("seeded")).incident
        assertEquals(SyncState.SYNCED, pulled.syncState)
        assertEquals(1, pulled.localVersion)
        assertEquals(1, pulled.syncedVersion)

        env.repository.markDeleted("seeded", undoUntil = env.clock.now())
        env.repository.commitDeletion("seeded")
        env.respond("DELETE", "/incidents/seeded", 200)
        env.respond("GET", "/incidents", 200, "[]")
        env.synchronizer.sync()

        assertTrue("DELETE /incidents/seeded" in env.requests)
    }

    @Test
    fun `pull updates synced rows but never touches rows with local changes`() = runTest {
        dao.insert(entity(id = "synced", title = "Old", localVersion = 3, syncedVersion = 3))
        dao.insert(
            entity(
                id = "pending",
                title = "Mine",
                syncState = SyncState.PENDING,
                pendingOperation = PendingOperation.UPDATE,
            ),
        )
        dao.insert(
            entity(
                id = "failed",
                title = "Mine too",
                syncState = SyncState.FAILED,
                pendingOperation = PendingOperation.UPDATE,
            ),
        )
        env.respond(
            "GET",
            "/incidents",
            200,
            "[${incidentJson(
                "synced",
                "New",
            )},${incidentJson("pending", "Theirs")},${incidentJson("failed", "Theirs")}]",
        )

        env.synchronizer.sync()

        val synced = checkNotNull(dao.getById("synced")).incident
        assertEquals("New", synced.title)
        assertEquals(3, synced.localVersion) // versions are never moved by the pull
        assertEquals("Mine", checkNotNull(dao.getById("pending")).incident.title)
        assertEquals("Mine too", checkNotNull(dao.getById("failed")).incident.title)
    }

    @Test
    fun `pull removes synced rows the server no longer has, with their files, and keeps unsent ones`() = runTest {
        dao.insert(entity(id = "deleted-elsewhere"))
        dao.insertPhotos(listOf(photoEntity("p1", incidentId = "deleted-elsewhere", localPath = "/photos/p1.jpg")))
        dao.insert(
            entity(
                id = "not-sent-yet",
                syncState = SyncState.PENDING,
                pendingOperation = PendingOperation.CREATE,
                syncedVersion = 0,
            ),
        )
        env.respond("PUT", "/incidents/not-sent-yet", 503)
        env.respond("GET", "/incidents", 200, "[]")

        env.synchronizer.sync()

        assertNull(dao.getById("deleted-elsewhere"))
        assertEquals(listOf("p1"), env.photoStore.deletedPhotos.map { it.id })
        assertEquals(SyncState.PENDING, checkNotNull(dao.getById("not-sent-yet")).incident.syncState)
    }

    @Test
    fun `pull skips incidents with values this app doesn't know`() = runTest {
        env.respond(
            "GET",
            "/incidents",
            200,
            "[${incidentJson(id = "future", status = "ARCHIVED")},${incidentJson(id = "ok")}]",
        )

        assertEquals(Outcome.SUCCESS, env.synchronizer.sync())

        assertNull(dao.getById("future"))
        assertEquals(SyncState.SYNCED, checkNotNull(dao.getById("ok")).incident.syncState)
    }
}
