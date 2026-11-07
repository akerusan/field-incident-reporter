package com.akerusan.fieldincidentreporter.core.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.akerusan.fieldincidentreporter.core.domain.SyncMonitor
import com.akerusan.fieldincidentreporter.core.domain.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Schedules [SyncWorker] under one unique work name. Every request is appended to the chain
 * (APPEND_OR_REPLACE), so syncs never run concurrently and a change made during a running sync is
 * picked up by the next one. One name only: two names would let two push-then-pull runs overlap,
 * and one run's pull could delete a row the other had just synced.
 */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    SyncScheduler,
    SyncMonitor {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF)
            .build()
        workManager.enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    override val isSyncing: Flow<Boolean>
        get() = workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }
            .distinctUntilChanged()

    companion object {
        const val SYNC_WORK_NAME = "sync"
        private val INITIAL_BACKOFF: Duration = Duration.ofSeconds(10)
    }
}
