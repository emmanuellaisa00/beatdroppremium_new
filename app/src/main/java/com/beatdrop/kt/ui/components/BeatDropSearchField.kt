package com.beatdrop.kt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Type

/**
 * BeatDropSearchField — matches the HTML concept:
 *   • Pill (radius 28), 52–56 dp tall
 *   • Dark obsidian glass background, hairline border
 *   • Search icon 60% white opacity, placeholder 45% white
 *   • Apple Music pink (#FA2D48) caret + focus border
 */
@Composable
fun BeatDropSearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    submitting: Boolean = false,
    showClear: Boolean = true,
    radius: Dp = 28.dp,
) {
    val C = LocalAppColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) C.accent.copy(alpha = 0.85f) else Color.White.copy(alpha = if (C.isDark) 0.08f else 0.18f),
        animationSpec = tween(220),
        label = "border",
    )
    val borderWidth = if (isFocused) 1.2.dp else 0.6.dp

    val pulseTransition = rememberInfiniteTransition(label = "submit-pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    val iconBase = if (isFocused || value.isNotEmpty()) C.accent else C.text.copy(alpha = 0.60f)
    val iconColor = if (submitting) iconBase.copy(alpha = pulseAlpha) else iconBase

    val glowElevation by animateFloatAsState(
        targetValue = if (isFocused || submitting) 12f else 0f,
        animationSpec = tween(240),
        label = "glow",
    )

    val shape = RoundedCornerShape(radius)

    val selectionColors = TextSelectionColors(
        handleColor = C.accent,
        backgroundColor = C.accent.copy(alpha = 0.30f),
    )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = glowElevation.dp,
                    shape = shape,
                    ambientColor = C.accent,
                    spotColor = C.accent,
                )
                .premiumGlass(level = GlassLevel.Z2_Card, shape = shape)
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Ic.Search,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(14.dp))

            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = placeholder,
                        style = Type.body,
                        color = C.text.copy(alpha = 0.45f),
                        fontWeight = FontWeight.Medium,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = Type.body.copy(
                        color = C.text,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(C.accent),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (onSubmit != null) ImeAction.Search else ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSubmit?.invoke()
                            keyboard?.hide()
                        },
                        onDone = { keyboard?.hide() },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (showClear && value.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .pressableScale(onClick = { onChange("") }, scaleTo = 0.85f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Ic.Close,
                        contentDescription = "Clear",
                        tint = C.text.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            if (onSubmit != null && value.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(C.accent)
                        .pressableScale(onClick = {
                            onSubmit()
                            keyboard?.hide()
                        }, scaleTo = 0.90f)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text(
                        "Search",
                        style = Type.caption,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Round 40 dp icon button — header shortcut to full Search.
 * Matches the HTML .icon-circle (dark glass, monochrome icon).
 */
@Composable
fun BeatDropSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Search",
) {
    val C = LocalAppColors.current
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(40.dp)
            .premiumGlass(level = GlassLevel.Z2_Card, shape = shape)
            .pressableScale(onClick = onClick, scaleTo = 0.86f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Ic.Search,
            contentDescription = contentDescription,
            tint = C.text.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
        )
    }
}
