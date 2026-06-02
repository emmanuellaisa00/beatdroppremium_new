# 🔍 SnapTube vs BeatDrop — Exhaustive Feature Gap Analysis

> What SnapTube has for YouTube media playback & download that BeatDrop currently lacks.
> Based on analysis of BeatDrop's full source code vs SnapTube's documented feature set (2025–2026).

---

## 📊 Summary

| Category | SnapTube | BeatDrop | Gap? |
|----------|----------|----------|------|
| Multi-platform downloads | 50–100+ sites | YouTube only | ❌ |
| Video downloads | 144p–4K, MP4/AVI/MOV | Audio only | ❌ |
| Format picker (per-download) | MP4/M4A/MP3, resolution picker | Auto-select, global cap only | ❌ |
| MP3 conversion | On-the-fly transcode | No transcode | ❌ |
| Built-in web browser | Yes, auto-detects videos | Search only | ❌ |
| Clipboard URL detection | Auto-detects pasted links | None | ❌ |
| Share-to-download (Android Share menu) | Yes | No intent filters | ❌ |
| Batch / playlist download | Queue multiple, entire playlists | One-at-a-time | ❌ |
| Download pause & resume | Per-download pause/resume | Cancel/retry only | ❌ |
| Download speed control | Fast mode, speed limiter | No user controls | ❌ |
| WiFi vs mobile data guard | Block downloads on mobile data | None | ❌ |
| PiP floating video player | Yes | No video playback | ❌ |
| WhatsApp status saver | Yes | No | ❌ |
| Content discovery / trending feed | For You, trending, categories | Search-only for online | ❌ |
| Channel subscriptions | Subscribe to channels | No | ❌ |
| Built-in VPN | Yes | No | ❌ |
| Private/hidden folder | Password-protected | No | ❌ |
| Recover deleted downloads | Yes | No | ❌ |
| File sharing (WhatsApp etc.) | One-tap share | No | ❌ |
| SD card storage selection | Yes | App-private dir only | ❌ |
| ID3/metadata tagging in files | Auto-tags downloaded files | In-app only, not in file | ❌ |
| Localization (30+ languages) | Yes | English only | ❌ |
| Dark mode | Yes | ✅ Already has it | ✅ |
| YouTube stream resolution (audio) | Multiple bitrate options | Global quality pref | ⚠️ Partial |
| Download notification progress | Yes | ✅ Has foreground service | ✅ |
| Parallel chunked download | Yes | ✅ 4-chunk parallel | ✅ |
| Auto-resume on 403/expired URL | N/A (native download) | ✅ Re-resolves & resumes | ✅ |

---

## 🔴 CRITICAL GAPS (Major features SnapTube has, BeatDrop doesn't)

### 1. Video Downloads (BeatDrop is audio-only)
**SnapTube:** Downloads full VIDEO in MP4, M4V, MOV, AVI, WMV at 144p through 4K (2160p).
**BeatDrop:** Exclusively audio extraction. `PlaybackService` disables the video track (`setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)`). The download function (`downloadYoutubeTrack`) only picks audio streams or the smallest muxed fallback — the muxed stream is played audio-only.
**Impact:** Users who want music videos, concerts, or any visual content have no path.

### 2. Multi-Platform Downloads (BeatDrop is YouTube-only)
**SnapTube:** 50–100+ platforms: YouTube, Facebook, Instagram, TikTok, Twitter/X, Vimeo, Dailymotion, Vevo, WhatsApp, OK.ru, and more.
**BeatDrop:** Only YouTube via Innertube (`searchYoutube` / `searchYoutubeMusic`), Piped, Invidious, and WebView extractor. `SearchProvider` is a single interface but only `InnertubeSearchProvider` exists.
**Impact:** No access to the ~60% of viral/content music that lives on TikTok, Instagram Reels.

### 3. Format Selection Before Download
**SnapTube:** Shows a dialog before every download with:
- Format: Video (MP4) vs Audio (MP3/M4A)
- Resolution: 144p, 240p, 360p, 480p, 720p, 1080p, 1440p, 4K
- Estimated file size per option

**BeatDrop:** No per-download picker. `QualityPreference` is a global setting (auto/high/medium/low) affecting ALL streams. User cannot choose per-song. No file size estimate.
**Impact:** User has no control over individual download quality or format.

### 4. MP3 Conversion / Transcoding
**SnapTube:** Converts YouTube audio to MP3 on-the-fly during download. Also supports M4A output.
**BeatDrop:** No transcoding at all. Downloads in whatever native format YouTube provides (m4a for AAC, opus for WebM). The file extension is detected from Content-Type/itag, but no conversion happens.
**Impact:** Files may not play on some devices/players that don't support opus. Users expecting MP3 won't get it.

### 5. Built-in Web Browser
**SnapTube:** Full in-app browser to navigate YouTube, Facebook, Instagram, etc. Auto-detects downloadable videos on every page. Shows a floating download button when a video is detected.
**BeatDrop:** No browser. Discovery is search-only (`SearchScreen` → `OutlinedTextField` → `runOnlineSearch`). Users cannot browse channels, playlists, or trending pages.
**Impact:** Discovery is limited to what the search API returns. Cannot access unlisted videos, specific channels, or browse organically.

