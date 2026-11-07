package com.akerusan.fieldincidentreporter.core.testing

import com.akerusan.fieldincidentreporter.core.domain.Clock
import com.akerusan.fieldincidentreporter.core.domain.ConnectivityObserver
import com.akerusan.fieldincidentreporter.core.domain.IdGenerator
import com.akerusan.fieldincidentreporter.core.domain.SyncMonitor
import com.akerusan.fieldincidentreporter.core.domain.SyncScheduler
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow

/** A clock that only moves when the test says so. */
class FakeClock(var now: Instant = Instant.parse("2026-11-07T09:00:00Z")) : Clock {
    override fun now(): Instant = now
}

/** Ids `id-1`, `id-2`, ... in order, so tests can predict them. */
class FixedIdGenerator(private val prefix: String = "id") : IdGenerator {
    private var next = 1

    override fun newId(): String = "$prefix-${next++}"
}

class FakeSyncScheduler : SyncScheduler {
    var requestCount = 0
        private set

    override fun requestSync() {
        requestCount++
    }
}

class FakeSyncMonitor(initial: Boolean = false) : SyncMonitor {
    override val isSyncing = MutableStateFlow(initial)
}

class FakeConnectivityObserver(initial: Boolean = true) : ConnectivityObserver {
    override val isOnline = MutableStateFlow(initial)
}
