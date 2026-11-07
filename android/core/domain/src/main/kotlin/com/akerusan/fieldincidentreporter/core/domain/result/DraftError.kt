package com.akerusan.fieldincidentreporter.core.domain.result

/** A reason an incident draft can't be saved. */
enum class DraftError {
    TITLE_MISSING,
    TITLE_TOO_LONG,
    DESCRIPTION_TOO_LONG,
    LOCATION_TOO_LONG,
    CATEGORY_MISSING,
    SEVERITY_MISSING,
    STATUS_NOT_ALLOWED,
    TOO_MANY_PHOTOS,
}
