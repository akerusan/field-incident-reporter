package com.akerusan.fieldincidentreporter.core.testing

import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import java.time.Instant

/** Test data builders with sensible defaults; override only what a test cares about. */
fun testIncident(
    id: String = "incident-1",
    title: String = "Broken pump",
    status: IncidentStatus = IncidentStatus.OPEN,
    severity: IncidentSeverity = IncidentSeverity.HIGH,
    photos: List<IncidentPhoto> = emptyList(),
    reportedAt: Instant = Instant.parse("2026-11-06T08:00:00Z"),
    syncState: SyncState = SyncState.SYNCED,
    pendingOperation: PendingOperation? = null,
    syncError: SyncError? = null,
) = Incident(
    id = id,
    title = title,
    description = "Oil leaking from the main seal",
    category = IncidentCategory.EQUIPMENT,
    severity = severity,
    status = status,
    locationNote = "Pump room B",
    photos = photos,
    reportedAt = reportedAt,
    updatedAt = reportedAt,
    syncState = syncState,
    pendingOperation = pendingOperation,
    syncError = syncError,
)

fun testDraft(
    title: String = "Broken pump",
    category: IncidentCategory? = IncidentCategory.EQUIPMENT,
    severity: IncidentSeverity? = IncidentSeverity.HIGH,
    status: IncidentStatus = IncidentStatus.OPEN,
    photos: List<IncidentPhoto> = emptyList(),
) = IncidentDraft(
    title = title,
    description = "Oil leaking from the main seal",
    category = category,
    severity = severity,
    status = status,
    locationNote = "Pump room B",
    photos = photos,
)
