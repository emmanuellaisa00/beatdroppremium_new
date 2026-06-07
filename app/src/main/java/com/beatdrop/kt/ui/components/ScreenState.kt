package com.beatdrop.kt.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.beatdrop.kt.data.BeatDropError
import com.beatdrop.kt.data.BeatDropResult
import com.beatdrop.kt.data.safeCall

/**
 * Screen state sealed class — every screen uses exactly one of these.
 */
sealed class ScreenState<out T> {
    data object Loading : ScreenState<Nothing>()
    data class Success<T>(val data: T) : ScreenState<T>()
    data class Error(val error: BeatDropError) : ScreenState<Nothing>()
}

/**
 * Remember a screen state from a suspending data load.
 * Automatically reloads when `key` changes.
 *
 * Usage:
 *   val state by rememberScreenState(albumId) { loadAlbum(it) }
 *   when (state) {
 *       is ScreenState.Loading -> LoadingState()
 *       is ScreenState.Success -> AlbumContent(state.data)
 *       is ScreenState.Error -> ErrorScreen(state.error, onRetry = ...)
 *   }
 */
@Composable
fun <T, K> rememberScreenState(
    key: K,
    loader: suspend (K) -> T,
): ScreenState<T> {
    var state by remember { mutableStateOf<ScreenState<T>>(ScreenState.Loading) }
    LaunchedEffect(key) {
        state = when (val result = safeCall { loader(key) }) {
            is BeatDropResult.Success -> ScreenState.Success(result.data)
            is BeatDropResult.Error -> ScreenState.Error(result.error)
        }
    }
    return state
}

/**
 * Simple rememberScreenState without a key parameter.
 */
@Composable
fun <T> rememberScreenState(
    loader: suspend () -> T,
): ScreenState<T> {
    var state by remember { mutableStateOf<ScreenState<T>>(ScreenState.Loading) }
    LaunchedEffect(Unit) {
        state = when (val result = safeCall { loader() }) {
            is BeatDropResult.Success -> ScreenState.Success(result.data)
            is BeatDropResult.Error -> ScreenState.Error(result.error)
        }
    }
    return state
}

/**
 * Wrapper that handles the loading/error/content pattern.
 * Wraps any screen composable with proper state handling.
 */
@Composable
fun <T> ScreenContentWrapper(
    state: ScreenState<T>,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    loadingMessage: String = "Loading…",
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is ScreenState.Loading -> LoadingState(message = loadingMessage)
        is ScreenState.Success -> content(state.data)
        is ScreenState.Error -> ErrorScreen(
            error = state.error,
            onRetry = onRetry,
            onDismiss = onDismiss,
        )
    }
}
