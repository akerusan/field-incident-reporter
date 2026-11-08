package com.akerusan.fieldincidentreporter.core.data.sync

import com.akerusan.fieldincidentreporter.core.data.mapper.ServerIncident
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentWithPhotos

/** The photo rows to write when the server's copy replaces a local row, and the local files that become obsolete. */
internal data class ServerPhotoMerge(
    val photos: List<IncidentPhotoEntity>,
    val obsoletePhotos: List<IncidentPhotoEntity>,
)

/**
 * Merges the server's photos into [local]: photos still on the server keep their local file (offline
 * thumbnails), and local photos the server no longer has become obsolete. Shared by the pull and
 * "discard my changes".
 */
internal fun mergeServerPhotos(local: IncidentWithPhotos, server: ServerIncident): ServerPhotoMerge {
    val localPathById = local.photos.associate { it.id to it.localPath }
    val photos = server.photos.map { it.copy(localPath = localPathById[it.id]) }
    val serverPhotoIds = photos.map { it.id }.toSet()
    return ServerPhotoMerge(photos = photos, obsoletePhotos = local.photos.filterNot { it.id in serverPhotoIds })
}
