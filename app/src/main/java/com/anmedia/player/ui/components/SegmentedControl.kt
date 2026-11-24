package com.anmedia.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anmedia.player.ui.theme.LocalExtendedColors

/** A pill-shaped segmented control, macOS/iOS style - replaces the stock Material [androidx.compose.material3.TabRow]. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    val extended = LocalExtendedColors.current
    val shape = RoundedCornerShape(12.dp)

    val rowModifier = modifier
        .clip(shape)
        .background(extended.surfaceSunken)
        .padding(3.dp)
        .let { if (scrollable) it.horizontalScroll(rememberScrollState()) else it.fillMaxWidth() }

    Row(modifier = rowModifier) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = (if (scrollable) Modifier else Modifier.weight(1f))
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) extended.accent else extended.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
