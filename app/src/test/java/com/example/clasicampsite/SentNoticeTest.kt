/*
 * Purpose: Unit tests for SentNotice: grouping single notifications into sends, and the
 * text shown on the Sent Notifications screen (US 02.10.02).
 * Outstanding issues: None known.
 * Parts of this file were written with help from Claude (Anthropic), 2026-11-27
 */
package com.example.clasicampsite

import com.example.clasicampsite.model.SentNotice
import com.example.clasicampsite.model.SentNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** Tests SentNotice. */
class SentNoticeTest {

    private val message = "Pigeon Lake Retreat: Gates open at 3 pm on Friday."

    private fun sent(userId: String, sentAt: Long, text: String = message, campsiteId: String = "camp-1") =
        SentNotification(userId = userId, message = text, sentAt = sentAt, campsiteId = campsiteId)

    @Test
    fun oneSendToManyEntrantsIsOneNotice() {
        val notices = SentNotice.group(listOf(sent("phone-a", 1000), sent("phone-b", 1000), sent("phone-c", 1000)))
        assertEquals(1, notices.size)
        assertEquals(3, notices[0].recipientCount)
        assertEquals(message, notices[0].message)
        assertEquals("camp-1", notices[0].campsiteId)
    }

    @Test
    fun separateSendsStaySeparateNewestFirst() {
        val notices = SentNotice.group(
            listOf(sent("phone-a", 1000), sent("phone-a", 5000, "Pigeon Lake Retreat: Quiet hours start at 10 pm."))
        )
        assertEquals(2, notices.size)
        assertEquals(5000L, notices[0].sentAt)
        assertEquals(1000L, notices[1].sentAt)
    }

    @Test
    fun sameMessageForDifferentCampsitesStaysSeparate() {
        val notices = SentNotice.group(listOf(sent("phone-a", 1000), sent("phone-b", 1000, campsiteId = "camp-2")))
        assertEquals(2, notices.size)
    }

    @Test
    fun repeatedEntrantIsCountedOnce() {
        val notices = SentNotice.group(listOf(sent("phone-a", 1000), sent("phone-a", 1000)))
        assertEquals(1, notices[0].recipientCount)
    }

    @Test
    fun nothingSentGivesEmptyList() {
        assertTrue(SentNotice.group(emptyList()).isEmpty())
    }

    @Test
    fun recipientsTextWording() {
        assertEquals("Sent to 1 entrant", SentNotice(recipientCount = 1).recipientsText())
        assertEquals("Sent to 4 entrants", SentNotice(recipientCount = 4).recipientsText())
    }

    @Test
    fun timeTextIsReadable() {
        val edmonton = ZoneId.of("America/Edmonton")
        val sentAt = ZonedDateTime.of(2026, 11, 27, 14, 15, 0, 0, edmonton).toInstant().toEpochMilli()
        assertEquals("Nov 27, 2:15 PM", SentNotice(sentAt = sentAt).timeText(edmonton))
    }
}
