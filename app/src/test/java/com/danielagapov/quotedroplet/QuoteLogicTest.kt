package com.danielagapov.quotedroplet

import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.data.QuoteFrequency
import com.danielagapov.quotedroplet.data.SubmissionStatus
import com.danielagapov.quotedroplet.data.highlightRanges
import com.danielagapov.quotedroplet.data.isAuthorValid
import com.danielagapov.quotedroplet.data.parseApiDate
import com.danielagapov.quotedroplet.data.quoteWithAuthor
import com.danielagapov.quotedroplet.notifications.QuoteNotifications
import com.danielagapov.quotedroplet.ui.theme.colorFromHex
import com.danielagapov.quotedroplet.ui.theme.paletteFor
import com.danielagapov.quotedroplet.ui.theme.presetPalettes
import com.danielagapov.quotedroplet.ui.theme.toHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class QuoteLogicTest {

    @Test
    fun authorPlaceholdersAreHidden() {
        assertFalse(isAuthorValid(null))
        assertFalse(isAuthorValid(""))
        assertFalse(isAuthorValid("   "))
        assertFalse(isAuthorValid("NULL"))
        assertFalse(isAuthorValid("Unknown Author"))
        assertTrue(isAuthorValid("Seneca"))
    }

    @Test
    fun parsesBackendTimestampsWithAndWithoutFractions() {
        assertEquals(Instant.parse("2026-06-27T07:34:54.931084Z"), parseApiDate("2026-06-27T07:34:54.931084Z"))
        assertEquals(Instant.parse("2026-11-02T12:00:00Z"), parseApiDate("2026-11-02T12:00:00Z"))
        assertNotNull(parseApiDate("2026-11-02T12:00:00+00:00"))
        assertNull(parseApiDate(null))
        assertNull(parseApiDate("not a date"))
    }

    @Test
    fun highlightsEveryMatchIgnoringCase() {
        // "The cat. the dog": matches at 0 and 9
        assertEquals(listOf(0..2, 9..11), highlightRanges("The cat. the dog", "the"))
        assertEquals(listOf(4..6), highlightRanges("Be  BRAVE", " bra "))
        assertTrue(highlightRanges("anything", "  ").isEmpty())
        assertTrue(highlightRanges("abc", "xyz").isEmpty())
    }

    @Test
    fun copiedTextPutsTheAuthorOnItsOwnLine() {
        assertEquals("Be brave.\n— Seneca", quoteWithAuthor(Quote(1, "Be brave.", "Seneca")))
        assertEquals("Be brave.", quoteWithAuthor(Quote(1, "Be brave.", "NULL")))
    }

    @Test
    fun categoriesMapToBackendFilters() {
        assertNull(QuoteCategory.All.apiFilter)
        assertEquals("wisdom", QuoteCategory.Wisdom.apiFilter)
        assertEquals(QuoteCategory.All, QuoteCategory.fromKey("nonsense"))
        assertEquals(QuoteCategory.Love, QuoteCategory.fromKey("Love"))
        assertFalse(QuoteCategory.remote.contains(QuoteCategory.Saved))
    }

    @Test
    fun frequenciesReadLikeIos() {
        assertEquals("Every 8 hours", QuoteFrequency.EightHours.displayName)
        assertEquals("Every day", QuoteFrequency.OneDay.displayName)
        assertEquals("Every week", QuoteFrequency.OneWeek.displayName)
        assertEquals(QuoteFrequency.OneDay, QuoteFrequency.fromKey(null))
    }

    @Test
    fun submissionStatusDefaultsToPending() {
        assertEquals(SubmissionStatus.Approved, SubmissionStatus.from("approved"))
        assertEquals(SubmissionStatus.Rejected, SubmissionStatus.from("REJECTED"))
        assertEquals(SubmissionStatus.Pending, SubmissionStatus.from("something else"))
    }

    @Test
    fun nextNotificationIsTodayOrTomorrow() {
        val zone = ZoneId.of("America/Vancouver")
        val morning = ZonedDateTime.of(2026, 11, 18, 8, 0, 0, 0, zone)
        val today9 = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val tomorrow7 = ZonedDateTime.of(2026, 11, 19, 7, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(today9, QuoteNotifications.nextTriggerMillis(9, 0, morning))
        assertEquals(tomorrow7, QuoteNotifications.nextTriggerMillis(7, 0, morning))
        // Exactly now counts as passed
        val tomorrow8 = ZonedDateTime.of(2026, 11, 19, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(tomorrow8, QuoteNotifications.nextTriggerMillis(8, 0, morning))
    }

    @Test
    fun palettesRoundTripThroughHex() {
        assertEquals("#3A2C24", presetPalettes[0].background.toHex())
        assertEquals("#1C7C54", colorFromHex("1C7C54").toHex())
        assertEquals("#ABCDEF", colorFromHex("#abcdef").toHex())
        val custom = listOf(colorFromHex("#111111"), colorFromHex("#222222"), colorFromHex("#333333"))
        assertEquals("#222222", paletteFor(3, custom).text.toHex())
        assertEquals(presetPalettes[0], paletteFor(99, custom))
        assertTrue(presetPalettes[0].isDark)
        assertFalse(presetPalettes[2].isDark)
    }
}
