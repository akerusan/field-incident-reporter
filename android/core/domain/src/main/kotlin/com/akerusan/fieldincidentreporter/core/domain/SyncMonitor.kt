package com.akerusan.fieldincidentreporter.core.domain

import kotlinx.coroutines.flow.Flow

/** Whether a background sync is queued or running. */
interface SyncMonitor {
    val isSyncing: Flow<Boolean>
}
