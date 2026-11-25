package com.anmedia.player.ui.screens.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anmedia.player.BuildConfig
import com.anmedia.player.ui.theme.LocalExtendedColors
import com.anmedia.player.update.UpdateCheckState
import com.anmedia.player.update.UpdateChecker

private const val REPO_URL = "https://github.com/karndiy/an-media"

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val extended = LocalExtendedColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val updateState by UpdateChecker.state.collectAsStateWithLifecycle()

    // painterResource(R.mipmap.ic_launcher) crashes at runtime on API 26+: the launcher icon is
    // an <adaptive-icon> (mipmap-anydpi-v26/ic_launcher.xml), a drawable type painterResource
    // doesn't support. Drawable.toBitmap() works for any Drawable, adaptive icons included.
    val appIconBitmap = remember(context) {
        val sizePx = with(density) { 48.dp.roundToPx() }
        context.applicationInfo.loadIcon(context.packageManager)
            .toBitmap(width = sizePx, height = sizePx)
            .asImageBitmap()
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About AnMedia") },
        text = {
            // AlertDialog's text slot doesn't scroll on its own, and this content (icon row,
            // description, Updates section with variable-length release notes, two buttons) can
            // overflow a real device's available height - without scroll, whatever overflows is
            // just clipped, which is why "View source on GitHub" (the last item) could disappear.
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column {
                        Text(
                            text = "AnMedia",
                            style = MaterialTheme.typography.titleMedium,
                            color = extended.textPrimary
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary
                        )
                    }
                }

                Text(
                    text = "A Winamp-inspired local + YouTube media player with a 5-band " +
                        "equalizer, playlists and background playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )

                // Kept right after the description, before the variable-length Updates section
                // below, so it's always visible on the first screen of the dialog - it previously
                // sat at the very end of the scrollable content, where it could read as "missing"
                // if the Updates section pushed it below the fold (nothing here hints a dialog is
                // scrollable) or on a build predating the scroll fix that cut it off outright.
                TextButton(onClick = { openUrl(REPO_URL) }) {
                    Text("View source on GitHub")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(extended.divider)
                )

                Text(text = "Updates", style = MaterialTheme.typography.labelLarge, color = extended.textPrimary)

                when (val s = updateState) {
                    is UpdateCheckState.Checking -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Checking for updates...",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary
                        )
                    }

                    is UpdateCheckState.Available -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Version ${s.info.versionName} is available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.accent
                        )
                        if (s.info.releaseNotes.isNotBlank()) {
                            Text(
                                text = s.info.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { openUrl(s.info.downloadUrl) }) {
                            Text("Download update (.apk)")
                        }
                    }

                    is UpdateCheckState.UpToDate -> Text(
                        text = "You're on the latest version.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )

                    is UpdateCheckState.Error -> Text(
                        text = "Couldn't check for updates: ${s.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.danger
                    )

                    UpdateCheckState.Idle -> Text(
                        text = "Not checked yet this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                }

                TextButton(onClick = { UpdateChecker.checkNow() }) {
                    Text(if (updateState is UpdateCheckState.Checking) "Checking..." else "Check for updates")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
