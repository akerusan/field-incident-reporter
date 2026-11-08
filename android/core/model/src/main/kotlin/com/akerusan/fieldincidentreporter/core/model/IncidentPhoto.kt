package com.akerusan.fieldincidentreporter.core.model

import kotlinx.serialization.Serializable

/**
 * A photo attached to an incident. A photo taken on this device has a [localPath] until it is
 * cleaned up; once uploaded it also has a [remoteUrl]. Photos pulled from the server only have a
 * [remoteUrl].
 */
@Serializable
data class IncidentPhoto(val id: String, val localPath: String? = null, val remoteUrl: String? = null)
