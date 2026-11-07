package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import javax.inject.Inject

/** A content URI for the camera app to write a new photo to. */
class CreateCaptureTargetUseCase @Inject constructor(private val photoStore: PhotoStore) {
    suspend operator fun invoke(): String = photoStore.createCaptureTarget()
}
