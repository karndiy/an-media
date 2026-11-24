package com.anmedia.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.anmedia.player.ui.theme.LocalExtendedColors

/**
 * A flat, rounded macOS-style card. [selected] swaps it to a tinted "active" look
 * (used for the selected EQ preset chip, etc.) instead of a skeuomorphic inset bevel.
 */
@Composable
fun BevelPanel(
    modifier: Modifier = Modifier,
    inset: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    val extended = LocalExtendedColors.current
    val selected = inset
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) extended.chipSelectedBg else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) extended.accent.copy(alpha = 0.5f) else extended.divider,
                shape = shape
            )
    ) {
        content()
    }
}