### 6. Clipboard URL Detection
**SnapTube:** Monitors clipboard for video URLs. When user copies a YouTube/Facebook/etc. link and opens SnapTube, it auto-offers to download that specific video.
**BeatDrop:** No clipboard monitoring. No URL parsing. No way to paste a `youtube.com/watch?v=...` link.
**Impact:** Users who find a song on YouTube (in the YouTube app) cannot paste its URL into BeatDrop — they must search for it by name and hope it's the same result.

### 7. Android Share Menu Integration
**SnapTube:** Registers intent filters so it appears in Android's "Share" sheet. User can share a video from YouTube/Facebook/TikTok directly to SnapTube for instant download.
**BeatDrop:** `AndroidManifest.xml` has no `<intent-filter>` for `ACTION_SEND` or `ACTION_VIEW` with video/audio URLs. The app cannot receive shared content.
**Impact:** Major usability gap — the "share to download" workflow is one of SnapTube's most-used features.

---

## 🟠 SIGNIFICANT GAPS (Important features)

### 8. Batch / Playlist Download
**SnapTube:** Download entire playlists or multiple videos at once. Queue management with sequential or concurrent downloads. Unlimited queue size.
**BeatDrop:** One-at-a-time only. `DownloadManager.enqueue()` processes each `OnlineResult` individually. No way to add a YouTube playlist URL or batch-select search results for download.
**Impact:** Building an offline library is tedious (one song at a time).

### 9. Download Pause & Resume (per-download)
**SnapTube:** Individual pause/resume for each download. Survives network interruptions and app restarts.
**BeatDrop:** Supports cancel (`DownloadManager.cancel()`) and retry (`DownloadManager.retry()`), but no pause/resume. If a download is interrupted, it starts from scratch. The chunked download has no checkpoint/state.
**Impact:** On slow/unstable networks, large downloads may never complete.

### 10. Download Speed Control
**SnapTube:** "Fast Download Mode" toggle, configurable max concurrent downloads (1–5), optional speed limiter.
**BeatDrop:** Uses 4 parallel HTTP Range chunks (`CHUNK_COUNT = 4`) with no user controls. No speed limit option. No configurable concurrency.
**Impact:** Downloads may consume all available bandwidth, interfering with streaming or other apps.

### 11. WiFi vs Mobile Data Guard
**SnapTube:** Setting to block downloads when on mobile data. Shows a warning before downloading on cellular.
**BeatDrop:** No network type detection. Downloads proceed regardless of connection type.
**Impact:** Users on limited data plans may unknowingly burn through their allowance.

### 12. Content Discovery / Trending Feed
**SnapTube:** Home feed with "For You" recommendations, trending videos, daily picks, curated categories (Music, Humor, Love, etc.), new releases.
**BeatDrop:** Online discovery is search-only. The "Discover" tab only works with the local library (Most Played, Recently Added, Random Mix, Radio mixes from local tracks). No online trending, no recommendations from YouTube.
**Impact:** Users must know what they're looking for — no serendipitous discovery of new music.

### 13. Channel / Artist Subscriptions
**SnapTube:** Subscribe to YouTube channels. Get notified of new uploads. Auto-download new content from subscribed channels.
**BeatDrop:** No subscription system. No push notifications for new releases. No way to follow an artist's new uploads.
**Impact:** Users must manually check for new music.

### 14. Floating Video Player (PiP)
**SnapTube:** Picture-in-Picture mode for watching videos in a floating window over other apps.
**BeatDrop:** No video playback at all (audio-only music player). The app has no PiP support.
**Impact:** Users who want to watch music videos while multitasking cannot.

### 15. Private / Hidden Folder
**SnapTube:** Password-protected private folder for sensitive downloaded content.
**BeatDrop:** All downloads go to `getExternalFilesDir(null)/BeatDrop/Downloads/` — visible in file managers. No privacy protection.
**Impact:** No privacy for downloaded content.

### 16. File Sharing
**SnapTube:** One-tap share of downloaded files to WhatsApp, Messenger, Bluetooth, etc.
**BeatDrop:** No sharing intent for downloaded files. No `ACTION_SEND` integration for local files.
**Impact:** Users cannot share downloaded music with friends.

### 17. SD Card Storage Selection
**SnapTube:** Option to save downloads directly to SD card to save internal storage.
**BeatDrop:** Downloads go to `app.getExternalFilesDir(null)/BeatDrop/Downloads/` (internal storage only). No SD card selection.
**Impact:** Users with large libraries may run out of internal storage.

### 18. Storage Management Dashboard
**SnapTube:** Shows total storage used, remaining space, file management UI, phone cleaner.
**BeatDrop:** No storage information displayed anywhere. No file management UI.
**Impact:** Users have no visibility into download storage usage.

---

## 🟡 MODERATE GAPS (Quality-of-life features)

