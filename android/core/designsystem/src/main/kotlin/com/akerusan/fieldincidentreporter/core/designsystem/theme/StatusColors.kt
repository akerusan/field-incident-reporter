package com.akerusan.fieldincidentreporter.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A background and the content color to use on it. */
@Immutable
data class ColorPair(val container: Color, val content: Color)

/**
 * Colors with a fixed meaning (severity, sync state) that Material's scheme doesn't cover. They
 * don't follow dynamic color: "critical" must look the same on every device.
 */
@Immutable
data class StatusColors(
    val severityLow: ColorPair,
    val severityMedium: ColorPair,
    val severityHigh: ColorPair,
    val severityCritical: ColorPair,
    val syncPending: ColorPair,
    val syncFailed: ColorPair,
    val resolved: ColorPair,
)

internal val LightStatusColors = StatusColors(
    severityLow = ColorPair(Color(0xFFD8F0D2), Color(0xFF1B4D17)),
    severityMedium = ColorPair(Color(0xFFFFE9B0), Color(0xFF5C4300)),
    severityHigh = ColorPair(Color(0xFFFFD6BA), Color(0xFF6B2C00)),
    severityCritical = ColorPair(Color(0xFFFFD2CC), Color(0xFF7F0E06)),
    syncPending = ColorPair(Color(0xFFDDE3F0), Color(0xFF2D3A52)),
    syncFailed = ColorPair(Color(0xFFFFDAD6), Color(0xFF93000A)),
    resolved = ColorPair(Color(0xFFD8F0D2), Color(0xFF1B4D17)),
)

internal val DarkStatusColors = StatusColors(
    severityLow = ColorPair(Color(0xFF234D20), Color(0xFFC3EBBA)),
    severityMedium = ColorPair(Color(0xFF574000), Color(0xFFFFE08A)),
    severityHigh = ColorPair(Color(0xFF6B3006), Color(0xFFFFCCA8)),
    severityCritical = ColorPair(Color(0xFF7F1A12), Color(0xFFFFCDC6)),
    syncPending = ColorPair(Color(0xFF39445A), Color(0xFFDDE3F0)),
    syncFailed = ColorPair(Color(0xFF93000A), Color(0xFFFFDAD6)),
    resolved = ColorPair(Color(0xFF234D20), Color(0xFFC3EBBA)),
)

/** Provided by [FieldIncidentReporterTheme]; read it through `FieldIncidentReporterTheme.statusColors`. */
internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
