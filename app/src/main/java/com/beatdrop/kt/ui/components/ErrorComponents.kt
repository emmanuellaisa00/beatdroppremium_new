package com.beatdrop.kt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.data.BeatDropError
import com.beatdrop.kt.data.icon
import com.beatdrop.kt.data.retryable
import com.beatdrop.kt.ui.theme.*
import androidx.compose.foundation.BorderStroke

/**
 * Full-screen error state — replaces screen content when data fails to load.
 * Shows icon, message, optional details, and retry button.
 */
@Composable
fun ErrorScreen(
    error: BeatDropError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            // Error icon
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Accent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Accent.copy(alpha = 0.20f)),
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(error.icon, fontSize = 32.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Error message
            Text(
                error.message,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                ),
                color = Color.White,
            )

            // Detail text
            val detail = errorDetail(error)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMedium,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            // Retry button
            if (error.retryable && onRetry != null) {
                Surface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(16.dp),
                    color = Accent,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Try again",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }

            // Dismiss / go back
            if (onDismiss != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        "Go back",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMedium,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Inline error banner — shows at top of a content area.
 * Used for non-fatal errors (e.g., one track failed to download).
 */
@Composable
fun ErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF2A1015),
        border = BorderStroke(1.dp, Color(0xFFFF375F).copy(alpha = 0.30f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                Icons.Filled.Warning, null,
                tint = Color(0xFFFF375F),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                ),
                modifier = Modifier.weight(1f),
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(
                        "Retry",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, null, tint = TextMedium, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Toast-style snackbar — shows briefly at bottom of screen.
 */
@Composable
fun ErrorToast(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xDD1A1A22),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Filled.ErrorOutline, null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(
                            actionLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Accent,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }

    // Auto-dismiss after 4s
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(4000)
            onDismiss()
        }
    }
}

/**
 * Loading state with optional message.
 */
@Composable
fun LoadingState(
    message: String = "Loading…",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Accent,
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextMedium,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

// ── Helpers ──

private fun errorDetail(error: BeatDropError): String? = when (error) {
    is BeatDropError.NetworkUnavailable -> "Check your connection and try again"
    is BeatDropError.NetworkTimeout -> "The server took too long to respond"
    is BeatDropError.HttpError -> when (error.code) {
        401 -> "Authentication required"
        403 -> "Access denied"
        404 -> "The requested content was not found"
        429 -> "Too many requests — slow down"
        in 500..599 -> "Server is having issues — try again later"
        else -> "HTTP ${error.code}"
    }
    is BeatDropError.PlaybackFailed -> "This track may be corrupted or unavailable"
    is BeatDropError.DownloadFailed -> "Check your connection and available storage"
    is BeatDropError.FileNotFound -> "The file may have been moved or deleted"
    is BeatDropError.StorageFull -> "Free up space in Settings → Storage"
    is BeatDropError.CodecError -> "This format is not supported by your device"
    is BeatDropError.DataLoadFailed -> "We couldn't load your data right now"
    is BeatDropError.DatabaseError -> "There was a problem reading local data"
    is BeatDropError.ParseError -> "The data format was unexpected"
    is BeatDropError.InvalidUrl -> "Make sure the link is a valid BeatDrop URL"
    is BeatDropError.LinkUnresolvable -> "This link doesn't match any content on BeatDrop"
    is BeatDropError.TermsRequired -> null
    is BeatDropError.Unknown -> error.cause?.message
}
