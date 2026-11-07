package com.akerusan.fieldincidentreporter.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkManagerSyncSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    @Test
    fun `a waiting sync is replaced rather than queued behind, and waits for a network`() {
        val scheduler = WorkManagerSyncScheduler(context)

        scheduler.enqueueSync()
        scheduler.enqueueSync()

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.SYNC_WORK_NAME)
            .get()
            .filterNot { it.state.isFinished }
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertEquals(NetworkType.CONNECTED, infos.single().constraints.requiredNetworkType)
    }

    @Test
    fun `nothing running starts a sync now, replacing one in backoff`() {
        assertEquals(ExistingWorkPolicy.REPLACE, WorkManagerSyncScheduler.policyFor(emptyList()))
        assertEquals(ExistingWorkPolicy.REPLACE, WorkManagerSyncScheduler.policyFor(listOf(WorkInfo.State.ENQUEUED)))
        assertEquals(ExistingWorkPolicy.REPLACE, WorkManagerSyncScheduler.policyFor(listOf(WorkInfo.State.SUCCEEDED)))
    }

    @Test
    fun `a running sync gets exactly one follow-up`() {
        assertEquals(
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            WorkManagerSyncScheduler.policyFor(listOf(WorkInfo.State.RUNNING)),
        )
        assertNull(WorkManagerSyncScheduler.policyFor(listOf(WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED)))
    }
}
