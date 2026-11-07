package com.akerusan.fieldincidentreporter.core.domain.result

/** The outcome of discarding a failed local change. */
enum class DiscardChangesResult {
    /** The incident now matches the server (or was removed, if the server doesn't have it). */
    DISCARDED,

    /** The server couldn't be reached; nothing changed. */
    NEEDS_CONNECTION,

    /** There was no failed change to discard. */
    NOTHING_TO_DISCARD,
}
