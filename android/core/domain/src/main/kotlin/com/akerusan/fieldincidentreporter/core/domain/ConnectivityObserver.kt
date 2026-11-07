package com.akerusan.fieldincidentreporter.core.domain

import kotlinx.coroutines.flow.Flow

/** Whether the device currently has a validated internet connection. */
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}
