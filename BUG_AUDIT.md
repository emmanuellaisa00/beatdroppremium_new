# BeatDrop Architecture Audit — Bug Report

## CRITICAL BUGS

### 🔴 B1. `buildSmartQueue()` double-removes on fallback path
**File:** `playback/OnlineSmartShuffle.kt:152-154`
**Severity:** Logic bug (harmless but indicates fragile code)
```kotlin
val next = pickNext(cur, remaining, recent, likedVideoIds)
    ?: remaining.removeFirst()   // (A) removes AND returns first item
remaining.remove(next)           // (B) tries to remove next again — no-op
```
**Trigger:** When all remaining tracks are in the recently-played ring, `pickNext` returns null. `removeFirst()` pops the first item (assigning to `next`), then `remove(next)` tries to remove it again — returns false, silently ignored.  
**Impact:** Works correctly by accident. Would break if `removeFirst()` were replaced with a non-mutating fallback.  
**Fix:** Restructure to `val next = pickNext(...) ?: remaining.removeFirst(); /* no second remove needed */`

---

### 🟠 B2. `cancelAutoMix()` races with `triggerOnlineAutoMix()` coroutine `finally` block
**File:** `PlayerViewModel.kt` (lines 869-883 vs 880-935)
**Severity:** Race condition (benign in practice, but undefined ordering)
```kotlin
// cancelAutoMix sets:
crossfadeJob = null
job.cancel()
controller?.volume = 1f

// But triggerOnlineAutoMix's finally block also runs:
isCrossfading = false; _mixingNext.value = null; crossfadeJob = null
```
**Trigger:** User hits skip during online fade. `cancelAutoMix` cancels the coroutine, sets volume=1, then `next()` calls `prepareAndPlayOnline` which starts a new resolve. The cancelled coroutine's `finally` block runs concurrently and sets `isCrossfading = false` — potentially after `next()` has already started the new play. The `finally` block also accesses `_mixingNext` which is now stale.
**Fix:** Guard the `finally` block with a `wasCancelled` check, or use a cancellation token.

---

## HIGH BUGS

### 🟠 B3. `onlineRecentlyPlayedIds` loaded from DataStore in arbitrary order
**File:** `PlayerViewModel.kt:296`
**Severity:** Incorrect recency tracking (LRU ring broken)
```kotlin
prefs.onlineRecentlyPlayedFlow.onEach { 
    onlineRecentlyPlayedIds.clear()
    onlineRecentlyPlayedIds.addAll(it) // Set → arbitrary iteration order
}
```
**Problem:** `pushRecentOnline` builds a proper LRU deque (last→first). But `setOnlineRecentlyPlayed` saves a `Set` → loses order. On restore, the deque has correct *elements* but wrong *order*. The first track the user plays after restart will incorrectly think the most-recently-added (not most-recently-played) is "last" and skip it.
**Fix:** Save as `JSONArray` preserving order, not `Set`. Or save a `List`.

---

### 🟠 B4. `triggerPrefetch()` skips online tracks entirely
**File:** `PlayerViewModel.kt:1058`
```kotlin
if (cur.isStreaming) return
```
**Impact:** When smart shuffle is playing online tracks, the next candidate's URL is never pre-resolved. The `triggerOnlineAutoMix` handler must resolve the URL during the fade (blocking), adding 200-800ms of silence. For local tracks, prefetch warms the Deck B buffer — the online equivalent (warming the HTTP cache) is never done.
**Fix:** Add an online prefetch path that calls `getStream()` in background to warm the playback cache.

---

### 🟠 B5. `next()` bypasses ExoPlayer queue for online tracks → lock screen shows wrong "Next" track
**File:** `PlayerViewModel.kt:599-616`
**Severity:** UX inconsistency
```kotlin
if (cur?.isStreaming == true && onlineContext.isNotEmpty()) {
    val nextIdx = onlineContextIndex + 1
    if (nextIdx in onlineContext.indices) {
        prepareAndPlayOnline(nextResult, onlineContext, nextIdx)
        return  // ← never updates ExoPlayer's queue
    }
}
```
**Problem:** `next()` calls `prepareAndPlayOnline` which uses `setMediaItem` (single item). The ExoPlayer queue never gets the next track. The lock screen / Bluetooth controls show an empty "Next" slot. During local playback, the queue is populated via `setMediaItems` or `addMediaItem`.
**Fix:** After smart shuffle, call `addMediaItem` to populate the next 1-2 tracks in ExoPlayer's queue.

