package com.akerusan.fieldincidentreporter.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.akerusan.fieldincidentreporter.core.designsystem.label.labelRes
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity

/** Severity, color-coded; the two highest levels also get an icon so color isn't the only cue. */
@Composable
fun SeverityPill(severity: IncidentSeverity, modifier: Modifier = Modifier) {
    val colors = FieldIncidentReporterTheme.statusColors
    Pill(
        text = stringResource(severity.labelRes),
        colors = when (severity) {
            IncidentSeverity.LOW -> colors.severityLow
            IncidentSeverity.MEDIUM -> colors.severityMedium
            IncidentSeverity.HIGH -> colors.severityHigh
            IncidentSeverity.CRITICAL -> colors.severityCritical
        },
        icon = when (severity) {
            IncidentSeverity.HIGH -> Icons.Filled.PriorityHigh
            IncidentSeverity.CRITICAL -> Icons.Filled.Warning
            else -> null
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun SeverityPillPreview() {
    FieldIncidentReporterTheme(dynamicColor = false) {
        SeverityPill(IncidentSeverity.CRITICAL)
    }
}
