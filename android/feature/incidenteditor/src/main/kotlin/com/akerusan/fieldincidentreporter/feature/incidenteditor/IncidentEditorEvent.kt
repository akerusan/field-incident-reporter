package com.akerusan.fieldincidentreporter.feature.incidenteditor

/** One-off events for the editor screen to act on. */
sealed interface IncidentEditorEvent {
    /** Open the camera app, writing the picture to [captureUri]. */
    data class LaunchCamera(val captureUri: String) : IncidentEditorEvent

    /** Leave the editor (after a save, a discard, or when there is nothing to edit). */
    data object Close : IncidentEditorEvent

    data object PhotoUnreadable : IncidentEditorEvent

    data object ReadOnly : IncidentEditorEvent
}
