package com.akerusan.fieldincidentreporter.core.network.model

import kotlinx.serialization.Serializable

/** A photo reference: [path] is the relative path returned by `POST /uploads`. */
@Serializable
data class PhotoDto(val id: String, val path: String)
