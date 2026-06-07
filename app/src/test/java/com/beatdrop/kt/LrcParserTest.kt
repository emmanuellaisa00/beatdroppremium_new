package com.beatdrop.kt

import com.beatdrop.kt.lyrics.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parsesBasicTimestamps() {
        val lrc = """
            [00:01.00]First line
            [00:03.50]Second line
            [01:00.00]Minute line
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(3, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals(3500L, lines[1].timeMs)
        assertEquals(60_000L, lines[2].timeMs)
        assertEquals("First line", lines[0].text)
    }

    @Test
    fun handlesMultipleTimestampsPerLine() {
        val lrc = "[00:01.00][00:05.00]Repeated chorus"
        val lines = LrcParser.parse(lrc)
        assertEquals(2, lines.size)
        assertEquals("Repeated chorus", lines[0].text)
        assertEquals("Repeated chorus", lines[1].text)
        // Sorted ascending
        assertTrue(lines[0].timeMs < lines[1].timeMs)
    }

    @Test
    fun ignoresNonTimestampLines() {
        val lrc = """
            [ar:Some Artist]
            [ti:Some Title]
            [00:02.00]Only this counts
            plain text with no tag
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(1, lines.size)
        assertEquals("Only this counts", lines[0].text)
    }

    @Test
    fun activeIndexTracksPosition() {
        val lines = LrcParser.parse("[00:00.00]A\n[00:05.00]B\n[00:10.00]C")
        assertEquals(-1, LrcParser.activeIndex(lines, -1))
        assertEquals(0, LrcParser.activeIndex(lines, 0))
        assertEquals(0, LrcParser.activeIndex(lines, 4999))
        assertEquals(1, LrcParser.activeIndex(lines, 5000))
        assertEquals(2, LrcParser.activeIndex(lines, 999_999))
    }

    @Test
    fun emptyContentYieldsNoLines() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n  \n").isEmpty())
    }
}
