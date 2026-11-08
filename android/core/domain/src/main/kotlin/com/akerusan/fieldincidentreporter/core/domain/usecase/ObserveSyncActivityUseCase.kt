package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.SyncMonitor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Whether a background sync is running, for the pull-to-refresh indicator. */
class ObserveSyncActivityUseCase @Inject constructor(private val syncMonitor: SyncMonitor) {
    operator fun invoke(): Flow<Boolean> = syncMonitor.isSyncing
}
