package com.anmedia.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anmedia.player.di.ViewModelFactory
import com.anmedia.player.permissions.rememberAnMediaPermissionsState
import com.anmedia.player.ui.navigation.AnMediaNavHost
import com.anmedia.player.ui.screens.common.PermissionRationaleScreen
import com.anmedia.player.ui.screens.player.PlayerViewModel
import com.anmedia.player.ui.theme.AnMediaTheme
import com.anmedia.player.ui.theme.ThemeController

class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as AnMediaApp).container
        viewModelFactory = ViewModelFactory(container)
        playerViewModel = ViewModelProvider(this, viewModelFactory)[PlayerViewModel::class.java]

        setContent {
            val themeMode by ThemeController.themeMode.collectAsStateWithLifecycle()
            AnMediaTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val permissionsState = rememberAnMediaPermissionsState()

                    LaunchedEffect(Unit) {
                        if (!permissionsState.allEssentialGranted) {
                            permissionsState.requestAll()
                        }
                    }

                    if (permissionsState.allEssentialGranted) {
                        AnMediaNavHost(viewModelFactory = viewModelFactory, playerViewModel = playerViewModel)
                    } else {
                        PermissionRationaleScreen(onGrant = permissionsState.requestAll)
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        maybeEnterPictureInPicture()
    }

    private fun maybeEnterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return

        val state = playerViewModel.uiState.value
        if (state.hasVideo && state.isPlaying) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            runCatching { enterPictureInPictureMode(params) }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }
}
