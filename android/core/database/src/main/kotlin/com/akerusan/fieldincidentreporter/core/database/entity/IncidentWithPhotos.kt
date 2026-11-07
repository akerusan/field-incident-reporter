package com.akerusan.fieldincidentreporter.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/** An incident with its photos. Room doesn't order relations, so use [orderedPhotos]. */
data class IncidentWithPhotos(
    @Embedded val incident: IncidentEntity,
    @Relation(parentColumn = "id", entityColumn = "incident_id")
    val photos: List<IncidentPhotoEntity>,
) {
    val orderedPhotos: List<IncidentPhotoEntity> get() = photos.sortedBy { it.position }
}
