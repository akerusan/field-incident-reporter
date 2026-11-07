package com.akerusan.fieldincidentreporter.core.testing

import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.result.DiscardChangesResult
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentFilter
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncState
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * An in-memory [IncidentRepository] for use-case and ViewModel tests. It follows the real
 * repository's visible behavior (pending deletes are hidden, undo restores them) without the
 * version bookkeeping, which is tested in the data module.
 */
class FakeIncidentRepository(initial: List<Incident> = emptyList()) : IncidentRepository {

    private val incidents = MutableStateFlow(initial.associateBy { it.id })
    private val undoDeadlines = MutableStateFlow<Map<String, Instant>>(emptyMap())

    /** What [discardLocalChanges] returns. */
    var discardResult: DiscardChangesResult = DiscardChangesResult.DISCARDED

    val retriedIds = mutableListOf<String>()
    val committedDeleteIds = mutableListOf<String>()

    /** All stored incidents, including hidden pending deletes. */
    val all: List<Incident> get() = incidents.value.values.toList()

    fun set(vararg items: Incident) {
        incidents.value = items.associateBy { it.id }
    }

    override fun observeIncidents(filter: IncidentFilter): Flow<List<Incident>> = incidents.map { map ->
        map.values
            .filterNot { it.isPendingDelete }
            .filter { filter.status == null || it.status == filter.status }
            .sortedByDescending { it.reportedAt }
    }

    override fun observeIncident(id: String): Flow<Incident?> =
        incidents.map { map -> map[id]?.takeUnless { it.isPendingDelete } }

    override fun observeUndoableDeletes(): Flow<List<Incident>> = combine(incidents, undoDeadlines) { map, deadlines ->
        map.values.filter { it.isPendingDelete && it.id in deadlines }
    }.distinctUntilChanged()

    override suspend fun getIncident(id: String): Incident? = incidents.value[id]

    override suspend fun createIncident(incident: Incident) {
        incidents.update { it + (incident.id to incident) }
    }

    override suspend fun updateIncident(id: String, draft: IncidentDraft, updatedAt: Instant): Boolean {
        val current = incidents.value[id] ?: return false
        incidents.update {
            it + (
                id to current.copy(
                    title = draft.title,
                    description = draft.description,
                    category = checkNotNull(draft.category),
                    severity = checkNotNull(draft.severity),
                    status = draft.status,
                    locationNote = draft.locationNote,
                    photos = draft.photos,
                    updatedAt = updatedAt,
                    syncState = SyncState.PENDING,
                    pendingOperation = current.pendingOperation ?: PendingOperation.UPDATE,
                    syncError = null,
                )
                )
        }
        return true
    }

    override suspend fun markDeleted(id: String, undoUntil: Instant): Boolean {
        val current = incidents.value[id] ?: return false
        undoDeadlines.update { it + (id to undoUntil) }
        incidents.update {
            it + (id to current.copy(syncState = SyncState.PENDING, pendingOperation = PendingOperation.DELETE))
        }
        return true
    }

    override suspend fun restoreDeleted(id: String): RestoreResult {
        val current = incidents.value[id]
        if (current == null || !current.isPendingDelete) return RestoreResult.TOO_LATE
        undoDeadlines.update { it - id }
        incidents.update { it + (id to current.copy(pendingOperation = PendingOperation.UPDATE)) }
        return RestoreResult.RESTORED
    }

    override suspend fun commitDeletion(id: String) {
        committedDeleteIds += id
        undoDeadlines.update { it - id }
    }

    override suspend fun retrySync(id: String) {
        retriedIds += id
    }

    override suspend fun discardLocalChanges(id: String): DiscardChangesResult = discardResult

    private val Incident.isPendingDelete: Boolean
        get() = syncState == SyncState.PENDING && pendingOperation == PendingOperation.DELETE
}
