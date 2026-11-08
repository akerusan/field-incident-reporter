package com.akerusan.fieldincidentreporter.core.domain

import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto

/** Photo files kept on the device until they are uploaded. */
interface PhotoStore {
    /**
     * Copies the image at [sourceUri] (a content URI from the camera or the photo picker) into app
     * storage, downscaled and compressed for upload, and returns it as a photo with id [photoId].
     * Returns null if the image can't be read.
     */
    suspend fun importPhoto(sourceUri: String, photoId: String): IncidentPhoto?

    /** A content URI the camera app can write a new picture to; pass it to [importPhoto] after. */
    suspend fun createCaptureTarget(): String

    /** Deletes the local files of [photos]. Photos without a local file are ignored. */
    suspend fun deleteLocalFiles(photos: List<IncidentPhoto>)
}
