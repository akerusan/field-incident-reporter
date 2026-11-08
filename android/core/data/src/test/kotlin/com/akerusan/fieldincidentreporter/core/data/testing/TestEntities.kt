package com.akerusan.fieldincidentreporter.core.data.testing

import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import java.time.Instant

/** An incident row with sensible defaults; override only what a test cares about. */
fun entity(
    id: String = "incident-1",
    title: String = "Broken pump",
    status: IncidentStatus = IncidentStatus.OPEN,
    syncState: SyncState = SyncState.SYNCED,
    pendingOperation: PendingOperation? = null,
    localVersion: Long = 1,
    syncedVersion: Long = 1,
    deleteAfter: Instant? = null,
    syncErrorReason: SyncError.Reason? = null,
) = IncidentEntity(
    id = id,
    title = title,
    description = "Oil leaking from the main seal",
    category = IncidentCategory.EQUIPMENT,
    severity = IncidentSeverity.HIGH,
    status = status,
    locationNote = "Pump room B",
    reportedAt = Instant.parse("2026-11-06T08:00:00Z"),
    updatedAt = Instant.parse("2026-11-06T08:00:00Z"),
    syncState = syncState,
    pendingOperation = pendingOperation,
    localVersion = localVersion,
    syncedVersion = syncedVersion,
    deleteAfter = deleteAfter,
    syncErrorReason = syncErrorReason,
    syncErrorDetail = syncErrorReason?.let { "detail" },
)

fun photoEntity(
    id: String,
    incidentId: String = "incident-1",
    position: Int = 0,
    localPath: String? = null,
    remotePath: String? = null,
) = IncidentPhotoEntity(id, incidentId, position, localPath, remotePath)

/** The API's JSON for an incident, as the mock server returns it. */
fun incidentJson(
    id: String = "incident-1",
    title: String = "Broken pump",
    status: String = "OPEN",
    photos: String = "[]",
) = """
    {"id":"$id","title":"$title","description":"Oil leaking from the main seal","category":"EQUIPMENT",
     "severity":"HIGH","status":"$status","locationNote":"Pump room B","photos":$photos,
     "reportedAt":"2026-11-06T08:00:00Z","updatedAt":"2026-11-06T08:00:00Z"}
""".trimIndent()
