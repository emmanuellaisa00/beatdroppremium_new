package com.beatdrop.kt.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Global on/off switch for in-app haptic feedback. Defaults to TRUE so the
 * first-paint feel is Spotify-snappy out of the box; MainScaffold provides
 * the real value driven by Prefs.hapticsFlow so users can disable it in
 * Settings → Haptics.
 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * Discrete haptic "kinds". Maps to the closest HapticFeedbackConstants
 * value Android can fire. Using a richer enum than raw constants so
 * call-sites read like 'why am I haptic-ing' rather than 'what number'.
 */
enum class HapticKind {
    /** Generic tap on any pressable. Default. */
    Tap,
    /** Play / pause / shuffle / repeat / mute — state-changing transport. */
    Confirm,
    /** Tab change, drag-handle reorder, gesture dismiss. */
    Tick,
    /** Destructive: delete, clear-all, cancel-download. */
    Reject,
    /** Long-press menu open. */
    LongPress,
}

/**
 * Fire a one-shot haptic from anywhere with access to a View, honouring
 * the LocalHapticsEnabled flag. Cheap — silently no-ops if disabled.
 */
fun View.haptic(kind: HapticKind, enabled: Boolean) {
    if (!enabled) return
    val const = when (kind) {
        HapticKind.Tap       -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticKind.Confirm   -> if (Build.VERSION.SDK_INT >= 30)
            HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        HapticKind.Tick      -> if (Build.VERSION.SDK_INT >= 27)
            HapticFeedbackConstants.TEXT_HANDLE_MOVE else HapticFeedbackConstants.VIRTUAL_KEY
        HapticKind.Reject    -> if (Build.VERSION.SDK_INT >= 30)
            HapticFeedbackConstants.REJECT else HapticFeedbackConstants.VIRTUAL_KEY
        HapticKind.LongPress -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(const)
}

/**
 * Compose-side convenience: returns a lambda you can stash in
 * `remember(haptics)` and call without re-resolving locals every press.
 */
@Composable
@ReadOnlyComposable
fun rememberHaptic(kind: HapticKind = HapticKind.Tap): () -> Unit {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return { view.haptic(kind, enabled) }
}
