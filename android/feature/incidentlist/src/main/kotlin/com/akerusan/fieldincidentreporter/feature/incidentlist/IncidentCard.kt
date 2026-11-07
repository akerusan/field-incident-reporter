package com.akerusan.fieldincidentreporter.feature.incidentlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akerusan.fieldincidentreporter.core.designsystem.component.IncidentPhotoImage
import com.akerusan.fieldincidentreporter.core.designsystem.component.SeverityPill
import com.akerusan.fieldincidentreporter.core.designsystem.component.StatusPill
import com.akerusan.fieldincidentreporter.core.designsystem.component.SyncPill
import com.akerusan.fieldincidentreporter.core.designsystem.format.toRelativeTimeText
import com.akerusan.fieldincidentreporter.core.designsystem.label.labelRes
import com.akerusan.fieldincidentreporter.core.designsystem.preview.PreviewIncidents
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.model.Incident

/** One incident in the list: what, where, how bad, and whether the server has it yet. */
@Composable
internal fun IncidentCard(incident: Incident, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val photo = incident.photos.firstOrNull()
            if (photo != null) {
                IncidentPhotoImage(
                    photo = photo,
                    contentDescription = null, // decorative here; the title describes the card
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = incident.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        incident.category.labelRes,
                    ) + " · " + incident.reportedAt.toRelativeTimeText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (incident.locationNote.isNotBlank()) {
                    Text(
                        text = incident.locationNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SeverityPill(incident.severity)
                    StatusPill(incident.status)
                    SyncPill(incident)
                    if (incident.photos.size > 1) PhotoCount(incident.photos.size)
                }
            }
        }
    }
}

@Composable
private fun PhotoCount(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = pluralStringResource(R.plurals.incident_list_photo_count, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Preview
@Composable
private fun IncidentCardPreview() {
    FieldIncidentReporterTheme(dynamicColor = false) {
        IncidentCard(incident = PreviewIncidents.failed, onClick = {})
    }
}
