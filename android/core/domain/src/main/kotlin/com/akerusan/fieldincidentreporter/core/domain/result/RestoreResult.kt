package com.akerusan.fieldincidentreporter.core.domain.result

/** The outcome of undoing a delete. */
enum class RestoreResult {
    RESTORED,

    /** The delete already reached the server; nothing left to restore. */
    TOO_LATE,
}
