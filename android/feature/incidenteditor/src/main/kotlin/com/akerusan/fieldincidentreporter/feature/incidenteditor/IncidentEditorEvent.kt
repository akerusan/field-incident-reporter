package com.akerusan.fieldincidentreporter.feature.incidenteditor

/** One-off events for the editor screen to act on. */
sealed interface IncidentEditorEvent {
    /** Open the camera app, writing the picture to [captureUri]. */
    data class LaunchCamera(val captureUri: String) : IncidentEditorEvent

    /** Leave the editor (after a save, a discard, or when there is nothing to edit). */
    data object Close : IncidentEditorEvent

    data object PhotoUnreadable : IncidentEditorEvent

    /** More photos were picked than the incident has room for; the extras were not added. */
    data object PhotoLimitReached : IncidentEditorEvent

    /** The incident is resolved, so it can't be edited. */
    data object ReadOnly : IncidentEditorEvent

    /** The incident was deleted while it was being edited, so the changes couldn't be saved. */
    data object Deleted : IncidentEditorEvent
}