---

### 🟠 B6. `triggerOnlineAutoMix` doesn't save `repeatMode` before the fade
**File:** `PlayerViewModel.kt:880-935`
**Severity:** Repeat mode lost after online crossfade
```kotlin
private fun triggerOnlineAutoMix(nextResult: OnlineResult, fadeMs: Long) {
    // ...
    val mainPlayer = controller ?: run { ... return }
    savedRepeatMode = mainPlayer.repeatMode  // ← saved but...
    crossfadeJob = viewModelScope.launch {
        // ...
        mainPlayer.repeatMode = savedRepeatMode  // ← restored AFTER stop/clear
        mainPlayer.stop(); mainPlayer.clearMediaItems()
    }
}
```
**Problem:** `mainPlayer.stop()` + `clearMediaItems()` on a MediaController may reset the repeat mode. The assignment `mainPlayer.repeatMode = savedRepeatMode` happens before those calls, but MediaController's `stop()` can trigger state changes that overwrite it. The order should be: stop → clear → set repeat mode → set media item → prepare → play.
**Fix:** Move `mainPlayer.repeatMode = savedRepeatMode` to after `clearMediaItems()`.

---

## MEDIUM BUGS

### 🟡 B7. `_ytTrackCache` grows unbounded — memory leak
**File:** `PlayerViewModel.kt:961`
```kotlin
private val _ytTrackCache = mutableMapOf<String, Track>()
```
**Problem:** Every online track played is cached in this map. For a long session (100+ tracks), each `Track` object holds stream headers, UAs, and artwork URIs. Never evicted. In a backgrounded app, this can contribute to OOM.
**Fix:** Use `LinkedHashMap` with LRU eviction (e.g., max 50 entries).

---

### 🟡 B8. Download button creates `OnlineResult` with potentially wrong `durationSecs`
**File:** `NowPlayingScreen.kt` (download button)
```kotlin
durationSecs = ((track?.durationMs ?: 0L) / 1000L).toInt()
```
**Problem:** `track.durationMs` could be 0 during the temp-track phase, or could be wrong for tracks where `youtubeResultToTrack` returned `durationMs = result.durationSecs * 1000L` but the source `OnlineResult` had `durationSecs = 0`. The download uses this `OnlineResult` for metadata — wrong duration leads to wrong display in the download manager.
**Fix:** Use the resolved track's actual duration from the DownloadManager's completion callback.

---

### 🟡 B9. No debounce on `getDiscoverData` — rapid tab switches cause concurrent fetches
**File:** `PlayerViewModel.kt:210-241`
```kotlin
fun getDiscoverData(forceRefresh: Boolean = false) {
    if (_discoverLoading.value) return  // ← only prevents double-fetch
    // ...
}
```
**Problem:** `_discoverLoading` prevents concurrent fetches, but if called rapidly (tab switch → back → tab switch), the second call is silently dropped. The user sees stale cache until the next 5-minute window. No debounce, no retry.
**Fix:** Track last request time and allow a "soft refresh" (< 5 min) if the user explicitly requests it.

---

### 🟡 B10. `prefs.setDiscoverCache` called on every fetch — unnecessary I/O
**File:** `PlayerViewModel.kt:233-237`
```kotlin
runCatching {
    prefs.setDiscoverCache(
        com.beatdrop.kt.data.Prefs.DiscoverCache(trending, pop, hiphop, _discoverLastFetch.value)
    )
}
```
**Problem:** Every 5-minute refresh writes to DataStore even if the data hasn't changed. DataStore serializes to protobuf on disk — this is wasted I/O for the same JSON.
**Fix:** Compare against cached values before persisting.

---

### 🟡 B11. `playOnlineByUrl` creates context with single item — no next/prev
**File:** `PlayerViewModel.kt:1329-1345`
```kotlin
fun playOnlineByUrl(url: String) {
    // ...
    playOnline(result)  // calls prepareAndPlayOnline with default context = listOf(result)
}
```
**Problem:** When playing from clipboard/deep link, the context is a single-item list. `next()` reaches the end and falls through to `seekToNextMediaItem()` which is a no-op for online tracks. User can't skip.
**Fix:** Search for the detected video and build a context from related results.

