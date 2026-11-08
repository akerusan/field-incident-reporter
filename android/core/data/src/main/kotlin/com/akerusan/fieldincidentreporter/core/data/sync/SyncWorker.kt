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
 * Runs one [IncidentSynchronizer] pass. Returns only success or retry, never failure: per-row
 * failures are recorded on the rows themselves, and a failed work request would also cancel the
 * follow-up sync queued behind it.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val synchronizer: IncidentSynchronizer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = try {
            synchronizer.sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Unexpected (a database error, a bug). Retry a few times in case it was transient, then
            // stop. runAttemptCount restarts when the scheduler replaces waiting work (a new write),
            // so this caps one run of retries, not the total; exponential backoff bounds the rest.
            Log.e(TAG, "Sync failed unexpectedly (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount + 1 >= MAX_UNEXPECTED_FAILURES) return Result.success()
            IncidentSynchronizer.Outcome.RETRY
        }
        return when (outcome) {
            IncidentSynchronizer.Outcome.SUCCESS -> Result.success()
            IncidentSynchronizer.Outcome.RETRY -> Result.retry()
        }
    }

    companion object {
        private const val MAX_UNEXPECTED_FAILURES = 3
        private const val TAG = "SyncWorker"
    }
}
