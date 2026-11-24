package com.anmedia.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.anmedia.player.domain.model.ThemeMode

/** Design tokens Material3's [androidx.compose.material3.ColorScheme] has no slot for. */
data class ExtendedColors(
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val surfaceElevated: Color,
    val surfaceSunken: Color,
    val chipSelectedBg: Color
)

private val LightExtendedColors = ExtendedColors(
    accent = AppPalette.AccentBlueLight,
    onAccent = AppPalette.OnAccent,
    success = AppPalette.SuccessLight,
    danger = AppPalette.DangerLight,
    warning = AppPalette.WarningLight,
    divider = AppPalette.LightDivider,
    textPrimary = AppPalette.LightTextPrimary,
    textSecondary = AppPalette.LightTextSecondary,
    textTertiary = AppPalette.LightTextTertiary,
    surfaceElevated = AppPalette.LightSurfaceElevated,
    surfaceSunken = AppPalette.LightSurfaceSunken,
    chipSelectedBg = AppPalette.AccentBlueLight.copy(alpha = 0.14f)
)

private val DarkExtendedColors = ExtendedColors(
    accent = AppPalette.AccentBlueDark,
    onAccent = AppPalette.OnAccent,
    success = AppPalette.SuccessDark,
    danger = AppPalette.DangerDark,
    warning = AppPalette.WarningDark,
    divider = AppPalette.DarkDivider,
    textPrimary = AppPalette.DarkTextPrimary,
    textSecondary = AppPalette.DarkTextSecondary,
    textTertiary = AppPalette.DarkTextTertiary,
    surfaceElevated = AppPalette.DarkSurfaceElevated,
    surfaceSunken = AppPalette.DarkSurfaceSunken,
    chipSelectedBg = AppPalette.AccentBlueDark.copy(alpha = 0.18f)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * Modern, macOS-inspired look by default (flat surfaces, rounded cards, a single system-blue
 * accent, follows light/dark) - or the original Winamp-style skin when [themeMode] is
 * [ThemeMode.CLASSIC], picked from the Player screen's theme switcher. Every screen reads colors
 * from [LocalExtendedColors]/[MaterialTheme] rather than hardcoding them, so switching themes
 * here is all that's needed; no screen-level changes.
 */
@Composable
fun AnMediaTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.CLASSIC -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val extended = when {
        themeMode == ThemeMode.CLASSIC -> ClassicExtendedColors
        darkTheme -> DarkExtendedColors
        else -> LightExtendedColors
    }

    val colorScheme = when {
        themeMode == ThemeMode.CLASSIC -> darkColorScheme(
            primary = extended.accent,
            onPrimary = extended.onAccent,
            secondary = extended.accent,
            onSecondary = extended.onAccent,
            background = WinampClassicPalette.MetalDark,
            onBackground = extended.textPrimary,
            surface = WinampClassicPalette.MetalMid,
            onSurface = extended.textPrimary,
            surfaceVariant = extended.surfaceElevated,
            onSurfaceVariant = extended.textSecondary,
            error = extended.danger,
            onError = extended.onAccent,
            outline = extended.divider
        )
        darkTheme -> darkColorScheme(
            primary = extended.accent,
            onPrimary = extended.onAccent,
            secondary = extended.accent,
            onSecondary = extended.onAccent,
            background = AppPalette.DarkWindowBg,
            onBackground = extended.textPrimary,
            surface = AppPalette.DarkSurface,
            onSurface = extended.textPrimary,
            surfaceVariant = extended.surfaceElevated,
            onSurfaceVariant = extended.textSecondary,
            error = extended.danger,
            onError = extended.onAccent,
            outline = extended.divider
        )
        else -> lightColorScheme(
            primary = extended.accent,
            onPrimary = extended.onAccent,
            secondary = extended.accent,
            onSecondary = extended.onAccent,
            background = AppPalette.LightWindowBg,
            onBackground = extended.textPrimary,
            surface = AppPalette.LightSurface,
            onSurface = extended.textPrimary,
            surfaceVariant = extended.surfaceElevated,
            onSurfaceVariant = extended.textSecondary,
            error = extended.danger,
            onError = extended.onAccent,
            outline = extended.divider
        )
    }

    val isClassic = themeMode == ThemeMode.CLASSIC
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (isClassic) ClassicTypography else AnMediaTypography,
            shapes = if (isClassic) ClassicShapes else AnMediaShapes,
            content = content
        )
    }
}