---

## LOW BUGS

### 🟢 B12. `"live"` in `remixKeywords` contradicts `isLive` penalty
**File:** `OnlineSmartShuffle.kt:32`
```kotlin
private val remixKeywords = setOf("remix", "acoustic", "live", ...)
// ...
if (candidate.isLive) s -= 20f
```
**Problem:** "live" in the title gives +3 bonus (remix keyword), but `isLive = true` gives -20 penalty. These are contradictory. A live performance labeled "Live at..." gets both bonuses.
**Fix:** Remove `"live"` from `remixKeywords`.

---

### 🟢 B13. Download button shows for non-YouTube streaming sources
**File:** `NowPlayingScreen.kt`
```kotlin
val videoId = track?.sourceVideoId
```
**Problem:** `sourceVideoId` is set to `result.videoId` by `youtubeResultToTrack`. For Piped/Invidious sources, the download manager may not handle these correctly. The button shows but download may fail silently.
**Fix:** Check `sourcePlatform` field before showing download button.

---

### 🟢 B14. `stop()` on MediaController during online handoff loses audio session
**File:** `PlayerViewModel.kt:928`
```kotlin
mainPlayer.stop(); mainPlayer.clearMediaItems()
```
**Problem:** `stop()` on a MediaController detaches the audio session. This causes a brief "audio duck" on some devices, and the system notification disappears momentarily before the new track starts. For local crossfade, the code carefully avoids `stop()` for this reason.
**Fix:** Use `seekTo` + `setMediaItem` without `stop()`, or use `player.stop()` (not controller) if the MediaSession needs to stay alive.

---

### 🟢 B15. No `pushRecent` in `playOnline` (single-arg overload)
**File:** `PlayerViewModel.kt:1169-1171`
```kotlin
fun playOnline(result: OnlineResult) {
    prepareAndPlayOnline(result)  // ← context defaults to listOf(result)
}
```
**Problem:** `playOnline` is called from `playOnlineByUrl` and `ChannelScreen`. These paths don't call `pushRecentOnline` directly — it's done inside `prepareAndPlayOnline`. This is actually fine, but tracing it is confusing. The code is correct but the call chain is fragile.
**Fix:** Document the call chain, or make `pushRecentOnline` call unconditional in `prepareAndPlayOnline`.

---

### 🟢 B16. `onlineContextIndex` not updated after `triggerOnlineAutoMix` when `nextResult` is not in context
**File:** `PlayerViewModel.kt:935`
```kotlin
onlineContextIndex = onlineContext.indexOfFirst { it.videoId == nextResult.videoId }.coerceAtLeast(0)
```
**Problem:** If the smart shuffle picks a track that's NOT in `onlineContext` (e.g., from the local pool, or the hybrid picker drops a track), `indexOfFirst` returns -1, coerced to 0. This means `next()` will jump to the *first* item in the context, not the one after the current.
**Fix:** Check if `indexOfFirst` returned -1 and handle it gracefully (e.g., don't update context).

---

### 🟢 B17. `Ic.Search` doesn't have an outlined variant for TabSpec2
**File:** `MainActivity.kt:165`
```kotlin
TabSpec2("search", "Search", Ic.Search, Ic.Search),
```
**Problem:** `TabSpec2` takes `iconFilled` and `iconOutlined` separately. All tabs use the same icon for both, which works visually but defeats the UX purpose of having filled/outlined state distinction. The search icon is always the same Lucide icon.
**Fix:** Not a bug per se, but inconsistent with the original design intent of `TabSpec2`.

---

## SUMMARY

| Severity | Count | Areas |
|----------|-------|-------|
| Critical | 1 | Double-remove logic in buildSmartQueue |
| High | 4 | LRU ring corruption, no online prefetch, lock screen queue, cancel/finally race |
| Medium | 5 | Memory leak, wrong metadata, unnecessary I/O, no debounce, single-item context |
| Low | 6 | Keyword contradiction, non-YouTube download, audio session, code clarity |

**Not affected:** YouTube extraction layer (0 changes to all 15 files), PlaybackService, local playback pipeline, GlassTabBar, Equalizer, Lyrics engine.