package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Spring press-scale + haptic + Liquid Glass interaction glow.
 *
 * When pressed, a radial glow emanates from the center of the element,
 * simulating the Liquid Glass "illuminate from within" feedback.
 * Optional long-press (combinedClickable) powers the Spotify-style
 * track action sheet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressableScale(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    scaleTo: Float = 0.97f,
    /**
     * Whether this pressable fires a tap-haptic. Default TRUE so the app
     * feels Spotify-snappy out of the box. The effective haptic still
     * AND-s with LocalHapticsEnabled (Settings → Haptics), so users who
     * disabled feedback in settings get silence regardless of this flag.
     */
    haptic: Boolean = true,
    enableGlow: Boolean = true,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    val C = LocalAppColors.current
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed && enableGlow) 0.35f else 0f,
        animationSpec = if (pressed) spring(stiffness = 600f) else spring(stiffness = 200f),
        label = "pressGlow",
    )
    val view = LocalView.current
    val hapticsEnabled = com.beatdrop.kt.ui.components.LocalHapticsEnabled.current
    return this
        .scale(scale)
        .then(
            if (enableGlow && glowAlpha > 0.01f) {
                Modifier.drawWithContent {
                    drawContent()
                    // Radial glow from center — Liquid Glass interaction feedback
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                C.glassGlow.copy(alpha = glowAlpha),
                                C.glassGlow.copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.8f,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.8f,
                    )
                }
            } else Modifier
        )
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClick = {
                if (haptic && hapticsEnabled) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
                onClick()
            },
            onLongClick = onLongClick?.let {
                {
                    if (hapticsEnabled) {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                    it()
                }
            },
        )
}