### 19. ID3 Metadata Tagging in Downloaded Files
**SnapTube:** Writes proper ID3 tags (title, artist, album, year, cover art) into the downloaded file so other music players read them correctly.
**BeatDrop:** `parseTitle()` extracts clean title/artist from YouTube metadata and stores it in the `Track` object, but does NOT write ID3 tags into the actual audio file. If the user opens the downloaded `.m4a`/`.opus` in another music player, it shows "Unknown" metadata.
**Impact:** Downloads are invisible/misidentified in other music players.

### 20. Recover Deleted Downloads
**SnapTube:** "Recover deleted files" option that re-downloads accidentally deleted content.
**BeatDrop:** No download history persistence. `DownloadManager.jobs` is in-memory (`MutableStateFlow`) and clears on process death. Deleted downloads are gone forever.
**Impact:** Accidental deletions require a full re-search and re-download.

### 21. Download History
**SnapTube:** Persistent download history with management (delete, re-download, share).
**BeatDrop:** Download jobs are ephemeral — stored in a `StateFlow<Map<String, DownloadJob>>` that resets when the app restarts. No persistent database of what was downloaded.
**Impact:** Users lose track of what they've downloaded.

### 22. Localization / Multi-language
**SnapTube:** Full UI in 30+ languages (Arabic, Chinese, French, Hindi, Spanish, Portuguese, etc.).
**BeatDrop:** All UI strings are hardcoded in English. No `strings.xml` translations. No locale handling.
**Impact:** Non-English-speaking users are excluded.

### 23. Built-in VPN
**SnapTube:** Integrated VPN to bypass regional restrictions on content.
**BeatDrop:** No VPN. Region-locked content is handled by trying multiple Piped/Invidious instances, which may or may not work.
**Impact:** Some content is inaccessible in restricted regions.

### 24. Age-Restricted Content
**SnapTube:** Can download age-restricted videos (via logged-in session or workaround).
**BeatDrop:** Age-restricted videos return `playabilityStatus=ERROR` from Innertube clients. Piped may work sometimes. WebView extractor may work if cookies consent is seeded. No reliable path.
**Impact:** Some music videos (with age gates) fail silently.

### 25. Search Result Filtering
**SnapTube:** Filter search results by duration, date, quality, type (video vs audio).
**BeatDrop:** Search returns results sorted by `musicRelevanceScore()`. No user-facing filters for duration, date, quality, or type.
**Impact:** Users cannot narrow down search results to find what they want.

### 26. Video Preview Before Download
**SnapTube:** Can play/preview the video in-app before committing to download.
**BeatDrop:** Can stream audio (effectively a preview), but no video preview. No inline playback in search results.
**Impact:** Users download "blind" based on title and thumbnail only.

---

## 🟢 What BeatDrop Already Has (no gap)

| Feature | Status |
|---------|--------|
| YouTube audio streaming (multi-tier resolver) | ✅ 5-tier: Piped → Innertube → WebView → Piped exhaustive → Invidious |
| YouTube audio download | ✅ Parallel chunked (4 chunks) + serial fallback |
| Search with autocomplete | ✅ `getSearchSuggestions()` + YouTube Music search |
| Download foreground service | ✅ `DownloadService` with notification progress |
| Stream URL caching (90-min TTL) | ✅ `urlCache` with auto-expiry |
| 403/expired URL auto-recovery | ✅ Re-resolves + resumes at same position |
| Dark mode | ✅ Theme.kt with light/dark toggle |
| Background playback | ✅ MediaSession + MediaNotification |
| Equalizer (real DSP) | ✅ `EqEngine` with `android.media.audiofx` |
| Auto-Mix / Auto-DJ | ✅ Crossfade with equal-power curves, loudness matching |
| Lyrics (synced) | ✅ LRC parsing + LrcLib online provider |
| Playback cache (200 MB) | ✅ `SimpleCache` with LRU eviction |
| Download retry | ✅ `DownloadManager.retry()` |
| YouTube cipher/signature handling | ✅ Rhino JS engine + WebView extractor |

---

## 📋 Priority Recommendations for Closing the Gap

### Tier 1 — Maximum Impact (Do These First)
1. **Clipboard URL detection + Share menu integration** — Accept YouTube URLs from clipboard and Android Share. Low effort, huge UX win.
2. **URL-based playback** — Let users paste a `youtube.com/watch?v=...` URL to play/download without searching.
3. **Batch download** — Multi-select in search results + "Download All" for playlists.
4. **ID3 tagging** — Write metadata into downloaded files so they work in other players.
5. **Download pause/resume** — Save partial download state, resume from checkpoint.

### Tier 2 — High Impact
6. **Video downloads** — Add video format selection (at minimum 720p MP4).
7. **Per-download format/quality picker** — Dialog before download with bitrate/resolution options.
8. **Online trending/discovery feed** — Show YouTube trending, new releases, curated playlists.
9. **Download history** — Persistent SQLite/Room database of all downloads.
10. **WiFi-only guard** — Setting to block downloads on mobile data.

### Tier 3 — Polish
11. **File sharing** — Share downloaded files via `ACTION_SEND`.
12. **SD card storage** — Option to save downloads to external storage.
13. **Localization** — Extract strings, add translations for top languages.
14. **Storage management** — Dashboard showing download sizes and cleanup options.
