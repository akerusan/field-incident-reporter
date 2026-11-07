package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import javax.inject.Inject

/**
 * The user left the editor without saving: delete the files of photos added in this session.
 * Photos that belong to the saved incident ([savedPhotoIds]) are kept.
 */
class DiscardDraftUseCase @Inject constructor(private val photoStore: PhotoStore) {
    suspend operator fun invoke(draft: IncidentDraft, savedPhotoIds: Set<String>) {
        photoStore.deleteLocalFiles(draft.photos.filterNot { it.id in savedPhotoIds })
    }
}
