package com.akerusan.fieldincidentreporter.feature.incidentdetail.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation key of the incident detail screen. */
@Serializable
data class IncidentDetailKey(val incidentId: String) : NavKey
