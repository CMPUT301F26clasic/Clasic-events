/**
 * Purpose: The message a campsite owner sends to everyone on their campsite's lottery
 * waitlist (US 02.10.01). Checks the message, builds the text entrants see, and picks
 * who should get it.
 * Outstanding issues: None known.
 */
package com.example.clasicampsite.model

/**
 * A notice from a campsite owner to their lottery waitlist.
 *
 * @property campsiteName Name of the campsite the notice is about
 * @property message What the owner typed
 */
data class WaitlistNotice(
    val campsiteName: String = "",
    val message: String = "",
) {

    /**
     * Checks the notice can be sent.
     * @param waitlistSize How many entrants are on the waitlist
     * @return the problem to show the owner, or null if it's ok to send
     */
    fun errorMessage(waitlistSize: Int): String? {
        if (message.isBlank()) return "Type a message first"
        if (message.trim().length > MAX_LENGTH) return "Message must be $MAX_LENGTH characters or less"
        if (waitlistSize < 1) return "Nobody is on the waitlist yet"
        return null
    }

    /** The text entrants see, e.g. "Pigeon Lake Retreat: Gates open at 3 pm." */
    fun fullText(): String =
        if (campsiteName.isBlank()) message.trim() else "${campsiteName.trim()}: ${message.trim()}"

    companion object {
        /** Longest message an owner can send. */
        const val MAX_LENGTH = 300

        /**
         * Picks who should get the notice: each waitlisted user once, minus anyone who
         * turned notifications off (US 01.10.01).
         * @param waitlistUserIds Device IDs of everyone on the waitlist
         * @param optedOutUserIds Device IDs of users who turned notifications off
         */
        fun recipients(waitlistUserIds: List<String>, optedOutUserIds: Set<String>): List<String> =
            waitlistUserIds.filter { it.isNotBlank() }.distinct().filter { it !in optedOutUserIds }

        /**
         * What to tell the owner after sending.
         * @param sent How many entrants got the notice
         * @param optedOut How many were skipped because they turned notifications off
         */
        fun sentSummary(sent: Int, optedOut: Int): String {
            if (sent == 0) return "Everyone on the waitlist has turned notifications off"
            val people = if (sent == 1) "1 entrant" else "$sent entrants"
            return if (optedOut > 0) "Sent to $people ($optedOut opted out)" else "Sent to $people"
        }
    }
}
