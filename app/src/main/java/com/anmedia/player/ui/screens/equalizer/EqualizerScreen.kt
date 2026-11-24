package com.anmedia.player.ui.screens.equalizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anmedia.player.domain.model.EqPreset
import com.anmedia.player.ui.components.BevelPanel
import com.anmedia.player.ui.components.EqBandSlider
import com.anmedia.player.ui.theme.LocalExtendedColors

@Composable
fun EqualizerScreen(viewModel: EqualizerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.eqState.collectAsStateWithLifecycle()
    val extended = LocalExtendedColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Graphic Equalizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "On", style = MaterialTheme.typography.labelLarge, color = extended.textSecondary)
                Switch(
                    checked = state.enabled,
                    onCheckedChange = viewModel::setEnabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = extended.accent)
                )
            }
        }

        if (!state.supported) {
            Text(
                text = "Equalizer attaches once audio starts playing.",
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary
            )
        }

        BevelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (state.supported) 1f else 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EqBandSlider(
                    label = "PRE",
                    valueDb = state.preampMb / 100f,
                    onValueChange = { if (state.supported) viewModel.setPreamp((it * 100).toInt()) },
                    valueRange = (state.minLevelMb / 100f)..(state.maxLevelMb / 100f).coerceAtLeast(1f)
                )
                state.bands.forEach { band ->
                    EqBandSlider(
                        label = formatFreqLabel(band.centerFreqHz),
                        valueDb = band.levelMb / 100f,
                        onValueChange = { viewModel.setBandLevel(band.index, (it * 100).toInt()) },
                        valueRange = (state.minLevelMb / 100f)..(state.maxLevelMb / 100f).coerceAtLeast(1f)
                    )
                }
            }
        }

        Text(text = "Presets", style = MaterialTheme.typography.labelLarge, color = extended.textSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .alpha(if (state.supported) 1f else 0.4f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqPreset.entries.filter { it != EqPreset.CUSTOM }.forEach { preset ->
                PresetChip(
                    label = preset.label,
                    selected = state.preset == preset,
                    onClick = { if (state.supported) viewModel.applyPreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val extended = LocalExtendedColors.current
    BevelPanel(
        inset = selected,
        modifier = Modifier
            .height(36.dp)
            .padding(2.dp)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .fillMaxSize()
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) extended.accent else extended.textPrimary
            )
        }
    }
}

private fun formatFreqLabel(hz: Int): String =
    if (hz >= 1000) "${hz / 1000}K" else "$hz"
