package com.anmedia.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The original Winamp-style skin, brought back as a selectable theme alongside the modern look. */
object WinampClassicPalette {
    val MetalDark = Color(0xFF0B1A26)
    val MetalMid = Color(0xFF15304A)
    val MetalLight = Color(0xFF28537A)
    val LcdBackground = Color(0xFF07140F)
    val LcdGreen = Color(0xFF00FF66)
    val LcdGreenDim = Color(0xFF3F9E63)
    val Chrome = Color(0xFFBFD8EC)
    val TextPrimary = Color(0xFFE2F1FA)
    val TextSecondary = Color(0xFF86ABC7)
    val TextTertiary = Color(0xFF4F7390)
    val Danger = Color(0xFFFF5C5C)
    val Warning = Color(0xFFFFB000)
}

internal val ClassicExtendedColors = ExtendedColors(
    accent = WinampClassicPalette.LcdGreen,
    onAccent = WinampClassicPalette.MetalDark,
    success = WinampClassicPalette.LcdGreen,
    danger = WinampClassicPalette.Danger,
    warning = WinampClassicPalette.Warning,
    divider = WinampClassicPalette.MetalLight,
    textPrimary = WinampClassicPalette.TextPrimary,
    textSecondary = WinampClassicPalette.TextSecondary,
    textTertiary = WinampClassicPalette.TextTertiary,
    surfaceElevated = WinampClassicPalette.MetalLight,
    surfaceSunken = WinampClassicPalette.LcdBackground,
    chipSelectedBg = WinampClassicPalette.LcdGreen.copy(alpha = 0.22f)
)

/** Sharp, blocky corners - the opposite of the modern theme's rounded macOS look. */
internal val ClassicShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp)
)

/** Monospace with wide tracking, evoking the original's LCD digital read-out. */
private val ClassicFontFamily = FontFamily.Monospace

internal val ClassicTypography = Typography(
    titleLarge = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.sp),
    titleMedium = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp),
    titleSmall = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 0.5.sp),
    labelLarge = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = ClassicFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
)
