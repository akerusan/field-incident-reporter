package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import javax.inject.Inject

/** The rules an incident must meet before it can be saved. Returns every problem found. */
class ValidateIncidentDraftUseCase @Inject constructor() {
    operator fun invoke(draft: IncidentDraft): Set<DraftError> = buildSet {
        val title = draft.title.trim()
        if (title.isEmpty()) add(DraftError.TITLE_MISSING)
        if (title.length > MAX_TITLE_LENGTH) add(DraftError.TITLE_TOO_LONG)
        if (draft.description.length > MAX_DESCRIPTION_LENGTH) add(DraftError.DESCRIPTION_TOO_LONG)
        if (draft.locationNote.length > MAX_LOCATION_LENGTH) add(DraftError.LOCATION_TOO_LONG)
        if (draft.category == null) add(DraftError.CATEGORY_MISSING)
        if (draft.severity == null) add(DraftError.SEVERITY_MISSING)
        if (draft.status !in IncidentStatus.technicianSelectable) add(DraftError.STATUS_NOT_ALLOWED)
        if (draft.photos.size > MAX_PHOTOS) add(DraftError.TOO_MANY_PHOTOS)
    }

    companion object {
        const val MAX_TITLE_LENGTH = 80
        const val MAX_DESCRIPTION_LENGTH = 2_000
        const val MAX_LOCATION_LENGTH = 200
        const val MAX_PHOTOS = 6
    }
}
