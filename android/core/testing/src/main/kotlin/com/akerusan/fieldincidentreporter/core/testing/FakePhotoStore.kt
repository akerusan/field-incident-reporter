package com.akerusan.fieldincidentreporter.core.testing

import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto

/** Records what was imported and deleted. Import fails (returns null) for [unreadableUris]. */
class FakePhotoStore : PhotoStore {
    val deletedPhotos = mutableListOf<IncidentPhoto>()
    val unreadableUris = mutableSetOf<String>()
    var captureTarget = "content://test.photos/captures/capture.jpg"

    override suspend fun importPhoto(sourceUri: String, photoId: String): IncidentPhoto? =
        if (sourceUri in unreadableUris) null else IncidentPhoto(id = photoId, localPath = "/photos/$photoId.jpg")

    override suspend fun createCaptureTarget(): String = captureTarget

    override suspend fun deleteLocalFiles(photos: List<IncidentPhoto>) {
        deletedPhotos += photos
    }
}
