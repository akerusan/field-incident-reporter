package com.akerusan.fieldincidentreporter.core.model

import java.time.Instant

/** An incident as the app shows it: the reported content plus its sync status. */
data class Incident(
    val id: String,
    val title: String,
    val description: String,
    val category: IncidentCategory,
    val severity: IncidentSeverity,
    val status: IncidentStatus,
    val locationNote: String,
    val photos: List<IncidentPhoto>,
    val reportedAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState,
    val pendingOperation: PendingOperation?,
    val syncError: SyncError?,
) {
    /** Resolved incidents belong to the operator; the technician can only view them. */
    val isReadOnly: Boolean get() = status == IncidentStatus.RESOLVED

    /** A delete the server refused; shown so the user can retry or discard it. */
    val isFailedDelete: Boolean
        get() = syncState == SyncState.FAILED && pendingOperation == PendingOperation.DELETE
}
