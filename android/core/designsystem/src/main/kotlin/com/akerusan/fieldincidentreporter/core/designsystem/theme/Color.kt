package com.akerusan.fieldincidentreporter.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: a deep "utility blue" primary with a safety-amber accent, chosen to read well
// outdoors and to keep amber/red free for severity.

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2A5C8F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF8A5100),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF3B6939),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCF0B4),
    onTertiaryContainer = Color(0xFF002204),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA2C9FF),
    onPrimary = Color(0xFF00315B),
    primaryContainer = Color(0xFF064876),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFFFB870),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFFA1D39A),
    onTertiary = Color(0xFF0A390F),
    tertiaryContainer = Color(0xFF235024),
    onTertiaryContainer = Color(0xFFBCF0B4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
)
