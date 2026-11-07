package com.akerusan.fieldincidentreporter.core.data.repository

import com.akerusan.fieldincidentreporter.core.data.mapper.IncidentMapper
import com.akerusan.fieldincidentreporter.core.data.mapper.ServerIncident
import com.akerusan.fieldincidentreporter.core.data.mapper.asLocalPhotos
import com.akerusan.fieldincidentreporter.core.data.sync.deleted
import com.akerusan.fieldincidentreporter.core.data.sync.deletionCommitted
import com.akerusan.fieldincidentreporter.core.data.sync.edited
import com.akerusan.fieldincidentreporter.core.data.sync.isPendingDelete
import com.akerusan.fieldincidentreporter.core.data.sync.restored
import com.akerusan.fieldincidentreporter.core.data.sync.retried
import com.akerusan.fieldincidentreporter.core.database.DatabaseTransactionRunner
import com.akerusan.fieldincidentreporter.core.database.dao.IncidentDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
) : IncidentRepository {

    override fun observeIncidents(filter: IncidentFilter): Flow<List<Incident>> =
        dao.observeVisible(filter.status).map { rows -> rows.map(mapper::toDomain) }

    override fun observeIncident(id: String): Flow<Incident?> =
        dao.observeVisibleById(id).map { row -> row?.let(mapper::toDomain) }

    override fun observeUndoableDeletes(): Flow<List<Incident>> =
        dao.observeUndoableDeletes().map { rows -> rows.map(mapper::toDomain) }

    override suspend fun getIncident(id: String): Incident? = dao.getById(id)?.let(mapper::toDomain)

    override suspend fun createIncident(incident: Incident) {
        transaction {
            dao.insert(mapper.toNewEntity(incident))
            dao.insertPhotos(mapper.toPhotoEntities(incident.id, incident.photos))
        }
        syncScheduler.requestSync()
    }

    override suspend fun updateIncident(id: String, draft: IncidentDraft, updatedAt: Instant): Boolean {
        val updated = transaction {
            val row = dao.getById(id) ?: return@transaction false
            dao.update(row.incident.edited(draft, updatedAt))
            dao.replacePhotos(id, mapper.toPhotoEntities(id, draft.photos, existing = row.photos))
            true
        }
        if (updated) syncScheduler.requestSync()
        return updated
    }

    /** No sync is requested: the delete waits for the undo window to close (see [commitDeletion]). */
    override suspend fun markDeleted(id: String, undoUntil: Instant): Boolean = transaction {
        val row = dao.getById(id) ?: return@transaction false
        dao.update(row.incident.deleted(undoUntil))
        true
    }

    override suspend fun restoreDeleted(id: String): RestoreResult {
        val restored = transaction {
            val incident = dao.getById(id)?.incident
            if (incident == null || !incident.isPendingDelete) return@transaction false
            dao.update(incident.restored())
            true
        }
        if (!restored) return RestoreResult.TOO_LATE
        syncScheduler.requestSync()
        return RestoreResult.RESTORED
    }

    override suspend fun commitDeletion(id: String) {
        val committed = transaction {
            val incident = dao.getById(id)?.incident
            if (incident == null || !incident.isPendingDelete) return@transaction false
            dao.update(incident.deletionCommitted())
            true
        }
        if (committed) syncScheduler.requestSync()
    }

    override suspend fun retrySync(id: String) {
        val retried = transaction {
            val incident = dao.getById(id)?.incident
            if (incident == null || incident.syncState != SyncState.FAILED) return@transaction false
            dao.update(incident.retried())
            true
        }
        if (retried) syncScheduler.requestSync()
    }

    override suspend fun discardLocalChanges(id: String): DiscardChangesResult {
        val row = dao.getById(id)
        if (row == null || row.incident.syncState != SyncState.FAILED) {
            return DiscardChangesResult.NOTHING_TO_DISCARD
        }
        if (row.incident.syncedVersion == 0L) {
            // The server never had it: discarding the change means discarding the incident.
            deleteLocally(id, expectedVersion = row.incident.localVersion)
            return DiscardChangesResult.DISCARDED
        }

        val response = try {
            api.getIncident(id)
        } catch (_: IOException) {
            return DiscardChangesResult.NEEDS_CONNECTION
        }
        return when {
            response.code() == HttpURLConnection.HTTP_NOT_FOUND -> {
                deleteLocally(id, expectedVersion = row.incident.localVersion)
                DiscardChangesResult.DISCARDED
            }

            response.isSuccessful -> {
                val server = response.body()?.let(mapper::toServerContent)
                    ?: return DiscardChangesResult.NEEDS_CONNECTION
                replaceWithServerCopy(row.incident.localVersion, server)
            }

            else -> DiscardChangesResult.NEEDS_CONNECTION
        }
    }

    /**
     * Overwrites a failed row with the server's version. The version keeps growing (it never goes
     * back) and is marked as confirmed. Skipped if the user changed the row while we were fetching.
     */
    private suspend fun replaceWithServerCopy(expectedVersion: Long, server: ServerIncident): DiscardChangesResult {
        val removedPhotos = transaction {
            val current = dao.getById(server.incident.id)
            if (current == null || current.incident.localVersion != expectedVersion) {
                return@transaction null
            }
            val newVersion = expectedVersion + 1
            dao.update(server.incident.copy(localVersion = newVersion, syncedVersion = newVersion))
            val localPathById = current.photos.associate { it.id to it.localPath }
            dao.replacePhotos(
                server.incident.id,
                server.photos.map { it.copy(localPath = localPathById[it.id]) },
            )
            val serverPhotoIds = server.photos.map { it.id }.toSet()
            current.photos.filterNot { it.id in serverPhotoIds }
        } ?: return DiscardChangesResult.NOTHING_TO_DISCARD
        photoStore.deleteLocalFiles(removedPhotos.asLocalPhotos())
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
