package com.akerusan.fieldincidentreporter.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import java.time.Instant

/**
 * An incident row: its content plus the bookkeeping the offline sync needs.
 *
 * Versioning: [localVersion] grows on every user write and never goes back. [syncedVersion] is the
 * [localVersion] the server last confirmed (0 = never reached the server). A sync confirms a write
 * only if [localVersion] still equals the version it sent, so an edit made during a sync is never
 * marked as synced by mistake.
 */
@Entity(
    tableName = "incidents",
    indices = [Index("sync_state"), Index("reported_at")],
)
data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: IncidentCategory,
    val severity: IncidentSeverity,
    val status: IncidentStatus,
    @ColumnInfo(name = "location_note") val locationNote: String,
    @ColumnInfo(name = "reported_at") val reportedAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "sync_state") val syncState: SyncState,
    @ColumnInfo(name = "pending_operation") val pendingOperation: PendingOperation?,
    @ColumnInfo(name = "local_version") val localVersion: Long,
    @ColumnInfo(name = "synced_version") val syncedVersion: Long,
    /** On a pending delete: non-null while it can still be undone; null once committed. */
    @ColumnInfo(name = "delete_after") val deleteAfter: Instant?,
    @ColumnInfo(name = "sync_error_reason") val syncErrorReason: SyncError.Reason?,
    @ColumnInfo(name = "sync_error_detail") val syncErrorDetail: String?,
)
