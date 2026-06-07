package com.beatdrop.kt

import com.beatdrop.kt.youtube.extractVideoRenderers
import com.beatdrop.kt.youtube.parseInnertubeRenderer
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class YoutubeServiceTest {

    @Test
    fun parseTimestamp_standardFormat() {
        // Access via reflection since parseTimestamp is private
        // Instead test via parseInnertubeRenderer which uses it
        val vr = JSONObject().apply {
            put("videoId", "test123")
            put("title", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Test Song"))))
            put("ownerText", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Test Artist"))))
            put("lengthText", JSONObject().put("simpleText", "3:45"))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertEquals(225, result!!.durationSecs) // 3*60 + 45
        assertEquals("test123", result.videoId)
    }

    @Test
    fun parseInnertubeRenderer_extractsVideoId() {
        val vr = JSONObject().apply {
            put("videoId", "abc123")
            put("title", JSONObject().put("simpleText", "My Song"))
            put("lengthText", JSONObject().put("simpleText", "4:20"))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertEquals("abc123", result!!.videoId)
        assertEquals(260, result.durationSecs)
    }

    @Test
    fun parseInnertubeRenderer_missingVideoId_returnsNull() {
        val vr = JSONObject().apply {
            put("title", JSONObject().put("simpleText", "No ID"))
        }
        assertNull(parseInnertubeRenderer(vr))
    }

    @Test
    fun parseInnertubeRenderer_missingTitle_returnsNull() {
        val vr = JSONObject().apply {
            put("videoId", "test")
        }
        assertNull(parseInnertubeRenderer(vr))
    }

    @Test
    fun parseInnertubeRenderer_liveStream_detected() {
        val vr = JSONObject().apply {
            put("videoId", "live1")
            put("title", JSONObject().put("simpleText", "Live Stream"))
            put("badges", JSONArray().put(
                JSONObject().put("metadataBadgeRenderer",
                    JSONObject().put("style", "BADGE_STYLE_TYPE_LIVE_NOW").put("label", "LIVE"))
            ))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertTrue(result!!.isLive)
    }

    @Test
    fun parseInnertubeRenderer_notLive_whenNoBadge() {
        val vr = JSONObject().apply {
            put("videoId", "normal1")
            put("title", JSONObject().put("simpleText", "Normal Song"))
            put("lengthText", JSONObject().put("simpleText", "3:30"))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertFalse(result!!.isLive)
    }

    @Test
    fun parseInnertubeRenderer_noDuration_notFlaggedAsLive() {
        // This was the bug — empty lengthText was falsely marking as live
        val vr = JSONObject().apply {
            put("videoId", "nodur1")
            put("title", JSONObject().put("simpleText", "Unknown Duration"))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertFalse(result!!.isLive) // Should NOT be live just because duration is unknown
        assertEquals(0, result.durationSecs)
    }

    @Test
    fun parseInnertubeRenderer_accessibilityDuration() {
        val vr = JSONObject().apply {
            put("videoId", "acc1")
            put("title", JSONObject().put("simpleText", "Accessible"))
            put("lengthText", JSONObject().put("accessibility",
                JSONObject().put("accessibilityData",
                    JSONObject().put("label", "3 minutes, 45 seconds"))))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertEquals(225, result!!.durationSecs)
    }

    @Test
    fun parseInnertubeRenderer_durationFromOverlay() {
        val vr = JSONObject().apply {
            put("videoId", "overlay1")
            put("title", JSONObject().put("simpleText", "Overlay Duration"))
            put("thumbnailOverlays", JSONArray().put(
                JSONObject().put("thumbnailOverlayTimeStatusRenderer",
                    JSONObject().put("text", JSONObject().put("simpleText", "5:00")))
            ))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertEquals(300, result!!.durationSecs)
    }

    @Test
    fun extractVideoRenderers_findsVideoRenderer() {
        val json = JSONObject().apply {
            put("contents", JSONObject().put("sectionListRenderer",
                JSONObject().put("contents", JSONArray().put(
                    JSONObject().put("itemSectionRenderer",
                        JSONObject().put("contents", JSONArray().put(
                            JSONObject().put("videoRenderer",
                                JSONObject().put("videoId", "found1").put("title",
                                    JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Found")))))
                        )))
                ))))
        }
        val renderers = extractVideoRenderers(json)
        assertEquals(1, renderers.size)
        assertEquals("found1", renderers[0].getString("videoId"))
    }

    @Test
    fun extractVideoRenderers_findsCompactVideoRenderer() {
        val json = JSONObject().apply {
            put("compactVideoRenderer", JSONObject().put("videoId", "compact1"))
        }
        val renderers = extractVideoRenderers(json)
        assertEquals(1, renderers.size)
        assertEquals("compact1", renderers[0].getString("videoId"))
    }

    @Test
    fun extractVideoRenderers_findsMultipleTypes() {
        val json = JSONObject().apply {
            put("items", JSONArray()
                .put(JSONObject().put("videoRenderer", JSONObject().put("videoId", "v1")))
                .put(JSONObject().put("compactVideoRenderer", JSONObject().put("videoId", "v2")))
                .put(JSONObject().put("gridVideoRenderer", JSONObject().put("videoId", "v3")))
            )
        }
        val renderers = extractVideoRenderers(json)
        assertEquals(3, renderers.size)
    }

    @Test
    fun extractVideoRenderers_doesNotEarlyReturn() {
        // Old bug: extractVideoRenderers returned after first match
        val json = JSONObject().apply {
            put("a", JSONObject().put("videoRenderer", JSONObject().put("videoId", "first")))
            put("b", JSONObject().put("videoRenderer", JSONObject().put("videoId", "second")))
        }
        val renderers = extractVideoRenderers(json)
        assertEquals(2, renderers.size)
    }

    @Test
    fun parseInnertubeRenderer_titleCleaning() {
        val vr = JSONObject().apply {
            put("videoId", "clean1")
            put("title", JSONObject().put("runs", JSONArray().put(
                JSONObject().put("text", "Drake - Hotline Bling (Official Audio)"))))
            put("ownerText", JSONObject().put("runs", JSONArray().put(
                JSONObject().put("text", "DrakeVEVO"))))
            put("lengthText", JSONObject().put("simpleText", "4:27"))
        }
        val result = parseInnertubeRenderer(vr)
        assertNotNull(result)
        assertEquals("Hotline Bling", result!!.title.trim())
        assertEquals("Drake", result.author.trim())
    }
}
