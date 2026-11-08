package com.akerusan.fieldincidentreporter.core.domain

/** Schedules a background sync (push local changes, then pull) as soon as a network is available. */
interface SyncScheduler {
    fun requestSync()
}
