package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.SyncScheduler
import javax.inject.Inject

/** Sends pending changes and fetches the latest incidents, as soon as a network is available. */
class RefreshIncidentsUseCase @Inject constructor(private val syncScheduler: SyncScheduler) {
    operator fun invoke() = syncScheduler.requestSync()
}
