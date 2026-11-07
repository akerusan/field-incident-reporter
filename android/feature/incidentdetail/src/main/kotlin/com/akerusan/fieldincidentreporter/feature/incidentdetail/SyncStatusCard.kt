package com.akerusan.fieldincidentreporter.feature.incidentdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akerusan.fieldincidentreporter.core.designsystem.theme.ColorPair
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncError
import com.akerusan.fieldincidentreporter.core.model.SyncState

/**
 * Explains where the incident is in the sync, and what the user can do about a failure. Nothing is
 * shown once the incident is synced.
 */
@Composable
internal fun SyncStatusCard(
    incident: Incident,
    isDiscarding: Boolean,
    onRetry: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    val colors = FieldIncidentReporterTheme.statusColors
    when (incident.syncState) {
        SyncState.SYNCED -> Unit

        SyncState.PENDING -> StatusCard(
            colors = colors.syncPending,
            title = stringResource(R.string.incident_detail_pending_title),
            message = stringResource(
                if (incident.pendingOperation == PendingOperation.DELETE) {
                    R.string.incident_detail_pending_delete_message
                } else {
                    R.string.incident_detail_pending_message
                },
            ),
        )

        SyncState.FAILED -> {
            val error = incident.syncError
            val resolvedByOperator = error?.reason == SyncError.Reason.RESOLVED_BY_OPERATOR
            StatusCard(
                colors = colors.syncFailed,
                title = stringResource(
                    if (incident.isFailedDelete) {
                        R.string.incident_detail_failed_delete_title
                    } else {
                        R.string.incident_detail_failed_title
                    },
                ),
                message = when (error?.reason) {
                    SyncError.Reason.RESOLVED_BY_OPERATOR -> stringResource(R.string.incident_detail_failed_resolved)

                    SyncError.Reason.REJECTED_BY_SERVER ->
                        stringResource(R.string.incident_detail_failed_rejected, error.detail.orEmpty())

                    SyncError.Reason.PHOTO_MISSING -> stringResource(R.string.incident_detail_failed_photo_missing)

                    SyncError.Reason.SERVER_ERROR, null -> stringResource(R.string.incident_detail_failed_server)
                },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Retrying can't help when an operator resolved the incident, so Discard leads.
                    if (resolvedByOperator) {
                        Button(onClick = onDiscardChanges, enabled = !isDiscarding) { DiscardLabel(isDiscarding) }
                    } else {
                        Button(onClick = onRetry) { Text(stringResource(R.string.incident_detail_retry)) }
                        OutlinedButton(onClick = onDiscardChanges, enabled = !isDiscarding) {
                            DiscardLabel(isDiscarding)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscardLabel(isDiscarding: Boolean) {
    if (isDiscarding) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        Text(stringResource(R.string.incident_detail_discard))
    }
}

@Composable
private fun StatusCard(colors: ColorPair, title: String, message: String, actions: @Composable () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.container,
        contentColor = colors.content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}
