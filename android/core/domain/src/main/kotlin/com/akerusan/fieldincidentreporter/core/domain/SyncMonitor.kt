package com.akerusan.fieldincidentreporter.core.domain

import kotlinx.coroutines.flow.Flow

/** Whether a background sync is running right now. */
interface SyncMonitor {
    val isSyncing: Flow<Boolean>
}
