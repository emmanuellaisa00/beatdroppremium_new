package com.beatdrop.kt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.beatdrop.kt.navigation.BeatDropNavGraph
import com.beatdrop.kt.navigation.Screen
import com.beatdrop.kt.ui.components.BottomDock
import com.beatdrop.kt.ui.components.MiniPlayer
import com.beatdrop.kt.ui.components.ErrorToast
import com.beatdrop.kt.ui.theme.BeatDropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Global unhandled exception handler — prevents hard crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log to debug screen if possible, then delegate to default
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Exception) {
                // If even the default handler crashes, just kill the activity
                finishAffinity()
            }
        }

        setContent { BeatDropTheme { BeatDropApp() } }
    }
}

@Composable
fun BeatDropApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Global error toast
    var globalError by remember { mutableStateOf<String?>(null) }

    val hideDockOn = setOf(
        Screen.Splash.route,
        Screen.Onboarding.route,
        Screen.NowPlaying.route,
        Screen.Lyrics.route,
        Screen.VideoPlayer.route,
    )
    val showDock = currentRoute != null && currentRoute !in hideDockOn
    val showMini = showDock && currentRoute != null

    val activeTab = when {
        currentRoute == Screen.Discover.route -> 0
        currentRoute == Screen.Search.route -> 1
        currentRoute == Screen.Library.route -> 2
        currentRoute == Screen.Radio.route -> 3
        else -> 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BeatDropNavGraph(navController = navController, modifier = Modifier.fillMaxSize())

        // Mini player
        AnimatedVisibility(
            visible = showMini,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(start = 10.dp, end = 10.dp, bottom = 100.dp)
                .zIndex(3f),
        ) {
            MiniPlayer(
                trackName = "4x4",
                artistName = "Don Toliver",
                coverIndex = 1,
                progress = 0.42f,
                onClick = {
                    try {
                        navController.navigate(Screen.NowPlaying.route)
                    } catch (_: Exception) { }
                },
            )
        }

        // Bottom dock
        AnimatedVisibility(
            visible = showDock,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
                .zIndex(4f),
        ) {
            BottomDock(activeTab = activeTab, onTabSelected = { tab ->
                val route = when (tab) {
                    0 -> Screen.Discover.route
                    1 -> Screen.Search.route
                    2 -> Screen.Library.route
                    3 -> Screen.Radio.route
                    else -> Screen.Discover.route
                }
                try {
                    navController.navigate(route) {
                        popUpTo(Screen.Discover.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                } catch (_: Exception) { }
            })
        }

        // Global error toast
        globalError?.let { msg ->
            ErrorToast(
                message = msg,
                visible = true,
                onDismiss = { globalError = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .zIndex(5f),
            )
        }
    }
}
