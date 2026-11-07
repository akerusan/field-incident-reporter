package com.akerusan.fieldincidentreporter.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.akerusan.fieldincidentreporter.core.data.testing.DataTestEnvironment
import com.akerusan.fieldincidentreporter.core.data.testing.entity
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {
    private val env = DataTestEnvironment()

    @After
    fun tearDown() = env.close()

    @Test
    fun `a transient failure asks WorkManager to retry`() = runTest {
        env.dao.insert(
            entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2),
        )
        env.respond("PUT", "/incidents/incident-1", 503)
        env.respond("GET", "/incidents", 200, "[]")

        assertEquals(ListenableWorker.Result.retry(), worker(runAttemptCount = 0).doWork())
    }

    @Test
    fun `repeated server errors mark the change failed, even when each run is a fresh work request`() = runTest {
        env.dao.insert(
            entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2),
        )
        env.respond("PUT", "/incidents/incident-1", 503)
        env.respond("GET", "/incidents", 200, "[]")

        // runAttemptCount = 0 every time, as after a reschedule: the count on the row still adds up.
        val results = List(IncidentSynchronizer.MAX_SERVER_ERRORS) { worker(runAttemptCount = 0).doWork() }

        assertEquals(ListenableWorker.Result.success(), results.last()) // never failure()
        assertEquals(SyncState.FAILED, checkNotNull(env.dao.getById("incident-1")).incident.syncState)
    }

    private fun worker(runAttemptCount: Int): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(ApplicationProvider.getApplicationContext())
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = SyncWorker(appContext, workerParameters, env.synchronizer)
                },
            )
            .build()
}
