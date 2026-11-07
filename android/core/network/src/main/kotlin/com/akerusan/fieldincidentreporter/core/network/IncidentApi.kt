package com.akerusan.fieldincidentreporter.core.network

import com.akerusan.fieldincidentreporter.core.network.model.IncidentDto
import com.akerusan.fieldincidentreporter.core.network.model.UploadResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * The incident API (server/openapi.yaml). Writes return [Response] so the caller can tell
 * retryable failures (5xx) from rejections (4xx) without exceptions. Write response bodies are
 * ignored: a successful status is all the sync needs.
 */
interface IncidentApi {
    @GET("incidents")
    suspend fun getIncidents(): List<IncidentDto>

    @GET("incidents/{id}")
    suspend fun getIncident(@Path("id") id: String): Response<IncidentDto>

    /** Idempotent upsert: creates or replaces the incident with this client-generated id. */
    @PUT("incidents/{id}")
    suspend fun putIncident(@Path("id") id: String, @Body incident: IncidentDto): Response<Unit>

    /** A 404 means the incident is already gone. */
    @DELETE("incidents/{id}")
    suspend fun deleteIncident(@Path("id") id: String): Response<Unit>

    @Multipart
    @POST("uploads")
    suspend fun uploadPhoto(@Part file: MultipartBody.Part): Response<UploadResponseDto>
}
