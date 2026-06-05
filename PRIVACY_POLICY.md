# BeatDrop Premium — Privacy Policy & Terms

**Publisher:** Laisacorp
**Last updated:** 2026-06-05
**App version:** v1.2.0

This document explains what BeatDrop Premium ("the app", "we") does with
your data, what you agree to by using it, and how to exercise the rights
you have under applicable data-protection law (in particular, the EU
**GDPR**, the **UK GDPR**, the **CCPA / CPRA**, and Kenya's **Data
Protection Act 2019**).

---

## 1. Short version

* The app stores **your music library, downloads, preferences and search
  history on your device only**. We don't run a backend that mirrors any
  of it.
* Stream URLs and search results are fetched from **YouTube / YouTube
  Music** (a Google service). When you stream or search, your device
  contacts Google's servers directly.
* The app does **not** collect, transmit, sell, or share any personal data
  with Laisacorp, or with any third party other than Google in the course
  of the requests above.
* You can erase everything by uninstalling the app or clearing its data
  in Android Settings.

If that's enough for you, **tap Accept** and continue.

---

## 2. What data the app handles

### 2.1 Data stored locally on your device

| Category | Examples | Purpose |
|---|---|---|
| Library index | Track titles, artists, albums, file paths | Build the in-app library |
| Playback state | Liked songs, playlists, play counts, last-played history | Personalise the UI |
| Preferences | Theme, crossfade ms, Wi-Fi-only switch, smart-shuffle on/off | Honor your settings |
| Search history | Last few queries you typed | Autocomplete |
| Downloads | Audio files + a JSON history index | Offline playback |
| Lyrics cache | `.lrc` files matched by track | Synced lyrics display |

All of the above is kept under `Android/data/com.beatdrop.kt/` and the
app's `DataStore` file. **None of it leaves your phone.**

### 2.2 Data transmitted off-device

The app makes outbound network requests **only** to:

| Endpoint | What's sent | Why |
|---|---|---|
| `youtube.com`, `youtubei.googleapis.com`, `music.youtube.com` | Your typed search query, the videoId you're trying to play, standard browser headers (User-Agent, IP) | Search results, stream-URL resolution |
| `googlevideo.com` (Google's CDN) | Standard HTTP range requests for audio | Audio streaming |
| Public Invidious / Piped mirrors (configurable, off by default for Piped) | Same as above | Fallback resolvers when Innertube is unreliable |
| `lrclib.net` | Track title + artist | Synced lyrics lookup |
| **Your self-hosted resolver backend** (if you've configured one in Settings → Streaming) | videoId | Optional `/resolve?id=` request |

We do **not** route any request through a Laisacorp-controlled server.
There is no Laisacorp analytics, telemetry, or crash-reporting SDK in
the app.

### 2.3 Data the app does *not* collect

* No account, login, email, phone number, or other identifier.
* No advertising ID.
* No location.
* No microphone, camera, or contacts access.
* No background tracking of any kind.

### 2.4 Permissions the app requests

| Android permission | Why |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Scan your local music library |
| `READ_MEDIA_IMAGES` | Show album-art covers from local files |
| `MANAGE_EXTERNAL_STORAGE` *(sideload builds only)* | Read sidecar `.lrc` lyrics + write downloads to user-chosen folders |
| `INTERNET` | Fetch search results, streams, lyrics |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep audio playing with the screen off |
| `POST_NOTIFICATIONS` (Android 13+) | Show the playback / download notification |

---

## 3. Lawful basis (GDPR / UK GDPR)

For users in the EU / UK / EEA the lawful bases under Article 6 GDPR
are:

* **Performance of the service** (Art. 6(1)(b)) — for everything needed
  to make playback, search and downloads work.
* **Consent** (Art. 6(1)(a)) — for the optional self-hosted resolver
  backend you may configure in Settings. You can withdraw consent at
  any time by clearing the URL.

We do not rely on legitimate-interests processing because we do not
collect personal data on our infrastructure.

---

## 4. Data subject rights

Because we hold no personal data on our servers, classic data-subject
requests (access, rectification, erasure, portability) are satisfied
by your own control over the device:

* **Access / Portability** — All of your library, playlists, liked songs,
  and download history lives in `Android/data/com.beatdrop.kt/`. You
  can copy it freely.
* **Erasure** — Uninstall the app, or use Android Settings → Apps →
  BeatDrop → Storage → Clear Data.
* **Restriction** — Toggle off the optional resolver backend in Settings,
  enable Wi-Fi-only downloads, or simply uninstall.
* **Objection** — N/A; we don't process anything you can object to.

For requests under the Kenya DPA or any other applicable law, contact
us at the address in §8.

---

## 5. Children

The app is not directed at children under 13. If you are a parent or
guardian and believe a child has used the app in a way that concerns
you, contact us (§8). There is no account or backend record to delete
on our side — clearing app data on the device removes everything the
app has retained.

---

## 6. YouTube / Google relationship

BeatDrop is **not** affiliated with, endorsed by, or sponsored by
Google LLC, YouTube LLC, YouTube Music, or any of their subsidiaries.
By streaming or downloading content fetched from those services you
are subject to the relevant Google / YouTube Terms of Service. You
are responsible for ensuring your use complies with local copyright
law. Where a track is licensed for offline use, the offline rights
remain those granted by the original platform.

---

## 7. Security

* All network traffic uses HTTPS.
* The app does not run a background server, does not bind any network
  socket as a listener, and does not accept incoming connections.
* Sensitive prefs (e.g. the optional Private Folder PIN) are stored in
  encrypted form in Android's standard DataStore. The PIN never leaves
  the device.

---

## 8. Contact

| | |
|---|---|
| Publisher | Laisacorp |
| Contact | Open a GitHub issue at the project repository |
| Repository | https://github.com/emmanuellaisa00/beatdroppremium |

For data-protection requests, please put **"GDPR / DPA request"** in the
issue title.

---

## 9. Changes to this policy

We may update this policy when the app changes in a way that affects
how data flows. When we do, the "Last updated" date at the top changes
and the app re-prompts you for acceptance on next launch.

---

## 10. Acceptance

By tapping **Accept** in the in-app sheet, or by continuing to use the
Discover or Search features after seeing this notice, you confirm that
you have read this policy and the underlying Terms (which are the
Google / YouTube ToS for any catalog requests we relay) and agree to be
bound by them.

If you do **not** agree, please uninstall the app — none of your data
has been transmitted off-device merely by opening it for the first time.
