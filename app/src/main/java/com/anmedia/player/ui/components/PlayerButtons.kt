package com.anmedia.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anmedia.player.ui.theme.LocalExtendedColors

/** Flat circular icon button - transport controls, macOS toolbar style, no bevel/metal. */
@Composable
fun IconCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    filled: Boolean = false,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val extended = LocalExtendedColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            filled -> extended.accent
            pressed -> extended.surfaceSunken
            else -> Color.Transparent
        },
        label = "iconButtonBackground"
    )
    val contentColor = when {
        filled -> extended.onAccent
        tint != Color.Unspecified -> tint
        else -> LocalContentColor.current
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else contentColor.copy(alpha = 0.35f),
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

/** Same flat button, but stays accent-tinted while [active] - shuffle/repeat style toggles. */
@Composable
fun IconToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    onClick: () -> Unit
) {
    val extended = LocalExtendedColors.current
    IconCircleButton(
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        tint = if (active) extended.accent else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    )
}
