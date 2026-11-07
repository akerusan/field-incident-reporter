package com.akerusan.fieldincidentreporter.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the JSON configuration and the API client, so production and tests use the same settings. */
object NetworkFactory {
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun incidentApi(baseUrl: String, client: OkHttpClient, json: Json): IncidentApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(IncidentApi::class.java)
}
