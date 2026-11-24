package com.anmedia.player.ui.screens.video

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anmedia.player.ui.components.VideoSurface
import com.anmedia.player.ui.screens.player.PlayerViewModel
import com.anmedia.player.ui.theme.LocalExtendedColors

@Composable
fun VideoPlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onFullscreenChange: (Boolean) -> Unit = {}
) {
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = LocalExtendedColors.current

    ApplyFullscreenWindowState(isFullscreen)
    BackHandler(enabled = isFullscreen) { onFullscreenChange(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (state.hasVideo) Color.Black else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (state.hasVideo) {
            VideoSurface(player = player, useController = true, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = { onFullscreenChange(!isFullscreen) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = extended.textTertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "No video playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Pick a local video, or add a YouTube URL in Video mode from the Library tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Hides system bars and rotates to landscape while [isFullscreen], restoring both on exit. */
@Composable
private fun ApplyFullscreenWindowState(isFullscreen: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity ?: return

    LaunchedEffect(isFullscreen) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
