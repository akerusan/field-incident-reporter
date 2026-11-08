package com.akerusan.fieldincidentreporter.core.network

import javax.inject.Inject
import javax.inject.Named
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Turns a server-relative path (`/uploads/x.jpg`) into an absolute URL for this device. The server
 * returns relative paths because "localhost" differs between the emulator, a device and a browser.
 */
class RemotePathResolver @Inject constructor(@Named(NetworkConstants.BASE_URL) baseUrl: String) {
    private val base: HttpUrl = baseUrl.toHttpUrl()

    fun toAbsoluteUrl(path: String): String = base.resolve(path)?.toString() ?: path
}
