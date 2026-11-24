@file:OptIn(UnstableApi::class)

package com.anmedia.player.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Shared ExoPlayer video surface. [useController] is off for inline previews (e.g. the Player
 * tab's hero area, which has its own transport row) and on for the dedicated, immersive Video tab.
 */
@Composable
fun VideoSurface(
    player: Player?,
    modifier: Modifier = Modifier,
    useController: Boolean = true
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.useController = useController
                controllerShowTimeoutMs = 3000
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowNextButton(false)
                setShowPreviousButton(false)
                setKeepContentOnPlayerReset(true)
            }
        },
        update = { view -> view.player = player },
        modifier = modifier.fillMaxSize()
    )
}
