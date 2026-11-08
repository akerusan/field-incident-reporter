package com.akerusan.fieldincidentreporter.core.data.mapper

import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity

/**
 * An incident as the server has it, converted to database rows. [incident] is marked SYNCED at
 * version 1; callers adjust the version fields when it replaces an existing row.
 */
data class ServerIncident(val incident: IncidentEntity, val photos: List<IncidentPhotoEntity>)
