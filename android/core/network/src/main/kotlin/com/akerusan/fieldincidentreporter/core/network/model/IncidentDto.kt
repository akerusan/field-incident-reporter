package com.akerusan.fieldincidentreporter.core.network.model

import kotlinx.serialization.Serializable

/**
 * An incident on the wire (see server/openapi.yaml). Enums and instants are kept as strings here;
 * the data layer parses them, so an unexpected value from the server can't crash deserialization of
 * the whole list.
 */
@Serializable
data class IncidentDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
    val status: String,
    val locationNote: String,
    val photos: List<PhotoDto>,
    val reportedAt: String,
    val updatedAt: String,
)
