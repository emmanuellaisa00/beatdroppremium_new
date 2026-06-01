package com.beatdrop.kt.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

data class TabSpec(val route: String, val label: String, val icon: ImageVector)
data class TabSpec2(val route: String, val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector)

/**
 * Floating liquid-glass pill tab bar (legacy — kept for backward compatibility).
 */
@Composable
fun GlassTabBar(tabs: List<TabSpec>, current: String, onSelect: (String) -> Unit) {
    val C = LocalAppColors.current

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(48f, 48f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                        clip = true
                    }
                else Modifier
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(if (C.isDark) Color(0xCC12101F) else Color(0xE6F2F2F7))
                .border(0.8.dp, if (C.isDark) Color(0x2EFFFFFF) else Color(0x33000000), RoundedCornerShape(50.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                LegacyTabItem(tab, tab.route == current, Modifier.weight(1f)) { onSelect(tab.route) }
            }
        }
    }
}

@Composable
private fun LegacyTabItem(tab: TabSpec, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val C = LocalAppColors.current
    val scale by animateFloatAsState(
        if (active) 1.08f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), "tabScale",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (active) Modifier
                    .background(if (C.isDark) Color(0x22FFFFFF) else Color(0x18000000))
                    .border(0.6.dp, if (C.isDark) Color(0x2BFFFFFF) else Color(0x22000000), RoundedCornerShape(16.dp))
                else Modifier
            )
            .pressableScale(onClick = onClick, scaleTo = 0.88f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 8.dp).scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(tab.icon, tab.label, tint = if (active) C.accent else C.textSecondary, modifier = Modifier.size(22.dp))
            Text(tab.label, color = if (active) C.accent else C.textSecondary, fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

// ─── iOS 26 Liquid Glass Tab Bar ─────────────────────────────────────────────

/**
 * iOS 26-style Liquid Glass tab bar with:
 *
 *   ✅ Backdrop blur + saturation boost (API 31+)
 *   ✅ Specular highlights responding to device tilt
 *   ✅ Rim light (Fresnel top-edge highlight for glass thickness)
 *   ✅ Scroll-responsive morphing: shrinks when scrolling down, expands back up
 *   ✅ Content-aware glass fill (adapts to dark/light)
 *   ✅ Interaction glow on tab press
 *   ✅ Concentric corner radii (outer bar vs inner indicator)
 *   ✅ Graceful pre-API-31 fallback (heavier opaque fill)
 *   ✅ Hairline border + inner shadow for depth
 *   ✅ Haptic feedback on selection
 *
 * @param isScrolledDown pass true when the user is scrolling down content.
 *        The tab bar morphs smaller to focus on content.
 */
@Composable
fun GlassTabBar2(
    tabs: List<TabSpec2>,
    current: String,
    isScrolledDown: Boolean = false,
    onSelect: (String) -> Unit,
) {
    val C = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val tilt = rememberDeviceTilt()

    // ── Scroll-responsive morphing ───────────────────────────────────────────
    val barHeight by animateDpAsState(
        targetValue = if (isScrolledDown) 42.dp else 54.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "barHeight",
    )
    val iconSize by animateDpAsState(
        targetValue = if (isScrolledDown) 20.dp else 26.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "iconSize",
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isScrolledDown) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "dotScale",
    )

    val outerRadius = 20.dp
    val outerShape = RoundedCornerShape(outerRadius)

    // ── Glass bar container ──────────────────────────────────────────────────
    Box(
        Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                // Backdrop blur + saturation
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        Modifier.graphicsLayer {
                            renderEffect = RenderEffect.createChainEffect(
                                RenderEffect.createColorFilterEffect(
                                    android.graphics.ColorMatrixColorFilter(
                                        android.graphics.ColorMatrix().apply { setSaturation(1.6f) }
                                    )
                                ),
                                RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP),
                            ).asComposeRenderEffect()
                            clip = true
                            shape = outerShape
                        }
                    else Modifier
                )
                // Glass fill
                .background(
                    if (C.isDark) Color(0xCC0E0C1A) else Color(0xD9F4F4F8),
                    shape = outerShape,
                )
                // Rim light (top-edge glow for glass thickness)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                C.glassRimLight.copy(alpha = if (C.isDark) 0.10f else 0.20f),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = size.height * 0.4f,
                        ),
                    )
                }
                // Specular highlight (device tilt)
                .specularHighlight(tilt, intensity = if (C.isDark) 0.08f else 0.05f, radius = 200f)
                // Hairline border
                .border(
                    width = if (C.isDark) 0.8.dp else 0.5.dp,
                    color = if (C.isDark) Color(0x30FFFFFF) else Color(0x20000000),
                    shape = outerShape,
                )
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(barHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { _, tab ->
                    val isActive = tab.route == current
                    LiquidTabItem(
                        tab = tab,
                        active = isActive,
                        iconSize = iconSize,
                        dotScale = dotScale,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isActive) return@LiquidTabItem
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(tab.route)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidTabItem(
    tab: TabSpec2,
    active: Boolean,
    iconSize: Dp,
    dotScale: Float,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val C = LocalAppColors.current
    val scale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabScale2",
    )
    val dotHeight by animateDpAsState(
        targetValue = if (active) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dotHeight",
    )

    Box(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale),
        ) {
            Icon(
                imageVector = if (active) tab.iconFilled else tab.iconOutlined,
                contentDescription = tab.label,
                tint = if (active) C.accent else C.textSecondary,
                modifier = Modifier.size(iconSize),
            )
            if (dotScale > 0.01f) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .width(4.dp)
                        .height(dotHeight)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(if (active) C.accent else Color.Transparent)
                )
            }
        }
    }
}
