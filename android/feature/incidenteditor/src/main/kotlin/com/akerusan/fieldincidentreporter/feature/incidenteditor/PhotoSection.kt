package com.akerusan.fieldincidentreporter.feature.incidenteditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.akerusan.fieldincidentreporter.core.designsystem.component.IncidentPhotoImage
import com.akerusan.fieldincidentreporter.core.domain.usecase.ValidateIncidentDraftUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto

/** Attached photos, with buttons to take one with the camera or choose from the gallery. */
@Composable
internal fun PhotoSection(
    photos: List<IncidentPhoto>,
    canAddPhoto: Boolean,
    isImporting: Boolean,
    error: String?,
    onTakePhoto: () -> Unit,
    onChoosePhotos: () -> Unit,
    onRemovePhoto: (IncidentPhoto) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                R.string.incident_editor_field_photos,
                photos.size,
                ValidateIncidentDraftUseCase.MAX_PHOTOS,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        if (photos.isNotEmpty() || isImporting) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos, key = { it.id }) { photo ->
                    PhotoThumbnail(photo = photo, onRemove = { onRemovePhoto(photo) })
                }
                if (isImporting) {
                    item {
                        val description = stringResource(R.string.incident_editor_adding_photo)
                        Box(modifier = Modifier.size(PHOTO_SIZE), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.semantics {
                                    contentDescription = description
                                },
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTakePhoto, enabled = canAddPhoto) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Text(stringResource(R.string.incident_editor_take_photo), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onChoosePhotos, enabled = canAddPhoto) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Text(stringResource(R.string.incident_editor_choose_photos), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: IncidentPhoto, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(PHOTO_SIZE)) {
        IncidentPhotoImage(
            photo = photo,
            modifier = Modifier
                .size(PHOTO_SIZE)
                .clip(RoundedCornerShape(8.dp)),
        )
        FilledTonalIconButton(
            onClick = onRemove,
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.incident_editor_remove_photo),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val PHOTO_SIZE = 96.dp
