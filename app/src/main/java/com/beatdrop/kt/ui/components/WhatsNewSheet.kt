package com.beatdrop.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Type

/**
 * Single What's-New highlight row.
 */
private data class WhatsNewItem(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

/**
 * WhatsNewSheet — surfaces the new shuffle / playback / UI behaviour to
 * existing users the first time they open the app after an update.
 *
 * Triggering rules (handled in MainActivity, not here):
 *   • Show iff Prefs.lastSeenWhatsNewFlow.first() < BuildConfig.VERSION_CODE
 *     AND the value is >= 0 (i.e. NOT a fresh install — fresh installs see
 *     Onboarding instead and the version is stored on its completion).
 *   • Dismissal stores BuildConfig.VERSION_CODE so the sheet won't re-appear
 *     until the next version bump.
 *
 * Visual:
 *   • Bottom sheet with the app's theme.
 *   • 4 highlight rows: icon chip + title + body.
 *   • Single 'Got it' CTA at the bottom.
 *   • Zero spinners, zero progress bars, no external network — pure copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    onDismiss: () -> Unit,
) {
    val C = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val items = listOf(
        WhatsNewItem(
            icon = Ic.Sparkles,
            title = "Smarter shuffle",
            body = "Up Next now flows by artist, collabs, genre tags in the title, " +
                   "tempo and era — not by what you liked last month. Likes still " +
                   "live in your Liked Songs playlist; they no longer bias the queue.",
        ),
        WhatsNewItem(
            icon = Ic.Play,
            title = "Never stops",
            body = "When an online song ends, the next one starts in under 200 ms. " +
                   "Predetermined the moment the current track begins, pre-warmed in " +
                   "the playback cache. No gap, no silence.",
        ),
        WhatsNewItem(
            icon = Ic.Search,
            title = "Search that you can read",
            body = "Every search bar is now opaque with high-contrast text, an accent " +
                   "focus ring, and a clear (×) chip. Tap a result and Now Playing " +
                   "opens instantly with the artwork + title already there.",
        ),
        WhatsNewItem(
            icon = Ic.MusicNote,
            title = "No more spinners",
            body = "Download progress is now drawn around the download icon itself. " +
                   "Search loading shows content-shaped silhouettes that crossfade " +
                   "into your real results. Buffer ahead of the playhead glows softly " +
                   "on the seek bar.",
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = C.bg1,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
        ) {
            // ── Title ───────────────────────────────────────────────────
            Text(
                "What's new",
                style = Type.title1,
                color = C.text,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            Text(
                "A few upgrades you'll notice immediately",
                style = Type.subhead,
                color = C.textSecondary,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // ── Highlight rows ──────────────────────────────────────────
            items.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = C.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = Type.headline,
                            color = C.text,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.body,
                            style = Type.footnote,
                            color = C.textSecondary,
                        )
                    }
                }
            }

            // ── CTA ─────────────────────────────────────────────────────
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(C.accent)
                    .pressableScale(onClick = onDismiss, scaleTo = 0.96f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Got it",
                    style = Type.headline,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
