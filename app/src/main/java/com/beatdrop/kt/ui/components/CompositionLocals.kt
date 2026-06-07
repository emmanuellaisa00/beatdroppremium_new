/*
 * Composition locals used by MainActivity for Haze blur, device tilt, and haptics.
 */

package com.beatdrop.kt.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset

// Haze blur state — provided by Root composable
val LocalHazeState = staticCompositionLocalOf<Any?> { null }

// Device tilt from accelerometer
val LocalDeviceTilt = staticCompositionLocalOf<State<Offset>?> { null }

// Haptics toggle
val LocalHapticsEnabled = staticCompositionLocalOf { true }

@Composable
fun rememberDeviceTilt(): State<Offset> {
    // Stub — tilt won't work without sensor registration
    return remember { mutableStateOf(Offset.Zero) }
}
