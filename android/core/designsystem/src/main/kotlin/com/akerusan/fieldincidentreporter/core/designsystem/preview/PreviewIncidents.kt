package com.akerusan.fieldincidentreporter.core.designsystem.preview

import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import java.time.Instant

/** Sample incidents for Compose previews, covering every sync state. */
object PreviewIncidents {
    private val reportedAt = Instant.parse("2026-11-07T08:30:00Z")

    val synced = Incident(
        id = "preview-1",
        title = "Hydraulic pump leaking oil",
        description = "Steady oil leak from the main seal of pump P-2. Drip tray placed.",
        category = IncidentCategory.EQUIPMENT,
        severity = IncidentSeverity.HIGH,
        status = IncidentStatus.OPEN,
        locationNote = "Pump room B, building 3",
        photos = emptyList(),
        reportedAt = reportedAt,
        updatedAt = reportedAt,
        syncState = SyncState.SYNCED,
        pendingOperation = null,
        syncError = null,
    )

    val pending = synced.copy(
        id = "preview-2",
        title = "Pothole on access road",
        category = IncidentCategory.ROAD_HAZARD,
        severity = IncidentSeverity.MEDIUM,
        status = IncidentStatus.IN_PROGRESS,
        syncState = SyncState.PENDING,
        pendingOperation = PendingOperation.CREATE,
    )

    val failed = synced.copy(
        id = "preview-3",
        title = "Exposed wiring in junction box",
        category = IncidentCategory.ELECTRICAL,
        severity = IncidentSeverity.CRITICAL,
        syncState = SyncState.FAILED,
        pendingOperation = PendingOperation.UPDATE,
        syncError = SyncError(SyncError.Reason.RESOLVED_BY_OPERATOR),
    )

    val resolved = synced.copy(
        id = "preview-4",
        title = "Broken handrail on stairwell",
        category = IncidentCategory.SAFETY,
        severity = IncidentSeverity.LOW,
        status = IncidentStatus.RESOLVED,
    )

    val all = listOf(synced, pending, failed, resolved)
}
