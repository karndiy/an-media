package com.anmedia.player.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anmedia.player.di.ViewModelFactory
import com.anmedia.player.ui.screens.equalizer.EqualizerScreen
import com.anmedia.player.ui.screens.equalizer.EqualizerViewModel
import com.anmedia.player.ui.screens.library.LibraryScreen
import com.anmedia.player.ui.screens.library.LibraryViewModel
import com.anmedia.player.ui.screens.player.PlayerScreen
import com.anmedia.player.ui.screens.player.PlayerViewModel
import com.anmedia.player.ui.screens.video.VideoPlayerScreen

@Composable
fun AnMediaNavHost(
    viewModelFactory: ViewModelFactory,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var isVideoFullscreen by remember { mutableStateOf(false) }

    // Leaving the Video tab should always drop fullscreen, so the bottom nav and system
    // bars come back even if the user navigated away without using the exit button.
    LaunchedEffect(currentRoute) {
        if (currentRoute != Destination.Video.route) isVideoFullscreen = false
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (!isVideoFullscreen) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    anMediaDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Player.route,
            modifier = Modifier.padding(if (isVideoFullscreen) PaddingValues(0.dp) else innerPadding)
        ) {
            composable(Destination.Player.route) {
                PlayerScreen(
                    viewModel = playerViewModel,
                    onNavigateToLibrary = {
                        navController.navigate(Destination.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Destination.Equalizer.route) {
                val vm: EqualizerViewModel = viewModel(factory = viewModelFactory)
                EqualizerScreen(viewModel = vm)
            }
            composable(Destination.Library.route) {
                val vm: LibraryViewModel = viewModel(factory = viewModelFactory)
                LibraryScreen(viewModel = vm)
            }
            composable(Destination.Video.route) {
                VideoPlayerScreen(
                    viewModel = playerViewModel,
                    isFullscreen = isVideoFullscreen,
                    onFullscreenChange = { isVideoFullscreen = it }
                )
            }
        }
    }
}
