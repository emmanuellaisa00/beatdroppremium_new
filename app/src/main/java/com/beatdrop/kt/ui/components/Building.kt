package com.beatdrop.kt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Type

/** Section header: bold title + optional trailing action, used across screens. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val C = LocalAppColors.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Type.title2, color = C.text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action, style = Type.callout, color = C.accent,
                modifier = Modifier.pressableScale(onClick = onAction, scaleTo = 0.92f, haptic = false),
            )
        }
    }
}

/** Small uppercase label above a group (Apple "BROWSE", Spotify section eyebrow). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    val C = LocalAppColors.current
    Text(text, style = Type.overline, color = C.textTertiary,
        modifier = modifier.padding(horizontal = Spacing.lg, vertical = 4.dp))
}

/** Shimmer placeholder block for loading states (premium perceived speed). */
@Composable
fun Shimmer(modifier: Modifier = Modifier, corner: Int = 12) {
    val C = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmerX",
    )
    val base = if (C.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    val hi = if (C.isDark) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.12f)
    Box(
        modifier.clip(RoundedCornerShape(corner.dp)).background(
            Brush.linearGradient(
                colors = listOf(base, hi, base),
                start = androidx.compose.ui.geometry.Offset(x * 200f, 0f),
                end = androidx.compose.ui.geometry.Offset((x + 1f) * 200f, 200f),
            )
        )
    )
}

/** Vertical gradient scrim — for legible text over artwork (Apple/Spotify hero). */
@Composable
fun GradientScrim(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.85f)))))
}

/** Shared Liquid Glass card — translucent fill + rim light + hairline border. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val C = LocalAppColors.current
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(shape)
            .background(if (C.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.55f))
            .drawWithContent {
                drawContent()
                drawRect(brush = Brush.verticalGradient(
                    listOf(if (C.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.15f), Color.Transparent),
                    startY = 0f, endY = size.height * 0.3f))
            }
            .border(0.8.dp, C.liquidGlassBorder, shape)
            .padding(16.dp),
        content = content,
    )
}
