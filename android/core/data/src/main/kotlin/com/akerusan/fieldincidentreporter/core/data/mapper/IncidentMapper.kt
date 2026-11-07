package com.akerusan.fieldincidentreporter.core.data.mapper

import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentWithPhotos
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.network.RemotePathResolver
import com.akerusan.fieldincidentreporter.core.network.model.IncidentDto
import com.akerusan.fieldincidentreporter.core.network.model.PhotoDto
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Converts between the three representations of an incident: the domain model, the Room row and
 * the API DTO. A photo's location has one name per layer: `remotePath` (server-relative) in the
 * database and the API, `remoteUrl` (absolute, for this device) in the domain. [toRemoteUrl] is the
 * only place one becomes the other.
 */
class IncidentMapper @Inject constructor(private val remotePathResolver: RemotePathResolver) {

    // --- Database -> domain ----------------------------------------------------------------------

    fun toDomain(row: IncidentWithPhotos): Incident = with(row.incident) {
        Incident(
            id = id,
            title = title,
            description = description,
            category = category,
            severity = severity,
            status = status,
            locationNote = locationNote,
            photos = row.orderedPhotos.map { it.toDomain() },
            reportedAt = reportedAt,
            updatedAt = updatedAt,
            syncState = syncState,
            pendingOperation = pendingOperation,
            syncError = syncErrorReason?.let { SyncError(it, syncErrorDetail) },
        )
    }

    private fun IncidentPhotoEntity.toDomain() = IncidentPhoto(
        id = id,
        localPath = localPath,
        remoteUrl = remotePath?.toRemoteUrl(),
    )

    fun String.toRemoteUrl(): String = remotePathResolver.toAbsoluteUrl(this)

    // --- Domain -> database ----------------------------------------------------------------------

    /** A newly reported incident: version 1, never synced. */
    fun toNewEntity(incident: Incident): IncidentEntity = IncidentEntity(
        id = incident.id,
        title = incident.title,
        description = incident.description,
        category = incident.category,
        severity = incident.severity,
        status = incident.status,
        locationNote = incident.locationNote,
        reportedAt = incident.reportedAt,
        updatedAt = incident.updatedAt,
        syncState = incident.syncState,
        pendingOperation = incident.pendingOperation,
        localVersion = 1,
        syncedVersion = 0,
        deleteAfter = null,
        syncErrorReason = null,
        syncErrorDetail = null,
    )

    /**
     * Photo rows for [photos], in order. Photos already stored keep their server path (the domain
     * only carries the absolute URL), looked up by id in [existing].
     */
    fun toPhotoEntities(
        incidentId: String,
        photos: List<IncidentPhoto>,
        existing: List<IncidentPhotoEntity> = emptyList(),
    ): List<IncidentPhotoEntity> {
        val remotePathById = existing.associate { it.id to it.remotePath }
        return photos.mapIndexed { index, photo ->
            IncidentPhotoEntity(
                id = photo.id,
                incidentId = incidentId,
                position = index,
                localPath = photo.localPath,
                remotePath = remotePathById[photo.id],
            )
        }
    }

    // --- API <-> database ------------------------------------------------------------------------

    /** The request body for `PUT /incidents/{id}`. Only photos with a server path are included. */
    fun toDto(incident: IncidentEntity, photos: List<IncidentPhotoEntity>): IncidentDto = IncidentDto(
        id = incident.id,
        title = incident.title,
        description = incident.description,
        category = incident.category.name,
        severity = incident.severity.name,
        status = incident.status.name,
        locationNote = incident.locationNote,
        photos = photos.sortedBy { it.position }.mapNotNull { photo ->
            photo.remotePath?.let { PhotoDto(id = photo.id, path = it) }
        },
        reportedAt = incident.reportedAt.toString(),
        updatedAt = incident.updatedAt.toString(),
    )

    /**
     * The server's content of an incident, or null if the DTO has a value this app doesn't know
     * (an unknown enum, a malformed date). Sync fields are left for the caller to set.
     */
    fun toServerContent(dto: IncidentDto): ServerIncident? {
        val incident = try {
            IncidentEntity(
                id = dto.id,
                title = dto.title,
                description = dto.description,
                category = IncidentCategory.valueOf(dto.category),
                severity = IncidentSeverity.valueOf(dto.severity),
                status = IncidentStatus.valueOf(dto.status),
                locationNote = dto.locationNote,
                reportedAt = Instant.parse(dto.reportedAt),
                updatedAt = Instant.parse(dto.updatedAt),
                syncState = SyncState.SYNCED,
                pendingOperation = null,
                localVersion = 1,
                syncedVersion = 1,
                deleteAfter = null,
                syncErrorReason = null,
                syncErrorDetail = null,
            )
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: DateTimeParseException) {
            return null
        }
        val photos = dto.photos.mapIndexed { index, photo ->
            IncidentPhotoEntity(
                id = photo.id,
                incidentId = dto.id,
                position = index,
                localPath = null,
                remotePath = photo.path,
            )
        }
        return ServerIncident(incident, photos)
    }
}
