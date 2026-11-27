/*
 * Purpose: Unit tests for WaitlistNotice: checking the owner's message, the text entrants
 * see, and who gets it (US 02.10.01).
 * Outstanding issues: None known.
 */
package com.example.clasicampsite

import com.example.clasicampsite.model.WaitlistNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests WaitlistNotice. */
class WaitlistNoticeTest {

    private val notice = WaitlistNotice(
        campsiteName = "Pigeon Lake Retreat",
        message = "Gates open at 3 pm on Friday.",
    )

    @Test
    fun validNoticeHasNoError() {
        assertNull(notice.errorMessage(waitlistSize = 3))
    }

    @Test
    fun blankMessageIsAnError() {
        assertEquals("Type a message first", notice.copy(message = "   ").errorMessage(3))
    }

    @Test
    fun messageLengthIsLimited() {
        assertNull(notice.copy(message = "a".repeat(300)).errorMessage(3))
        assertEquals(
            "Message must be 300 characters or less",
            notice.copy(message = "a".repeat(301)).errorMessage(3),
        )
    }

    @Test
    fun emptyWaitlistIsAnError() {
        assertEquals("Nobody is on the waitlist yet", notice.errorMessage(0))
    }

    @Test
    fun fullTextStartsWithCampsiteName() {
        assertEquals(
            "Pigeon Lake Retreat: Gates open at 3 pm on Friday.",
            notice.copy(message = "  Gates open at 3 pm on Friday. ").fullText(),
        )
        assertEquals("Hello", WaitlistNotice(message = "Hello").fullText())
    }

    @Test
    fun recipientsSkipOptedOutBlanksAndRepeats() {
        val waitlist = listOf("phone-a", "phone-b", "phone-a", "", "phone-c")
        assertEquals(listOf("phone-a", "phone-c"), WaitlistNotice.recipients(waitlist, setOf("phone-b")))
    }

    @Test
    fun sentSummaryWording() {
        assertEquals("Sent to 1 entrant", WaitlistNotice.sentSummary(1, 0))
        assertEquals("Sent to 4 entrants (2 opted out)", WaitlistNotice.sentSummary(4, 2))
        assertEquals(
            "Everyone on the waitlist has turned notifications off",
            WaitlistNotice.sentSummary(0, 3),
        )
    }
}
