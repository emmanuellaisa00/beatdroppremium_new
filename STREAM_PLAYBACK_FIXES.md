# Stream Playback Fixes — "play like SnapTube when you tap a search result"

This document summarizes the changes made so that **Search → tap result → audio plays**
works reliably, and so the project builds at all.

## 1. Build-breaker: malformed AndroidManifest (🔴 was preventing any APK)
`app/src/main/AndroidManifest.xml` had a `READ_EXTERNAL_STORAGE` element closed twice
with stray `tools:ignore` text after `/>`. That is invalid XML and failed the build.
**Fixed** — merged into a single well-formed element.

## 2. Native signature / n-throttle decipher (new)
Added `youtube/YoutubeCipher.kt`:
- Downloads the player `base.js` (cached per player version).
- Extracts the **signature decipher** function (+ its helper object) and the
  **`n`-throttle transform** using current yt-dlp-style regexes.
- Evaluates them with **Mozilla Rhino** (`org.mozilla:rhino:1.7.14`, interpreted
  mode — required on Android).
- `resolveFormatUrl()` now accepts **ciphered** formats (`signatureCipher`) that the
  old code discarded, and fixes the throttling `n` parameter on plain URLs too.

`YoutubeService.getStream()` primes the cipher and runs ciphered adaptive formats
through it. The pure-"plain URL only" filter that previously dropped most 2026
formats is gone for the streaming path.

> The WebView extractor (`YoutubeExtractor`) remains the **primary**: it uses the
> browser's own V8 to run YouTube's JS, so it survives obfuscation changes that
> break regex extraction. The native cipher is a best-effort **secondary** with
> graceful degradation (any failure → next strategy).

## 3. Per-stream UA/headers carried into ExoPlayer (fixes 403)
googlevideo URLs are bound to the client that resolved them. Previously the URL was
resolved with one UA but `PlaybackService` fetched it with a single hard-coded UA,
causing 403s.

- `getStream()` now returns a `ResolvedStream(url, userAgent, headers)`.
- `Track` carries `streamUserAgent` / `streamHeaders` / `sourceVideoId`.
- `PlayerViewModel.toMediaItem()` encodes them into the URI fragment
  (`#bdua=...&bdh_Referer=...`).
- `PlaybackService` uses a `ResolvingDataSource` that parses that fragment, strips
  it, and applies the values (incl. `User-Agent`) as HTTP request headers.
- The downloader replays the same UA/headers on its HEAD + chunked/serial requests.

## 4. Adaptive-streaming codecs
Added `media3-exoplayer-hls` and `media3-exoplayer-dash` so HLS/DASH manifests
(returned by some videos/Invidious) actually play instead of failing to demux.

## 5. 403 / expired-token auto-recovery
`PlayerViewModel`'s `onPlayerError` now, on `ERROR_CODE_IO_BAD_HTTP_STATUS` for a
streaming track, invalidates the cached URL, re-resolves the stream, seeks back to
the previous position, and resumes — once per track (loop-guarded).

## 6. Misc hardening
- URL cache TTL reduced 4h → 90m (googlevideo tokens expire faster).
- `invalidateStreamCache(videoId)` exposed for recovery.
- `setHandleAudioBecomingNoisy(true)` on the player (pause on unplug).

---

### Build
`./gradlew assembleDebug` — or push to `main`; GitHub Actions builds + releases the APK.

### Reality check
If a given video can't be resolved by **any** of: WebView (V8) → Innertube+cipher →
Invidious, it will surface a clear message and you can pick another track. That's the
same failure mode SnapTube/yt-dlp have when YouTube ships a breaking player update.
