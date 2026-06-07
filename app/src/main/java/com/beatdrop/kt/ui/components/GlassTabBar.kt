package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors

data class TabSpec2(val route: String, val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector)

/**
 * BeatDrop Bottom Dock — matches the HTML concept:
 *   • Floating pill, 90% screen width, 72 dp tall, radius 36
 *   • Dark obsidian glass (rgba(22,24,30, 0.62))
 *   • Active tab = inset glass orb (recessed look, NOT a bright color)
 *   • Monochrome icons, no labels
 *   • Spring-physics motion when switching tabs
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

    val outerShape = RoundedCornerShape(36.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .premiumGlass(level = GlassLevel.Z4_TabBar, shape = outerShape),
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                val activeIdx = tabs.indexOfFirst { it.route == current }.coerceAtLeast(0)
                val barWidthPx = with(LocalDensity.current) { this@BoxWithConstraints.maxWidth.toPx() }
                val itemWidthPx = barWidthPx / tabs.size
                val puckSizePx = with(LocalDensity.current) { 44.dp.toPx() }
                val targetXpx = (activeIdx * itemWidthPx) + (itemWidthPx / 2f) - (puckSizePx / 2f)
                val animatedX by animateFloatAsState(
                    targetValue = targetXpx,
                    animationSpec = spring(stiffness = 400f, dampingRatio = 0.75f),
                    label = "puckX",
                )

                // Inset active orb — recessed look (not a bright fill)
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset(animatedX.toInt(), 0) }
                        .size(44.dp)
                        .premiumGlass(level = GlassLevel.Z5_ActiveLens, shape = CircleShape),
                )

                // Tab items — icon only, monochrome
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        val isActive = tab.route == current
                        DockItem(
                            tab = tab,
                            active = isActive,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isActive) return@DockItem
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab.route)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    tab: TabSpec2,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val C = LocalAppColors.current

    Box(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (active) tab.iconFilled else tab.iconOutlined,
            contentDescription = tab.label,
            tint = if (active) C.text else C.text.copy(alpha = 0.50f),
            modifier = Modifier.size(22.dp),
        )
    }
}
