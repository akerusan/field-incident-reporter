package com.akerusan.fieldincidentreporter.core.network

import com.akerusan.fieldincidentreporter.core.network.model.ErrorDto
import javax.inject.Inject
import kotlinx.serialization.json.Json
import retrofit2.Response

/** Reads the API's `{ code, message }` error body from a failed response, if it has one. */
class ApiErrorParser @Inject constructor(private val json: Json) {
    fun parse(response: Response<*>): ErrorDto? {
        val body = response.errorBody()?.string() ?: return null
        return runCatching { json.decodeFromString<ErrorDto>(body) }.getOrNull()
    }
}
