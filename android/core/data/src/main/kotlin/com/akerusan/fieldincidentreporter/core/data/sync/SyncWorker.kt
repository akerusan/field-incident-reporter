package com.akerusan.fieldincidentreporter.core.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

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
        val outcome = try {
            synchronizer.sync(isLastAttempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Anything unexpected (a database error, a bug) still must not fail the work.
            Log.e(TAG, "Sync failed unexpectedly; it will be retried", e)
            IncidentSynchronizer.Outcome.RETRY
        }
        return when (outcome) {
            IncidentSynchronizer.Outcome.SUCCESS -> Result.success()
            IncidentSynchronizer.Outcome.RETRY -> Result.retry()
        }
    }

    companion object {
        /** Runs before rows that keep getting server errors (5xx) are marked FAILED. */
        const val MAX_SYNC_ATTEMPTS = 5
        private const val TAG = "SyncWorker"
    }
}
