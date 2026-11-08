package com.akerusan.fieldincidentreporter.core.designsystem.label

import androidx.annotation.StringRes
import com.akerusan.fieldincidentreporter.core.designsystem.R
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus

/** User-facing labels for the domain enums, shared by every screen. */

@get:StringRes
val IncidentCategory.labelRes: Int
    get() = when (this) {
        IncidentCategory.EQUIPMENT -> R.string.category_equipment
        IncidentCategory.ROAD_HAZARD -> R.string.category_road_hazard
        IncidentCategory.ELECTRICAL -> R.string.category_electrical
        IncidentCategory.PLUMBING -> R.string.category_plumbing
        IncidentCategory.SAFETY -> R.string.category_safety
        IncidentCategory.OTHER -> R.string.category_other
    }

@get:StringRes
val IncidentSeverity.labelRes: Int
    get() = when (this) {
        IncidentSeverity.LOW -> R.string.severity_low
        IncidentSeverity.MEDIUM -> R.string.severity_medium
        IncidentSeverity.HIGH -> R.string.severity_high
        IncidentSeverity.CRITICAL -> R.string.severity_critical
    }

@get:StringRes
val IncidentStatus.labelRes: Int
    get() = when (this) {
        IncidentStatus.OPEN -> R.string.status_open
        IncidentStatus.IN_PROGRESS -> R.string.status_in_progress
        IncidentStatus.RESOLVED -> R.string.status_resolved
    }
