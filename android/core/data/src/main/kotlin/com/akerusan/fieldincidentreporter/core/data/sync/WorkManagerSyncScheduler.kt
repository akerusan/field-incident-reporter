package com.akerusan.fieldincidentreporter.core.data.sync

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.akerusan.fieldincidentreporter.core.domain.SyncMonitor
import com.akerusan.fieldincidentreporter.core.domain.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Schedules [SyncWorker] under one unique work name, so syncs never run concurrently (two
 * overlapping push-then-pull runs could let one run's pull delete a row the other just synced).
 *
 * Each request looks at the sync already scheduled:
 *  - nothing running: start a sync now, replacing one that is waiting (for a network, or in backoff
 *    after a failure), so a new change or a refresh never waits behind a long backoff;
 *  - a sync running: queue exactly one follow-up, which picks up every change made meanwhile.
 */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    SyncScheduler,
    SyncMonitor {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    // Decisions read WorkManager's state, so they run off the caller's thread, one at a time.
    private val executor = Executors.newSingleThreadExecutor()

    override fun requestSync() {
        executor.execute(::enqueueSync)
    }

    @VisibleForTesting
    internal fun enqueueSync() {
        val states = workManager.getWorkInfosForUniqueWork(SYNC_WORK_NAME).get().map { it.state }
        val policy = policyFor(states) ?: return
        workManager.enqueueUniqueWork(SYNC_WORK_NAME, policy, newSyncRequest())
    }

    /** True while a sync is queued or running. */
    override val isSyncing: Flow<Boolean>
        get() = workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME)
            .map { infos -> infos.any { !it.state.isFinished } }
            .distinctUntilChanged()

    private fun newSyncRequest(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF)
        .build()

    companion object {
        const val SYNC_WORK_NAME = "sync"
        private val INITIAL_BACKOFF: Duration = Duration.ofSeconds(10)

        /** How to enqueue a new sync given the states of the scheduled ones; null means nothing to do. */
        @VisibleForTesting
        internal fun policyFor(states: List<WorkInfo.State>): ExistingWorkPolicy? {
            val running = WorkInfo.State.RUNNING in states
            val followUpQueued = WorkInfo.State.BLOCKED in states
            return when {
                // The queued follow-up will see this change too.
                running && followUpQueued -> null

                running -> ExistingWorkPolicy.APPEND_OR_REPLACE

                else -> ExistingWorkPolicy.REPLACE
            }
        }
    }
}
