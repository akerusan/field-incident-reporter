package com.akerusan.fieldincidentreporter.core.data.sync

import com.akerusan.fieldincidentreporter.core.data.mapper.IncidentMapper
import com.akerusan.fieldincidentreporter.core.data.mapper.asLocalPhotos
import com.akerusan.fieldincidentreporter.core.database.DatabaseTransactionRunner
import com.akerusan.fieldincidentreporter.core.database.dao.IncidentDao
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentWithPhotos
import com.akerusan.fieldincidentreporter.core.domain.Clock
import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.network.ApiErrorParser
import com.akerusan.fieldincidentreporter.core.network.IncidentApi
import com.akerusan.fieldincidentreporter.core.network.model.ErrorDto
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * One sync run: **push** every pending local change, then **pull** the server's incidents.
 *
 * Push and pull run in the same run, in that order, so a pull can never work from a list fetched
 * before a push it races with. Runs never overlap: they share a single WorkManager unique work
 * name (see WorkManagerSyncScheduler).
 *
 * Each row is independent: a failure on one never stops the others. Failures are classified as
 *  - **network** (no connection, timeout): the row stays pending and the run is retried later;
 *  - **server error** (5xx, 429, a malformed response): retried with backoff; after
 *    [MAX_SERVER_ERRORS] in a row the change is marked FAILED. The count lives on the row, so
 *    rescheduling the sync never resets it;
 *  - **rejected** (other 4xx, including 409 for resolved incidents): marked FAILED right away,
 *    since retrying the same request can't succeed; the user can retry or discard.
 */
