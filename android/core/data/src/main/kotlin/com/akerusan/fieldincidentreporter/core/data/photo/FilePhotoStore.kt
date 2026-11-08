package com.akerusan.fieldincidentreporter.core.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.akerusan.fieldincidentreporter.core.data.di.IoDispatcher
import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Keeps photos in app-private storage (`files/photos/<id>.jpg`), downscaled to at most
 * [MAX_DIMENSION_PX] and re-encoded as JPEG. That keeps uploads small on field connections and
 * strips location and device metadata from the original. The EXIF orientation is applied to the
 * pixels first, since the re-encoded file has no EXIF.
 *
 * Camera captures go to a temporary file in `cache/captures/`, shared with the camera app through
 * a FileProvider, and are deleted once imported.
 */
class FilePhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PhotoStore {

    private val photosDir = File(context.filesDir, "photos")
    private val capturesDir = File(context.cacheDir, "captures")
    private val authority = context.packageName + ".photos"

    override suspend fun createCaptureTarget(): String = withContext(ioDispatcher) {
        capturesDir.mkdirs()
        val file = File(capturesDir, "${UUID.randomUUID()}.jpg")
        FileProvider.getUriForFile(context, authority, file).toString()
    }

    override suspend fun importPhoto(sourceUri: String, photoId: String): IncidentPhoto? = withContext(ioDispatcher) {
        val uri = sourceUri.toUri()
        try {
            val bitmap = decodeDownscaled(uri) ?: return@withContext null
            val oriented = bitmap.applyOrientation(readOrientation(uri))
            photosDir.mkdirs()
            val target = File(photosDir, "$photoId.jpg")
            target.outputStream().use { oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            IncidentPhoto(id = photoId, localPath = target.absolutePath)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null // the picker's read grant expired, or the URI isn't readable
        } finally {
            deleteIfCapture(uri)
        }
    }

    override suspend fun deleteLocalFiles(photos: List<IncidentPhoto>) = withContext(ioDispatcher) {
        photos.mapNotNull { it.localPath }
            .map(::File)
            .filter { it.parentFile == photosDir } // never delete outside our own folder
            .forEach { it.delete() }
    }

    private fun decodeDownscaled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longestSide = max(bounds.outWidth, bounds.outHeight)
        if (longestSide <= 0) return null

        // Decode at the largest power-of-two reduction that stays above the target, then scale.
        var sampleSize = 1
        while (longestSide / (sampleSize * 2) >= MAX_DIMENSION_PX) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        val scale = MAX_DIMENSION_PX.toFloat() / max(decoded.width, decoded.height)
        if (scale >= 1f) return decoded
        return decoded.scale((decoded.width * scale).roundToInt(), (decoded.height * scale).roundToInt())
    }

    private fun readOrientation(uri: Uri): Int = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    private fun Bitmap.applyOrientation(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)

            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)

            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply {
                postRotate(90f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply {
                postRotate(270f)
                postScale(-1f, 1f)
            }

            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun deleteIfCapture(uri: Uri) {
        if (uri.authority != authority) return
        val name = uri.lastPathSegment ?: return
        File(capturesDir, File(name).name).delete()
    }

    private companion object {
        const val MAX_DIMENSION_PX = 1920
        const val JPEG_QUALITY = 80
    }
}
