package com.akerusan.fieldincidentreporter.core.data.repository

import com.akerusan.fieldincidentreporter.core.data.di.IoDispatcher
import com.akerusan.fieldincidentreporter.core.data.mapper.IncidentMapper
import com.akerusan.fieldincidentreporter.core.data.mapper.ServerIncident
import com.akerusan.fieldincidentreporter.core.data.mapper.asLocalPhotos
import com.akerusan.fieldincidentreporter.core.data.sync.deleted
import com.akerusan.fieldincidentreporter.core.data.sync.deletionCommitted
import com.akerusan.fieldincidentreporter.core.data.sync.edited
import com.akerusan.fieldincidentreporter.core.data.sync.isBeingDeleted
import com.akerusan.fieldincidentreporter.core.data.sync.isPendingDelete
import com.akerusan.fieldincidentreporter.core.data.sync.mergeServerPhotos
import com.akerusan.fieldincidentreporter.core.data.sync.replacedByServerCopy
import com.akerusan.fieldincidentreporter.core.data.sync.restored
import com.akerusan.fieldincidentreporter.core.data.sync.retried
import com.akerusan.fieldincidentreporter.core.database.DatabaseTransactionRunner
import com.akerusan.fieldincidentreporter.core.database.dao.IncidentDao
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.domain.SyncScheduler
import com.akerusan.fieldincidentreporter.core.domain.result.DiscardChangesResult
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentFilter
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.network.IncidentApi
import java.io.IOException
import java.net.HttpURLConnection
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException

/**
 * Offline-first incidents: every write goes to the local database in one transaction, applying the
 * sync state machine (SyncTransitions.kt), then schedules a background sync. The UI only ever
 * reads the database. The only network call made from here is [discardLocalChanges], which needs
 * the server's copy right away.
 */
