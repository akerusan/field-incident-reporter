package com.akerusan.fieldincidentreporter.core.domain

import java.time.Instant

/** The current time, injectable so time-dependent logic can be tested. */
fun interface Clock {
    fun now(): Instant
}
