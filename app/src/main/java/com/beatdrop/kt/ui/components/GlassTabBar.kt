package com.beatdrop.kt.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius

data class TabSpec(val route: String, val label: String, val icon: ImageVector)

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
