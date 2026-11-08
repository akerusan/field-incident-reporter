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
    fun `on the last attempt the change is marked failed and the work succeeds`() = runTest {
        env.dao.insert(
            entity(syncState = SyncState.PENDING, pendingOperation = PendingOperation.UPDATE, localVersion = 2),
        )
        env.respond("PUT", "/incidents/incident-1", 503)
        env.respond("GET", "/incidents", 200, "[]")

        val result = worker(runAttemptCount = SyncWorker.MAX_SYNC_ATTEMPTS - 1).doWork()

        assertEquals(ListenableWorker.Result.success(), result) // never failure(): it would cancel queued syncs
        assertEquals(SyncState.FAILED, checkNotNull(env.dao.getById("incident-1")).incident.syncState)
    }

    private fun worker(runAttemptCount: Int): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(ApplicationProvider.getApplicationContext())
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        context: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = SyncWorker(context, workerParameters, env.synchronizer)
                },
            )
            .build()
}
