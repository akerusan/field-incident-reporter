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
 * An incident photo. The local file is preferred when it exists, so photos taken on this device show
 * offline and before upload; otherwise the server copy is loaded.
 */
@Composable
fun IncidentPhotoImage(
    photo: IncidentPhoto,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.photo_content_description),
    contentScale: ContentScale = ContentScale.Crop,
) {
    val model: Any? = remember(photo) {
        photo.localPath?.let(::File)?.takeIf { it.exists() } ?: photo.remoteUrl
    }
    var failed by remember(model) { mutableStateOf(model == null) }
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (failed) {
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
                onError = { failed = true },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
