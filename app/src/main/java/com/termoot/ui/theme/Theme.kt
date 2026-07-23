package com.termoot.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TermootColorScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = TextOnPrimary,
    primaryContainer = TerminalGreen.copy(alpha = 0.12f),
    onPrimaryContainer = TerminalGreen,
    secondary = TerminalBlue,
    onSecondary = TextOnPrimary,
    secondaryContainer = TerminalBlue.copy(alpha = 0.12f),
    onSecondaryContainer = TerminalBlue,
    tertiary = TerminalPurple,
    onTertiary = TextOnPrimary,
    tertiaryContainer = TerminalPurple.copy(alpha = 0.12f),
    onTertiaryContainer = TerminalPurple,
    background = BackgroundDark,
    onBackground = OnBackground,
    surface = SurfaceDark,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariant,
    outline = BorderDark,
    outlineVariant = BorderMuted,
    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDark,
    scrim = ScrimColor
)

@Composable
fun TermootTheme(content: @Composable () -> Unit) {
    val colorScheme = TermootColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundDark.toArgb()
            window.navigationBarColor = BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TermootTypography,
        shapes = TermootShapes,
        content = content
    )
}
