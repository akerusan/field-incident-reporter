package com.akerusan.fieldincidentreporter.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs one [IncidentSynchronizer] pass. Returns only success or retry, never failure: a failed
 * request would cancel every sync appended after it, and per-row failures are already recorded on
 * the rows themselves.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val synchronizer: IncidentSynchronizer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isLastAttempt = runAttemptCount >= MAX_SYNC_ATTEMPTS - 1
        return when (synchronizer.sync(isLastAttempt)) {
            IncidentSynchronizer.Outcome.SUCCESS -> Result.success()
            IncidentSynchronizer.Outcome.RETRY -> Result.retry()
        }
    }

    companion object {
        /** Runs before rows that keep getting server errors (5xx) are marked FAILED. */
        const val MAX_SYNC_ATTEMPTS = 5
    }
}
