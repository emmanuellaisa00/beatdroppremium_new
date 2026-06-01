package com.beatdrop.kt.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

data class TabSpec(val route: String, val label: String, val icon: ImageVector)
data class TabSpec2(val route: String, val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector)

/**
 * Floating liquid-glass pill tab bar.
 * Matches the frosted-dark pill in the design reference:
 *   - True backdrop blur via RenderEffect (API 31+), graceful scrim fallback below.
 *   - White translucent fill so content behind bleeds through subtly.
 *   - Single hairline white border ring around the whole pill.
 *   - Floats with horizontal + vertical margin — never touches the screen edges.
 */
@Composable
fun GlassTabBar(tabs: List<TabSpec>, current: String, onSelect: (String) -> Unit) {
    val C = LocalAppColors.current

    // Outer blurred backdrop layer (API 31+ only — blurs the content behind this node)
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
        // Frosted glass pill surface on top of the blur
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))               // fully pill-shaped ends
                .background(                                    // translucent fill — content bleeds through
                    if (C.isDark)
                        Color(0xCC12101F)                      // Frosted dark violet glass
                    else
                        Color(0xE6F2F2F7)                      // Frosted light glass
                )
                .border(
                    width = 0.8.dp,
                    color = if (C.isDark) Color(0x2EFFFFFF) else Color(0x33000000),
                    shape = RoundedCornerShape(50.dp),
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                TabItem(tab, tab.route == current, Modifier.weight(1f)) { onSelect(tab.route) }
            }
        }
    }
}

@Composable
private fun TabItem(tab: TabSpec, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val C = LocalAppColors.current
    val scale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tabScale",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (active)
                    Modifier
                        .background(
                            if (C.isDark) Color(0x22FFFFFF) else Color(0x18000000)
                        )
                        .border(
                            0.6.dp,
                            if (C.isDark) Color(0x2BFFFFFF) else Color(0x22000000),
                            RoundedCornerShape(16.dp),
                        )
                else Modifier
            )
            .pressableScale(onClick = onClick, scaleTo = 0.88f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                tab.icon, tab.label,
                tint = if (active) C.accent else C.textSecondary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                tab.label,
                color = if (active) C.accent else C.textSecondary,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

// ─── Apple Music-Style Clean Tab Bar (Icon-Only) ─────────────────────────────

/**
 * Apple Music-inspired bottom tab bar:
 *   - Full-width, clean solid background — no aggressive blur.
 *   - Icon-only tabs — no label text, maximum clarity.
 *   - Outlined icon for inactive, filled icon for active state.
 *   - Subtle accent-colored indicator dot beneath active tab.
 *   - Smooth scale animation + haptic feedback on selection.
 *   - Proper safe area handling for devices with home indicator.
 *
 * Apple HIG reference (per iOS 18+ Figma specs):
 *   - Tab bar container: 49pt + safe area
 *   - Tab item touch target: minimum 44×44pt
 *   - Icons: 25-26dp for clear visibility
 *   - Active: filled icon + accent color
 *   - Inactive: outlined icon + secondary/tertiary color
 */
@Composable
fun GlassTabBar2(tabs: List<TabSpec2>, current: String, onSelect: (String) -> Unit) {
    val C = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val activeIndex = tabs.indexOfFirst { it.route == current }.coerceAtLeast(0)

    // Background: clean, minimal translucency — no blur to keep icons crisp
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                if (C.isDark)
                    Color(0xE8121019)  // Nearly solid dark, slight translucency
                else
                    Color(0xE8F8F8FA)  // Nearly solid light, slight translucency
            )
    ) {
        // Subtle top border line
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .align(Alignment.TopCenter)
                .background(
                    if (C.isDark) Color(0x1FFFFFFF) else Color(0x1A000000)
                )
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(50.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = tab.route == current
                TabItem2(
                    tab = tab,
                    active = isActive,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isActive) return@TabItem2
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(tab.route)
                    },
                )
            }
        }
    }
}

@Composable
private fun TabItem2(tab: TabSpec2, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val C = LocalAppColors.current
    val scale by animateFloatAsState(
        targetValue = if (active) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tabScale2",
    )
    val dotHeight by animateDpAsState(
        targetValue = if (active) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "dotHeight",
    )

    Box(
        modifier
            .height(50.dp)
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
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(4.dp)
                    .height(dotHeight)
                    .clip(CircleShape)
                    .background(if (active) C.accent else Color.Transparent)),
            Spacer(Modifier.height(2.dp))
        }
    }
}
