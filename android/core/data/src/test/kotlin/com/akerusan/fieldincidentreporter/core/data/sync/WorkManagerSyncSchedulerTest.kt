package com.akerusan.fieldincidentreporter.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `sync requests are chained under one unique work name and wait for a network`() {
        val scheduler = WorkManagerSyncScheduler(context)

        scheduler.requestSync()
        scheduler.requestSync()

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.SYNC_WORK_NAME)
            .get()
        assertEquals(2, infos.size)
        assertTrue(infos.all { it.constraints.requiredNetworkType == NetworkType.CONNECTED })
        // The second request waits for the first: syncs never run concurrently.
        assertTrue(infos.any { it.state == WorkInfo.State.BLOCKED })
    }
}
