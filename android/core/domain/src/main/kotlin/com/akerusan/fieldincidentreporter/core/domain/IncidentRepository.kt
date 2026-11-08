package com.akerusan.fieldincidentreporter.core.domain

import com.akerusan.fieldincidentreporter.core.domain.result.DiscardChangesResult
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentFilter
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Incidents, stored on the device first and synced to the server in the background.
 *
 * Every write is local and returns immediately; the implementation schedules the sync. Reads are
 * flows that update when local data changes, including after a sync.
 */
interface IncidentRepository {
    /** Incidents matching [filter], newest first. Incidents awaiting deletion are excluded. */
    fun observeIncidents(filter: IncidentFilter): Flow<List<Incident>>

    /** One incident, or null if it doesn't exist or is awaiting deletion. */
    fun observeIncident(id: String): Flow<Incident?>

    /** Deleted incidents that can still be restored (the undo window). */
    fun observeUndoableDeletes(): Flow<List<Incident>>

    /** One incident in any state, or null. */
    suspend fun getIncident(id: String): Incident?

    /** Stores a new incident and schedules its upload. */
    suspend fun createIncident(incident: Incident)

    /**
     * Replaces the content of an existing incident with [draft] and schedules the upload.
     * Returns false if the incident doesn't exist.
     */
    suspend fun updateIncident(id: String, draft: IncidentDraft, updatedAt: Instant): Boolean

    /**
     * Marks an incident as deleted. It disappears from lists right away; the server delete waits
     * until [undoUntil] or until [commitDeletion], whichever comes first, so it can be undone.
     * Returns false if the incident doesn't exist.
     */
    suspend fun markDeleted(id: String, undoUntil: Instant): Boolean

    /** Undoes [markDeleted], unless the delete already reached the server. */
    suspend fun restoreDeleted(id: String): RestoreResult

    /** Ends the undo window early and schedules the server delete now. */
    suspend fun commitDeletion(id: String)

    /** Sends a failed change again. */
    suspend fun retrySync(id: String)

    /** Drops a failed local change and restores the server's version (needs a connection). */
    suspend fun discardLocalChanges(id: String): DiscardChangesResult
}
