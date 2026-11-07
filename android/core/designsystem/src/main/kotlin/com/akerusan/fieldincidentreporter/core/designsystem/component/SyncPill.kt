package com.akerusan.fieldincidentreporter.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.akerusan.fieldincidentreporter.core.designsystem.R
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.SyncState

/** Shows whether the server has this incident yet. Nothing is shown once it is synced. */
@Composable
fun SyncPill(incident: Incident, modifier: Modifier = Modifier) {
    val colors = FieldIncidentReporterTheme.statusColors
    when (incident.syncState) {
        SyncState.SYNCED -> Unit

        SyncState.PENDING -> Pill(
            text = stringResource(R.string.sync_pending),
            colors = colors.syncPending,
            icon = Icons.Filled.CloudOff,
            modifier = modifier,
        )

        SyncState.FAILED -> Pill(
            text = stringResource(if (incident.isFailedDelete) R.string.sync_failed_delete else R.string.sync_failed),
            colors = colors.syncFailed,
            icon = Icons.Filled.ErrorOutline,
            modifier = modifier,
        )
    }
}
