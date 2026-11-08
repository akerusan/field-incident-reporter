package com.akerusan.fieldincidentreporter.core.network.model

import kotlinx.serialization.Serializable

/** The API's error body: a stable [code] and a diagnostic [message]. */
@Serializable
data class ErrorDto(val code: String, val message: String) {
    companion object {
        const val CODE_INCIDENT_RESOLVED = "INCIDENT_RESOLVED"
    }
}
