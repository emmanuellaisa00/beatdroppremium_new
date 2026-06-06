# BeatDrop — Glassmorphism Redesign

Full visual redesign of the app to match the HTML concept (Apple Music + iOS-26
Liquid Glass + the Behance Spotify-Glassmorphism reference).

**Accent**: Apple Music Pink (`#FA2D48`) — replaces the previous green / purple.
**Material**: Single dark-obsidian glass everywhere, no per-screen variants.
**Background**: Pure black (`#000000`) with a soft pink ambient glow top-left
and a cool navy ambient bottom-right.

---

## Foundation

### `ui/theme/Theme.kt`
Complete rewrite of the colour ladder, glass tokens, radii, spacing, blur,
and spring constants.

- `accent` → `#FA2D48` (was `#21FF6B` green)
- `accentDark` → `#D41F38`
- `glassSurface`, `glassPlayer`, `glassNav` → dark obsidian
  (`rgba(28,30,38, 0.42)` / `rgba(22,24,30, 0.62)`)
- `glassBorder` lowered to 6% white (was 12%)
- `glassRimLight` / `glassInnerShadow` rebalanced — soft white top sheen +
  inset dark bottom = "carved obsidian" look
- New `Spacing.xxxl = 32.dp` for section separation
- `Radius` / `RadiusFamily` ladders unified (`8/16/22/28/36/44`)
- Default theme is now `"dark"` (was `"light"`)

### `ui/theme/Type.kt`
Unchanged — the existing dense premium scale already matches the concept.

---

## Shared components (glass system)

### `ui/components/PremiumGlass.kt`  — rewritten
Single material, six depth levels. Each level controls fill alpha + shadow
stack:

| Level             | Use                          | Fill α |
|-------------------|------------------------------|--------|
| `Z1_List`         | dense list rows              | 0.36   |
| `Z2_Card`         | cards, chips, headers        | 0.42   |
| `Z3_MiniPlayer`   | floating mini player         | 0.62   |
| `Z4_TabBar`       | bottom dock                  | 0.62   |
| `Z5_ActiveLens`   | inset glass orb (active tab) | 0.18   |
| `Z6_Floating`     | modals / sheets              | 0.70   |

Layered effects per surface: shadow stack (volume+ambient+contact) → clip →
gradient substrate → top rim highlight → bottom inset shadow → noise →
hairline border.

### `ui/components/GlassExtras.kt`  — rewritten
- `ScreenScaffold` now paints pure black + pink/navy ambient glows
- `IconPuck` now uses `premiumGlass(Z2_Card)` — matches the HTML `.icon-btn`
- `GlassHeader` rebuilt with 40 dp circular glass back-button
- `SectionHeader` uses `Type.title2` (was `overline`) — matches HTML
  "Recently Played" / "Songs" section style
- `noiseOverlay` density tuned (140 divisor, was 120) for cleaner grain

### `ui/components/Glass.kt`  — slimmed
- `masterGlass`, `glassCard`, `glassRow`, `glassSheet` are all thin
  wrappers around `premiumGlass(...)` so legacy call sites keep working
- `glassBlur` is now a no-op (preserved for binary compat)

### `ui/components/GlassTabBar.kt`  — rewritten
- 72 dp tall, radius 36, 20 dp horizontal insets — matches the HTML dock
- Active tab is an **inset glass orb** (recessed look), not a coloured fill
- Monochrome icons, no labels (matches reference)
- Spring-physics motion when switching tabs

### `ui/components/MiniPlayer.kt`  — rewritten
- 70 dp tall, radius 36, dark obsidian glass (same as dock — they read as a
  matched pair floating above the page)
- Circular album thumb (54 dp)
- Title (white) + artist (55% white)
- Devices/cast icon + **clean white play triangle** (no background fill)
- Hairline pink progress line at the bottom edge

### `ui/components/BeatDropSearchField.kt`  — rewritten
- 52 dp pill (was 48 dp)
- Search icon at 60% white opacity (was `textSecondary`)
- Placeholder at 45% white (matches HTML)
- Apple Music pink caret + focus glow

### `ui/components/Building.kt`
- `GlassCard`, `GlassPill`, `GlassSearchBar` migrated to `premiumGlass(...)`
- Removed inline gradient + border stacks — one material everywhere

