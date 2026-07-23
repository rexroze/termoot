package com.termoot.ui.theme

import androidx.compose.ui.graphics.Color

// Backgrounds
val BackgroundDark = Color(0xFF0D1117)
val SurfaceDark = Color(0xFF161B22)
val SurfaceVariantDark = Color(0xFF21262D)
val BorderDark = Color(0xFF30363D)
val BorderMuted = Color(0xFF21262D)

// Terminal accent colors
val TerminalGreen = Color(0xFF3FB950)
val TerminalBlue = Color(0xFF58A6FF)
val TerminalYellow = Color(0xFFD29922)
val TerminalRed = Color(0xFFF85149)
val TerminalPurple = Color(0xFFBC8CFF)
val TerminalCyan = Color(0xFF39D4C5)
val TerminalOrange = Color(0xFFF0883E)

// Text colors
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)
val TextDisabled = Color(0xFF484F58)
val TextOnPrimary = Color(0xFF0D1117)

// On-colors for surfaces
val OnBackground = TextPrimary
val OnSurface = TextPrimary
val OnSurfaceVariant = TextSecondary

// Error
val ErrorColor = Color(0xFFF85149)
val OnError = Color(0xFF0D1117)
val ErrorContainer = Color(0xFF3D1414)
val OnErrorContainer = Color(0xFFFFB1B1)

// Success
val SuccessColor = TerminalGreen
val SuccessContainer = Color(0xFF0E3A1A)

// Dim overlays
val ScrimColor = Color(0x80000000)

// Workspace accent palette — index-based lookup for colorIndex
val WorkspaceAccentColors = listOf(
    TerminalGreen,  // 0 — default
    TerminalBlue,   // 1
    TerminalYellow, // 2
    TerminalRed,    // 3
    TerminalPurple, // 4
    TerminalCyan    // 5
)

fun workspaceAccentColor(index: Int): Color =
    WorkspaceAccentColors.getOrElse(index % WorkspaceAccentColors.size) { TerminalGreen }
