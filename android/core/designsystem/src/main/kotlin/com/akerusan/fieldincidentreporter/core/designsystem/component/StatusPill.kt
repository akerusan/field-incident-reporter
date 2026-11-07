package com.akerusan.fieldincidentreporter.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.akerusan.fieldincidentreporter.core.designsystem.label.labelRes
import com.akerusan.fieldincidentreporter.core.designsystem.theme.ColorPair
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus

/** Lifecycle status of an incident. */
@Composable
fun StatusPill(status: IncidentStatus, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Pill(
        text = stringResource(status.labelRes),
        colors = when (status) {
            IncidentStatus.OPEN -> ColorPair(scheme.secondaryContainer, scheme.onSecondaryContainer)
            IncidentStatus.IN_PROGRESS -> ColorPair(scheme.primaryContainer, scheme.onPrimaryContainer)
            IncidentStatus.RESOLVED -> FieldIncidentReporterTheme.statusColors.resolved
        },
        icon = if (status == IncidentStatus.RESOLVED) Icons.Filled.CheckCircle else null,
        modifier = modifier,
    )
}