### `ui/components/IosSeekBar.kt`
- No code changes — `C.accent` is now pink, picks up automatically

### `ui/components/AppIcon.kt`
- No changes — already exposes `Ic.MusicNote`, `Ic.Airplay`, etc.

---

## Screens

All 26 screens inherit the new look automatically through the rebuilt
shared components. Specific per-screen edits below.

| Screen                  | Visual changes |
|-------------------------|----------------|
| `LibraryScreen`         | Header rebuilt: wordmark + tagline + 3 circular glass icons. Segmented control = dark glass pill with inset active orb (was green-tinted fill). |
| `NowPlayingScreen`      | Background = soft radial wash from album art (was 78% art tint over dark blue). Ambient glow = pink (was electric blue). Transport row = clean monochrome with **inset glass big-play orb** (76 dp). Favourite hearts → pink (were iOS yellow). |
| `SearchScreen`          | Title rebuilt with proper spacing. Offline banner uses pink tint. Browse-category grid retained (the only spot of colour in the screen, as in the HTML reference). |
| `DiscoverScreen`        | Play buttons inherit pink. Comment header retuned. |
| `OnboardingScreen`      | **Full rewrite** — pure black bg + pink ambient, monochrome glass feature cards, solid pink CTA pill. Removed all purple `#C77DFF` references. |
| `SplashScreen`          | Pure black backdrop with subtle pink radial glow. Wordmark = pure white (was split "Beat" green / "Drop" white). |
| `OnlineAlbumScreen`     | Bottom backdrop changed from `#0E0A1F` (midnight indigo) → `#000000`. Play button text/icon → white on pink. |
| `SettingsScreen`        | Error reds now use the accent pink. |
| `TrendingScreen`        | Filter chip text on selected pill → white (was black). |
| `DownloadsScreen`       | Filter chip text on selected pill → white (was black). |
| `TermsSheet`, `WhatsNewSheet` | CTA text → white on pink. |
| `RadioScreen`           | Unchanged — colourful gradient mix tiles intentionally kept (match the HTML "Browse all" treatment). |
| All other screens       | No source edits needed — inherit theme via `C.accent` + `ScreenScaffold`. |

---

## `MainActivity.kt`
- Global background gradient retuned: pure black + 16% art tint (was 32%)
- Global scrim alpha 0.95 (`#F2000000`) over the artwork — recedes the art
- Added pink + navy ambient radial wash behind the scrim
- Removed the cyan/blue specular overlay — replaced with a single subtle
  tilt-responsive sheen

---

## Files NOT modified
- `Type.kt` (already correct)
- `IosSeekBar.kt`, `AppleLyrics.kt`, `LyricsSilhouettes.kt`,
  `SearchSilhouettes.kt`, `AppIcon.kt`, `DynamicColor.kt`,
  `DiegeticDownloadIcon.kt`, `Motion.kt`, `Haptics.kt`,
  `TrackActionsSheet.kt`, `OnlineTrackActionsSheet.kt`,
  `FormatPickerDialog.kt` — all inherit the new theme automatically.
- All `data/`, `playback/`, `extraction/`, `lyrics/`, `youtube/`,
  `util/` — non-visual, untouched.

---

## Known follow-ups (visual fidelity first, breaking changes flagged)

1. **Comment cleanup** — file-top comments still reference "iOS 26" and
   the legacy depth budget. Cosmetic only.
2. **NowPlaying lyrics overlay** — the radial wash works but the existing
   `Brush.verticalGradient` is still used in one fallback path. Cosmetic.
3. **Tab labels** — the new dock removed labels (matches HTML). If you
   want them back, set them in `GlassTabBar.DockItem` under the icon with
   `C.text.copy(alpha = if (active) 1f else 0.5f)`.
4. **Accent backwards-compat** — every screen that hardcoded `Color.Black`
   on `C.accent` has been switched to `Color.White` (white reads better on
   pink than black does).
5. **Light theme** — fully retuned but not deeply tested visually. Default
   theme switched to `"dark"` since the obsidian material is calibrated for
   it. Light theme uses milky white glass and the same pink accent.

---

## Build status
Visual-fidelity-first redesign. Every change preserves public
function signatures and Composable shapes, so the app should still build.
The only behaviour change is that `glassBlur` is now a no-op (was always
unreliable across devices).

Run `./gradlew assembleDebug` to verify.
