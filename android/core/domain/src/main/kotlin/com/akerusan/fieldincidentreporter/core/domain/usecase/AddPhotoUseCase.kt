package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.IdGenerator
import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import javax.inject.Inject

/**
 * Imports a photo from the camera or the photo picker into app storage, ready to attach.
 * Returns null if the image can't be read.
 */
class AddPhotoUseCase @Inject constructor(private val photoStore: PhotoStore, private val idGenerator: IdGenerator) {
    suspend operator fun invoke(sourceUri: String): IncidentPhoto? =
        photoStore.importPhoto(sourceUri, photoId = idGenerator.newId())
}
