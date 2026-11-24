package com.anmedia.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    data object Player : Destination("player", "Player", Icons.Filled.PlayCircle)
    data object Equalizer : Destination("equalizer", "EQ", Icons.Filled.GraphicEq)
    data object Library : Destination("library", "Library", Icons.Filled.LibraryMusic)
    data object Video : Destination("video", "Video", Icons.Filled.Videocam)
}

val anMediaDestinations = listOf(Destination.Player, Destination.Equalizer, Destination.Library, Destination.Video)
