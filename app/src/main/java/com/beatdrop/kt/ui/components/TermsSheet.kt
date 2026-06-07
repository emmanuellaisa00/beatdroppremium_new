package com.beatdrop.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.*

/**
 * Privacy / Terms acceptance sheet.
 * Legal gate shown on first launch before user can proceed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsSheet(
    visible: Boolean,
    onAccept: () -> Unit,
) {
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = { /* non-dismissible */ },
            containerColor = GlassBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        .width(36.dp).height(4.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                )
            },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Shield icon
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Accent.copy(alpha = 0.25f)),
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🛡️", fontSize = androidx.compose.ui.unit.sp(28.sp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Before you continue",
                    style = MaterialTheme.typography.headlineLarge,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    buildAnnotatedString {
                        append("By using BeatDrop, you agree to our ")
                        withStyle(SpanStyle(color = Accent, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
                            append("Terms of Service")
                        }
                        append(" and acknowledge our ")
                        withStyle(SpanStyle(color = Accent, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
                            append("Privacy Policy")
                        }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMedium),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "We don't collect personal data. Music stays on your device unless you stream from the catalogue.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextLow),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(Modifier.height(28.dp))

                // Accept button
                Surface(
                    onClick = onAccept,
                    shape = RoundedCornerShape(16.dp),
                    color = Accent,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "I agree",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Decline
                TextButton(onClick = { /* exit app */ }) {
                    Text(
                        "No thanks",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextLow,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
}
