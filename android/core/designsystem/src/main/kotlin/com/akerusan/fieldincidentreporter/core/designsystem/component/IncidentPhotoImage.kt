package com.akerusan.fieldincidentreporter.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.akerusan.fieldincidentreporter.core.designsystem.R
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import java.io.File

/**
 * An incident photo. The local file is tried first, so photos taken on this device show offline and
 * before upload; if it can't be loaded (it was cleaned up, for example), the server copy is. Coil
 * does the file access off the main thread.
 */
@Composable
fun IncidentPhotoImage(
    photo: IncidentPhoto,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.photo_content_description),
    contentScale: ContentScale = ContentScale.Crop,
) {
    val sources: List<Any> = remember(photo) { listOfNotNull(photo.localPath?.let(::File), photo.remoteUrl) }
    var sourceIndex by remember(photo) { mutableStateOf(0) }
    val model = sources.getOrNull(sourceIndex)
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (model == null) {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = stringResource(R.string.photo_unavailable),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onError = { sourceIndex++ }, // next source, or the placeholder when none is left
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
