package com.anmedia.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.anmedia.player.ui.theme.LocalExtendedColors

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val extended = LocalExtendedColors.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = extended.accent,
            activeTrackColor = extended.accent,
            inactiveTrackColor = extended.divider,
            disabledThumbColor = extended.textTertiary,
            disabledActiveTrackColor = extended.textTertiary,
            disabledInactiveTrackColor = extended.divider
        )
    )
}

@Composable
fun VerticalAppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -1f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val extended = LocalExtendedColors.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier
            .graphicsLayer { rotationZ = 270f }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = -(placeable.width / 2 - placeable.height / 2),
                        y = -(placeable.height / 2 - placeable.width / 2)
                    )
                }
            },
        colors = SliderDefaults.colors(
            thumbColor = extended.accent,
            activeTrackColor = extended.accent,
            inactiveTrackColor = extended.divider
        )
    )
}

@Composable
fun EqBandSlider(
    label: String,
    valueDb: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -15f..15f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val extended = LocalExtendedColors.current
    Column(
        modifier = modifier.width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%+.1f".format(valueDb),
            style = MaterialTheme.typography.labelSmall,
            color = extended.accent
        )
        Box(modifier = Modifier.fillMaxHeight(0.78f)) {
            VerticalAppSlider(
                value = valueDb,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                modifier = Modifier.fillMaxHeight()
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = extended.textSecondary
        )
    }
}
