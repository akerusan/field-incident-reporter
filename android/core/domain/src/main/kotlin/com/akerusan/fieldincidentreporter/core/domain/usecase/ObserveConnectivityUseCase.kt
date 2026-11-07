package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.ConnectivityObserver
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Whether the device is online, for the offline banner. */
class ObserveConnectivityUseCase @Inject constructor(private val connectivityObserver: ConnectivityObserver) {
    operator fun invoke(): Flow<Boolean> = connectivityObserver.isOnline
}
