/**
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

    /**
     * Makes one saved notification for the tests.
     *
     * @param userId who it was sent to
     * @param sentAt when it was sent, in milliseconds
     * @param text the notification text
     * @param campsiteId the campsite it was sent about
     * @return the saved notification
     */
    private fun sent(userId: String, sentAt: Long, text: String = message, campsiteId: String = "camp-1") =
        SentNotification(userId = userId, message = text, sentAt = sentAt, campsiteId = campsiteId)

    /** US 02.10.02: one send to many entrants shows as one sent notification */
    @Test
    fun oneSendToManyEntrantsIsOneNotice() {
        val notices = SentNotice.group(listOf(sent("phone-a", 1000), sent("phone-b", 1000), sent("phone-c", 1000)))
        assertEquals(1, notices.size)
        assertEquals(3, notices[0].recipientCount)
        assertEquals(message, notices[0].message)
        assertEquals("camp-1", notices[0].campsiteId)
    }

    /** US 02.10.02: separate sends stay separate, newest first */
    @Test
    fun separateSendsStaySeparateNewestFirst() {
        val notices = SentNotice.group(
            listOf(sent("phone-a", 1000), sent("phone-a", 5000, "Pigeon Lake Retreat: Quiet hours start at 10 pm."))
        )
        assertEquals(2, notices.size)
        assertEquals(5000L, notices[0].sentAt)
        assertEquals(1000L, notices[1].sentAt)
    }

    /** US 02.10.02: the same message sent for two campsites shows twice */
    @Test
    fun sameMessageForDifferentCampsitesStaysSeparate() {
        val notices = SentNotice.group(listOf(sent("phone-a", 1000), sent("phone-b", 1000, campsiteId = "camp-2")))
        assertEquals(2, notices.size)
    }

    /** US 02.10.02: an entrant who got it twice is counted once */
    @Test
    fun repeatedEntrantIsCountedOnce() {
        val notices = SentNotice.group(listOf(sent("phone-a", 1000), sent("phone-a", 1000)))
        assertEquals(1, notices[0].recipientCount)
    }

    /** US 02.10.02: with nothing sent, the list is empty */
    @Test
    fun nothingSentGivesEmptyList() {
        assertTrue(SentNotice.group(emptyList()).isEmpty())
    }

    /** US 02.10.02: the recipient count reads entrant or entrants correctly */
    @Test
    fun recipientsTextWording() {
        assertEquals("Sent to 1 entrant", SentNotice(recipientCount = 1).recipientsText())
        assertEquals("Sent to 4 entrants", SentNotice(recipientCount = 4).recipientsText())
    }

    /** US 02.10.02: the send time is shown in a readable way */
    @Test
    fun timeTextIsReadable() {
        val edmonton = ZoneId.of("America/Edmonton")
        val sentAt = ZonedDateTime.of(2026, 11, 27, 14, 15, 0, 0, edmonton).toInstant().toEpochMilli()
        assertEquals("Nov 27, 2:15 PM", SentNotice(sentAt = sentAt).timeText(edmonton))
    }
}
