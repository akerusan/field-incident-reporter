package com.akerusan.fieldincidentreporter.core.data.util

import com.akerusan.fieldincidentreporter.core.domain.Clock
import java.time.Instant
import javax.inject.Inject

class SystemClock @Inject constructor() : Clock {
    override fun now(): Instant = Instant.now()
}
