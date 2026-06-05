package com.beatdrop.kt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Type

/**
 * TermsSheet — first-time Discover/Search privacy + terms acceptance prompt.
 *
 * Rendered as a tap-to-dismiss-disabled ModalBottomSheet that occupies
 * the bottom half of the screen. Top: title + 3-bullet summary +
 * "Read full policy" toggle that expands the rest of the document
 * inline. Bottom: green Accept pill (records currentVersionCode into
 * Prefs.termsAcceptedVersion).
 *
 * The full policy text lives in /PRIVACY_POLICY.md at the repo root.
 * The body shown here is a hand-written summary so users don't have to
 * download an external file — it covers the same lawful bases (GDPR,
 * UK GDPR, CCPA/CPRA, Kenya DPA).
 *
 * Behaviour rules (orchestrated by the caller, not this composable):
 *   • Sheet shows the first time the user opens Discover or Search after
 *     install OR after a major-version bump that changes the policy.
 *   • Dismissal is *not* a no-op — the user must tap Accept. There is
 *     no back-press / scrim-tap dismiss.
 *   • Accept → vm.acceptTerms(currentVersionCode) → sheet closes and
 *     never re-appears for this version.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsSheet(
    onAccept: () -> Unit,
) {
    val C = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expanded by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    ModalBottomSheet(
        // No dismiss handler — user must Accept (or kill app). Tap-on-
        // scrim and back-press are both swallowed because onDismissRequest
        // is a no-op.
        onDismissRequest = { /* no-op: must Accept */ },
        sheetState = sheetState,
        containerColor = C.bg1,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(horizontal = 22.dp)
                .padding(bottom = 18.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Ic.Shield,
                        contentDescription = null,
                        tint = C.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Before you continue",
                        style = Type.title2,
                        color = C.text,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Privacy & Terms · 30-second read",
                        style = Type.footnote,
                        color = C.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Scrollable body ─────────────────────────────────────────
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scroll),
            ) {
                BulletParagraph(
                    title = "Everything stays on your device",
                    body  = "Your library, downloads, playlists, search history, " +
                            "and preferences are stored locally. We don't operate a " +
                            "backend that mirrors any of it.",
                )
                BulletParagraph(
                    title = "Search & streaming go directly to YouTube",
                    body  = "When you search or play a track, your device contacts " +
                            "YouTube / YouTube Music (a Google service) directly. " +
                            "Standard browser-style headers apply.",
                )
                BulletParagraph(
                    title = "No tracking, no ads, no account",
                    body  = "BeatDrop has no analytics SDK, no advertising ID use, " +
                            "no login. There's no Laisacorp server we route through.",
                )

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        SectionHeading("Data handled locally")
                        ParagraphText(
                            "Library index, playback state, liked songs, playlists, " +
                            "play counts, search history (last few queries), downloaded " +
                            "audio files, .lrc lyrics cache, theme + crossfade settings, " +
                            "private-folder PIN. All under Android/data/com.beatdrop.kt/.",
                        )
                        SectionHeading("Data sent off-device")
                        ParagraphText(
                            "HTTPS requests to youtube.com / youtubei.googleapis.com / " +
                            "music.youtube.com (search + stream resolution), " +
                            "googlevideo.com (audio bytes), lrclib.net (synced lyrics), " +
                            "and — only if you've configured one in Settings — your own " +
                            "self-hosted resolver backend.",
                        )
                        SectionHeading("Your rights")
                        ParagraphText(
                            "Under GDPR / UK GDPR / CCPA / Kenya DPA: erase everything " +
                            "by uninstalling or Settings → Apps → BeatDrop → Clear Data. " +
                            "All your data lives on the device — there's nothing on our " +
                            "side to request or delete.",
                        )
                        SectionHeading("Permissions we ask for")
                        ParagraphText(
                            "READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE (scan your library), " +
                            "READ_MEDIA_IMAGES (album art), INTERNET (search + stream), " +
                            "FOREGROUND_SERVICE_MEDIA_PLAYBACK (background audio), " +
                            "POST_NOTIFICATIONS on Android 13+ (playback notification), " +
                            "and on sideload builds MANAGE_EXTERNAL_STORAGE (sidecar .lrc " +
                            "+ user-chosen download folders).",
                        )
                        SectionHeading("YouTube relationship")
                        ParagraphText(
                            "BeatDrop is not affiliated with, endorsed by, or sponsored " +
                            "by Google or YouTube. Your use of streamed content is also " +
                            "subject to Google's / YouTube's Terms of Service and local " +
                            "copyright law.",
                        )
                        SectionHeading("Contact")
                        ParagraphText(
                            "Publisher: Laisacorp. Open an issue at " +
                            "github.com/emmanuellaisa00/beatdroppremium with the title " +
                            "\"GDPR / DPA request\" for any data-protection enquiry.",
                        )
                    }
                }
            }

            // ── Read-more toggle ────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .pressableScale(onClick = { expanded = !expanded }, scaleTo = 0.97f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    if (expanded) "Show less ↑" else "Read full policy ↓",
                    color = C.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))

            // ── Accept CTA ──────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(C.accent)
                    .pressableScale(onClick = onAccept, scaleTo = 0.96f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Accept & Continue",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BulletParagraph(title: String, body: String) {
    val C = LocalAppColors.current
    Row(
        Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(C.accent),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = C.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                body,
                color = C.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    val C = LocalAppColors.current
    Text(
        text,
        color = C.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun ParagraphText(text: String) {
    val C = LocalAppColors.current
    Text(
        text,
        color = C.textSecondary,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}
