package com.akerusan.fieldincidentreporter.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentWithPhotos
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.SyncError
import kotlinx.coroutines.flow.Flow

/**
 * Incidents and their photos. "Visible" excludes pending deletes, which are hidden while they can
 * still be undone; a delete the server refused (FAILED) is visible again so the user can act on it.
 */
@Dao
interface IncidentDao {

    // --- Reads for the UI ------------------------------------------------------------------------

    @Transaction
    @Query(
        """
        SELECT * FROM incidents
        WHERE NOT (sync_state = 'PENDING' AND pending_operation IS 'DELETE')
          AND (:status IS NULL OR status = :status)
        ORDER BY reported_at DESC
        """,
    )
    fun observeVisible(status: IncidentStatus?): Flow<List<IncidentWithPhotos>>

    @Transaction
    @Query(
        """
        SELECT * FROM incidents
        WHERE id = :id AND NOT (sync_state = 'PENDING' AND pending_operation IS 'DELETE')
        """,
    )
    fun observeVisibleById(id: String): Flow<IncidentWithPhotos?>

    /** Pending deletes that can still be undone, most recent first. */
    @Transaction
    @Query(
        """
        SELECT * FROM incidents
        WHERE sync_state = 'PENDING' AND pending_operation IS 'DELETE' AND delete_after IS NOT NULL
        ORDER BY delete_after DESC
        """,
    )
    fun observeUndoableDeletes(): Flow<List<IncidentWithPhotos>>

    // --- Reads for writes and sync ---------------------------------------------------------------

    @Transaction
    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getById(id: String): IncidentWithPhotos?

    /** Rows with a change waiting to be sent, oldest report first. */
    @Transaction
    @Query("SELECT * FROM incidents WHERE sync_state = 'PENDING' ORDER BY reported_at ASC")
    suspend fun getPending(): List<IncidentWithPhotos>

    @Query("SELECT id, sync_state FROM incidents")
    suspend fun getSyncStates(): List<IncidentSyncStateRow>

    // --- Writes ----------------------------------------------------------------------------------

    @Insert
    suspend fun insert(incident: IncidentEntity)

    @Update
    suspend fun update(incident: IncidentEntity)

    @Upsert
    suspend fun upsert(incident: IncidentEntity)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert
    suspend fun insertPhotos(photos: List<IncidentPhotoEntity>)

    @Query("DELETE FROM incident_photos WHERE incident_id = :incidentId")
    suspend fun deletePhotosOf(incidentId: String)

    @Transaction
    suspend fun replacePhotos(incidentId: String, photos: List<IncidentPhotoEntity>) {
        deletePhotosOf(incidentId)
        insertPhotos(photos)
    }

    /** Records a photo's server path after upload. A sync-side write: no version bump. */
    @Query("UPDATE incident_photos SET remote_path = :remotePath WHERE id = :photoId")
    suspend fun setPhotoRemotePath(photoId: String, remotePath: String)

    // --- Conditional sync confirmations ----------------------------------------------------------
    // Each applies only if the row still has the version the sync sent. Returns the rows changed:
    // 0 means the user wrote to the row meanwhile, so it stays pending for the next sync.

    @Query(
        """
        UPDATE incidents
        SET sync_state = 'SYNCED', pending_operation = NULL, synced_version = :version,
            sync_error_reason = NULL, sync_error_detail = NULL
        WHERE id = :id AND local_version = :version
        """,
    )
    suspend fun markSynced(id: String, version: Long): Int

    /** Keeps the pending operation, so the user can retry or discard it. */
    @Query(
        """
        UPDATE incidents
        SET sync_state = 'FAILED', sync_error_reason = :reason, sync_error_detail = :detail
        WHERE id = :id AND local_version = :version
        """,
    )
    suspend fun markFailed(id: String, version: Long, reason: SyncError.Reason, detail: String?): Int

    @Query("DELETE FROM incidents WHERE id = :id AND local_version = :version")
    suspend fun deleteIfVersion(id: String, version: Long): Int
}
