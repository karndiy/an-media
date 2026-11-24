package com.anmedia.player.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw palette, macOS (Sonoma) inspired. [Theme.kt] maps these into a Material3
 * [androidx.compose.material3.ColorScheme] plus [ExtendedColors] for the tokens
 * Material3 has no slot for (dividers, tiered text, success/warning).
 */
object AppPalette {
    val LightWindowBg = Color(0xFFF2F2F7)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFFFFFFF)
    val LightSurfaceSunken = Color(0xFFE9E9EE)
    val LightDivider = Color(0xFFE2E2E7)
    val LightTextPrimary = Color(0xFF1C1C1E)
    val LightTextSecondary = Color(0xFF6E6E73)
    val LightTextTertiary = Color(0xFFAEAEB2)

    val DarkWindowBg = Color(0xFF1C1C1E)
    val DarkSurface = Color(0xFF2C2C2E)
    val DarkSurfaceElevated = Color(0xFF3A3A3C)
    val DarkSurfaceSunken = Color(0xFF161618)
    val DarkDivider = Color(0xFF3F3F42)
    val DarkTextPrimary = Color(0xFFF5F5F7)
    val DarkTextSecondary = Color(0xFF98989D)
    val DarkTextTertiary = Color(0xFF636366)

    val AccentBlueLight = Color(0xFF007AFF)
    val AccentBlueDark = Color(0xFF0A84FF)
    val SuccessLight = Color(0xFF34C759)
    val SuccessDark = Color(0xFF30D158)
    val DangerLight = Color(0xFFFF3B30)
    val DangerDark = Color(0xFFFF453A)
    val WarningLight = Color(0xFFFF9F0A)
    val WarningDark = Color(0xFFFF9F0A)
    val OnAccent = Color(0xFFFFFFFF)
}
