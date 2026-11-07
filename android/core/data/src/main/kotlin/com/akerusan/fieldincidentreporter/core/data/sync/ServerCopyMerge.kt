package com.akerusan.fieldincidentreporter.core.data.sync

import com.akerusan.fieldincidentreporter.core.data.mapper.ServerIncident
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentWithPhotos

/** What to write when the server's copy replaces a local row, and which local photo files it makes obsolete. */
internal data class ServerCopyMerge(
    val incident: IncidentEntity,
    val photos: List<IncidentPhotoEntity>,
    val obsoletePhotos: List<IncidentPhotoEntity>,
)

/**
 * Merges the server's photos into [local]: photos still on the server keep their local file (offline
 * thumbnails), and local photos the server no longer has become obsolete. Used by both the pull and
 * "discard my changes", which differ only in the [incident] row they write.
 */
internal fun mergeServerCopy(
    local: IncidentWithPhotos,
    server: ServerIncident,
    incident: IncidentEntity,
): ServerCopyMerge {
    val localPathById = local.photos.associate { it.id to it.localPath }
    val photos = server.photos.map { it.copy(localPath = localPathById[it.id]) }
    val serverPhotoIds = photos.map { it.id }.toSet()
    return ServerCopyMerge(
        incident = incident,
        photos = photos,
        obsoletePhotos = local.photos.filterNot { it.id in serverPhotoIds },
    )
}
