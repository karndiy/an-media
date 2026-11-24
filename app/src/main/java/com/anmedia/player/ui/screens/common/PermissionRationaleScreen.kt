package com.anmedia.player.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anmedia.player.R
import com.anmedia.player.ui.theme.LocalExtendedColors

@Composable
fun PermissionRationaleScreen(onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val extended = LocalExtendedColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_rationale_title),
            style = MaterialTheme.typography.titleLarge,
            color = extended.accent,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.permission_rationale_body),
            color = extended.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = onGrant) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}