class IncidentSynchronizer @Inject constructor(
    private val dao: IncidentDao,
    private val transaction: DatabaseTransactionRunner,
    private val api: IncidentApi,
    private val mapper: IncidentMapper,
    private val errorParser: ApiErrorParser,
    private val photoStore: PhotoStore,
    private val clock: Clock,
) {
    enum class Outcome { SUCCESS, RETRY }

    suspend fun sync(): Outcome {
        var retry = false
        for (row in dao.getPending()) {
            when (val result = push(row)) {
                PushResult.Done, PushResult.Skipped -> Unit
                PushResult.NetworkError -> retry = true
                is PushResult.ServerError -> if (!recordServerError(row, result.detail)) retry = true
            }
        }
        if (!pull()) retry = true
        return if (retry) Outcome.RETRY else Outcome.SUCCESS
    }

    /** Counts a server error on the row; returns true once the change has been marked FAILED. */
    private suspend fun recordServerError(row: IncidentWithPhotos, detail: String): Boolean {
        val incident = row.incident
        if (incident.serverErrorCount + 1 < MAX_SERVER_ERRORS) {
            dao.recordServerError(incident.id, incident.localVersion)
            return false
        }
        dao.markFailed(incident.id, incident.localVersion, SyncError.Reason.SERVER_ERROR, detail)
        return true
    }

    // --- Push ------------------------------------------------------------------------------------

    private sealed interface PushResult {
        /** Confirmed or marked FAILED; nothing left to do for now. */
        data object Done : PushResult

        /** Not due yet (a delete still inside its undo window). */
        data object Skipped : PushResult

        data object NetworkError : PushResult

        data class ServerError(val detail: String) : PushResult
    }

    private suspend fun push(row: IncidentWithPhotos): PushResult {
        val incident = row.incident
        // The version this push sends. Confirmations below only apply if it is still current.
        val version = incident.localVersion
        return try {
            when (checkNotNull(incident.pendingOperation)) {
                PendingOperation.CREATE, PendingOperation.UPDATE -> pushUpsert(row, version)
                PendingOperation.DELETE -> pushDelete(row, version)
            }
        } catch (_: IOException) {
            PushResult.NetworkError
        } catch (_: SerializationException) {
            PushResult.ServerError(MALFORMED_RESPONSE)
        }
    }

    private suspend fun pushUpsert(row: IncidentWithPhotos, version: Long): PushResult {
        val incident = row.incident
        val photos = row.orderedPhotos.map { photo ->
            if (photo.remotePath != null) return@map photo
            val file = photo.localPath?.let(::File)?.takeIf { it.exists() }
            if (file == null) {
                // Sending the incident without it would lose the photo silently: let the user decide.
                dao.markFailed(incident.id, version, SyncError.Reason.PHOTO_MISSING, detail = null)
                return PushResult.Done
            }
            val response = api.uploadPhoto(file.asMultipartPart())
            if (!response.isSuccessful) return handleFailure(row, version, response)
            val path = response.body()?.path ?: return PushResult.ServerError(MALFORMED_RESPONSE)
            dao.setPhotoRemotePath(photo.id, path) // kept even if this push fails later
            photo.copy(remotePath = path)
        }
        val response = api.putIncident(incident.id, mapper.toDto(incident, photos))
        if (!response.isSuccessful) return handleFailure(row, version, response)
        dao.markSynced(incident.id, version)
        return PushResult.Done
    }

    private suspend fun pushDelete(row: IncidentWithPhotos, version: Long): PushResult {
        val incident = row.incident
        val deleteAfter = incident.deleteAfter
        if (deleteAfter != null && deleteAfter.isAfter(clock.now())) return PushResult.Skipped

        // Sent even if the server never confirmed a create: the PUT may have reached it with its
        // response lost. A 404 (it never got there) is as good as a success.
        val response = api.deleteIncident(incident.id)
        val alreadyGone = response.code() == HttpURLConnection.HTTP_NOT_FOUND
        if (!response.isSuccessful && !alreadyGone) return handleFailure(row, version, response)

        // Conditional: if the user pressed Undo meanwhile, the row survives with a newer version
        // and the next push re-creates it on the server (PUT is an upsert).
        if (dao.deleteIfVersion(incident.id, version) == 1) {
            photoStore.deleteLocalFiles(row.photos.asLocalPhotos())
        }
        return PushResult.Done
    }

    /** Classifies an unsuccessful response: retryable server error, or a rejection recorded on the row. */
    private suspend fun handleFailure(row: IncidentWithPhotos, version: Long, response: Response<*>): PushResult {
        val code = response.code()
        val error = errorParser.parse(response)
        val detail = error?.message ?: "HTTP $code"
        if (isRetryable(code)) return PushResult.ServerError(detail)
        val reason = if (code == HttpURLConnection.HTTP_CONFLICT && error?.code == ErrorDto.CODE_INCIDENT_RESOLVED) {
            SyncError.Reason.RESOLVED_BY_OPERATOR
        } else {
            SyncError.Reason.REJECTED_BY_SERVER
        }
        dao.markFailed(row.incident.id, version, reason, detail)
        return PushResult.Done
    }

    // --- Pull ------------------------------------------------------------------------------------

    /**
     * Applies the server's list. Rows with local changes (PENDING or FAILED) are never touched, so
     * the user's work is never lost to a pull. Returns false if the list couldn't be fetched for a
     * reason worth retrying.
     */
    private suspend fun pull(): Boolean {
        val remote = try {
            api.getIncidents()
        } catch (_: IOException) {
            return false
        } catch (e: HttpException) {
            return !isRetryable(e.code())
        } catch (_: SerializationException) {
            return false
        }

        val obsoletePhotos = transaction {
            val localById = dao.getAll().associateBy { it.incident.id }
            val obsolete = mutableListOf<IncidentPhotoEntity>()
            val remoteIds = mutableSetOf<String>()

            for (dto in remote) {
                remoteIds += dto.id
                val server = mapper.toServerContent(dto) ?: continue
                val local = localById[dto.id]
                when (local?.incident?.syncState) {
                    null -> {
                        dao.insert(server.incident)
                        dao.insertPhotos(server.photos)
                    }

                    SyncState.SYNCED -> {
                        val incident = local.incident.updatedFromServer(server.incident)
                        val merge = mergeServerPhotos(local, server)
                        if (incident != local.incident || merge.photos != local.orderedPhotos) {
                            dao.update(incident)
                            dao.replacePhotos(dto.id, merge.photos)
                            obsolete += merge.obsoletePhotos
                        }
                    }

                    SyncState.PENDING, SyncState.FAILED -> Unit // local changes win until sent
                }
            }

            // Synced rows the server no longer has were deleted elsewhere.
            localById.values
                .filter { it.incident.syncState == SyncState.SYNCED && it.incident.id !in remoteIds }
                .forEach { row ->
                    obsolete += row.photos
                    dao.deleteById(row.incident.id)
                }
            obsolete
        }
        photoStore.deleteLocalFiles(obsoletePhotos.asLocalPhotos())
        return true
    }

    private fun File.asMultipartPart(): MultipartBody.Part =
        MultipartBody.Part.createFormData("file", name, asRequestBody(JPEG))

    companion object {
        /** Consecutive server errors before a change is marked FAILED. */
        const val MAX_SERVER_ERRORS = 5

        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val MALFORMED_RESPONSE = "Unexpected response from the server"
        private val JPEG = "image/jpeg".toMediaType()

        /** Errors worth retrying as-is: the server is struggling, not refusing the request. */
        private fun isRetryable(code: Int): Boolean =
            code >= HttpURLConnection.HTTP_INTERNAL_ERROR || code == HTTP_TOO_MANY_REQUESTS
    }
}
