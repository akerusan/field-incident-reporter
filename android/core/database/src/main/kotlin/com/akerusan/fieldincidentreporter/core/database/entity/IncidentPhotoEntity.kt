package com.akerusan.fieldincidentreporter.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A photo of an incident. [localPath] is the file on this device (until it is cleaned up);
 * [remotePath] is the server's relative path (`/uploads/...`), set once uploaded.
 */
@Entity(
    tableName = "incident_photos",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("incident_id")],
)
data class IncidentPhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "incident_id") val incidentId: String,
    /** Display order within the incident. */
    val position: Int,
    @ColumnInfo(name = "local_path") val localPath: String?,
    @ColumnInfo(name = "remote_path") val remotePath: String?,
)
