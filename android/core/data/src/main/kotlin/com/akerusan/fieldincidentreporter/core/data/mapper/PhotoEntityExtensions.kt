package com.akerusan.fieldincidentreporter.core.data.mapper

import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto

/** The photos as the photo store needs them to delete their local files. */
internal fun List<IncidentPhotoEntity>.asLocalPhotos(): List<IncidentPhoto> =
    map { IncidentPhoto(id = it.id, localPath = it.localPath) }
