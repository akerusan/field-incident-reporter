package com.akerusan.fieldincidentreporter.feature.incidenteditor.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation key of the editor: a new report when [incidentId] is null, otherwise an edit. */
@Serializable
data class IncidentEditorKey(val incidentId: String? = null) : NavKey