@Singleton
class OfflineFirstIncidentRepository @Inject constructor(
    private val dao: IncidentDao,
    private val transaction: DatabaseTransactionRunner,
    private val api: IncidentApi,
    private val mapper: IncidentMapper,
    private val photoStore: PhotoStore,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IncidentRepository {

    // Mapping runs off the main thread: collectors (ViewModels) would otherwise do it there.

    override fun observeIncidents(filter: IncidentFilter): Flow<List<Incident>> =
        dao.observeVisible(filter.status).map { rows -> rows.map(mapper::toDomain) }.flowOn(ioDispatcher)

    override fun observeIncident(id: String): Flow<Incident?> =
        dao.observeVisibleById(id).map { row -> row?.let(mapper::toDomain) }.flowOn(ioDispatcher)

    override fun observeUndoableDeletes(): Flow<List<Incident>> =
        dao.observeUndoableDeletes().map { rows -> rows.map(mapper::toDomain) }.flowOn(ioDispatcher)

    override suspend fun getIncident(id: String): Incident? = dao.getById(id)?.let(mapper::toDomain)

    override suspend fun createIncident(incident: Incident) {
        transaction {
            dao.insert(mapper.toNewEntity(incident))
            dao.insertPhotos(mapper.toPhotoEntities(incident.id, incident.photos))
        }
        syncScheduler.requestSync()
    }

    /** Returns false if the incident doesn't exist or is being deleted (there is nothing to edit). */
    override suspend fun updateIncident(id: String, draft: IncidentDraft, updatedAt: Instant): Boolean {
        val updated = transaction {
            val row = dao.getById(id)
            if (row == null || row.incident.isBeingDeleted) return@transaction false
            dao.update(row.incident.edited(draft, updatedAt))
            dao.replacePhotos(id, mapper.toPhotoEntities(id, draft.photos, existing = row.photos))
            true
        }
        if (updated) syncScheduler.requestSync()
        return updated
    }

    /**
     * One undo at a time: deleting closes the undo window of any earlier delete, which is then sent
     * right away. The new delete itself waits for its own window (see [commitDeletion]). Deleting an
     * incident that is already being deleted is a no-op.
     */
    override suspend fun markDeleted(id: String, undoUntil: Instant): Boolean {
        var closedOtherWindows = false
        val found = transaction {
            val incident = dao.getById(id)?.incident ?: return@transaction false
            if (!incident.isBeingDeleted) {
                closedOtherWindows = dao.closeUndoWindows() > 0
                dao.update(incident.deleted(undoUntil))
            }
            true
        }
        if (closedOtherWindows) syncScheduler.requestSync()
        return found
    }

    override suspend fun restoreDeleted(id: String): RestoreResult {
        val restored = applyTransition(id, guard = { it.isPendingDelete }, transition = IncidentEntity::restored)
        return if (restored) RestoreResult.RESTORED else RestoreResult.TOO_LATE
    }

    override suspend fun commitDeletion(id: String) {
        applyTransition(id, guard = { it.isPendingDelete }, transition = IncidentEntity::deletionCommitted)
    }

    override suspend fun retrySync(id: String) {
        applyTransition(id, guard = { it.syncState == SyncState.FAILED }, transition = IncidentEntity::retried)
    }

    /** Applies [transition] to the row if [guard] allows it, then schedules a sync. Returns whether it applied. */
    private suspend fun applyTransition(
        id: String,
        guard: (IncidentEntity) -> Boolean,
        transition: (IncidentEntity) -> IncidentEntity,
    ): Boolean {
        val applied = transaction {
            val incident = dao.getById(id)?.incident
            if (incident == null || !guard(incident)) return@transaction false
            dao.update(transition(incident))
            true
        }
        if (applied) syncScheduler.requestSync()
        return applied
    }

    override suspend fun discardLocalChanges(id: String): DiscardChangesResult {
        val row = dao.getById(id)
        if (row == null || row.incident.syncState != SyncState.FAILED) {
            return DiscardChangesResult.NOTHING_TO_DISCARD
        }

        // Asked even for a create the server never confirmed: its PUT may have arrived anyway.
        val response = try {
            api.getIncident(id)
        } catch (_: IOException) {
            return DiscardChangesResult.NEEDS_CONNECTION
        } catch (_: SerializationException) {
            return DiscardChangesResult.SERVER_ERROR
        }
        return when {
            response.code() == HttpURLConnection.HTTP_NOT_FOUND -> {
                deleteLocally(id, expectedVersion = row.incident.localVersion)
                DiscardChangesResult.DISCARDED
            }

            response.isSuccessful -> {
                val server = response.body()?.let(mapper::toServerContent)
                    ?: return DiscardChangesResult.SERVER_ERROR
                replaceWithServerCopy(row.incident.localVersion, server)
            }

            else -> DiscardChangesResult.SERVER_ERROR
        }
    }

    /** Overwrites a failed row with the server's version. Skipped if the user changed the row meanwhile. */
    private suspend fun replaceWithServerCopy(expectedVersion: Long, server: ServerIncident): DiscardChangesResult {
        val obsoletePhotos = transaction {
            val current = dao.getById(server.incident.id)
            if (current == null || current.incident.localVersion != expectedVersion) {
                return@transaction null
            }
            val merge = mergeServerPhotos(current, server)
            dao.update(current.incident.replacedByServerCopy(server.incident))
            dao.replacePhotos(server.incident.id, merge.photos)
            merge.obsoletePhotos
        } ?: return DiscardChangesResult.NOTHING_TO_DISCARD
        photoStore.deleteLocalFiles(obsoletePhotos.asLocalPhotos())
        return DiscardChangesResult.DISCARDED
    }

    private suspend fun deleteLocally(id: String, expectedVersion: Long) {
        val photos = transaction {
            val row = dao.getById(id) ?: return@transaction emptyList()
            if (dao.deleteIfVersion(id, expectedVersion) == 0) return@transaction emptyList()
            row.photos
        }
        photoStore.deleteLocalFiles(photos.asLocalPhotos())
    }
}
