package com.anmedia.player.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class AnMediaPermissionsState(
    val mediaGranted: Boolean,
    val recordAudioGranted: Boolean,
    val notificationsGranted: Boolean,
    val requestAll: () -> Unit
) {
    val allEssentialGranted: Boolean get() = mediaGranted
}

fun requiredMediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

fun allRuntimePermissions(): Array<String> {
    val permissions = requiredMediaPermissions().toMutableList()
    permissions += Manifest.permission.RECORD_AUDIO
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions.toTypedArray()
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@Composable
fun rememberAnMediaPermissionsState(): AnMediaPermissionsState {
    val context = LocalContext.current

    var mediaGranted by remember {
        mutableStateOf(requiredMediaPermissions().all { context.hasPermission(it) })
    }
    var recordAudioGranted by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO))
    }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        mediaGranted = requiredMediaPermissions().all { results[it] == true || context.hasPermission(it) }
        recordAudioGranted = results[Manifest.permission.RECORD_AUDIO] == true ||
            context.hasPermission(Manifest.permission.RECORD_AUDIO)
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            results[Manifest.permission.POST_NOTIFICATIONS] == true ||
            context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    return AnMediaPermissionsState(
        mediaGranted = mediaGranted,
        recordAudioGranted = recordAudioGranted,
        notificationsGranted = notificationsGranted,
        requestAll = { launcher.launch(allRuntimePermissions()) }
    )
}
