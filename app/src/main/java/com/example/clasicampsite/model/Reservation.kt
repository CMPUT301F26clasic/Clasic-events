/**
 * Purpose: A reservation a lottery winner makes, saved in the Firestore "reservations" collection.
 * Outstanding issues: None.
 */
package com.example.clasicampsite.model

/**
 * A campsite reservation.
 *
 * @property id Firestore document ID
 * @property lotteryId The lottery the entrant won
 * @property campsiteId The campsite that was reserved
 * @property campsiteName Name of the campsite, saved so the confirmation can show it
 * @property userId Device ID of the entrant who reserved
 * @property dates The nights reserved, written like "2027-05-07"
 * @property status "active", "cancelled" or "rejected"
 */
data class Reservation(
    var id: String = "",
    var lotteryId: String = "",
    var campsiteId: String = "",
    var campsiteName: String = "",
    var userId: String = "",
    var dates: List<String> = emptyList(),
    var status: String = "active",
) {
    /**
     * Returns the number of nights reserved.
     *
     * @return how many dates are in this reservation
     */
    fun nights(): Int = dates.size

    /**
     * Returns the last night of this reservation.
     *
     * @return the latest date, or "" if there are no dates
     */
    fun lastNight(): String = dates.maxOrNull() ?: ""

    /**
     * Returns true if this reservation is still coming up: it is active and its last night is
     * on or after [today] (US 01.06.01).
     *
     * @param today today's date, written like "2027-05-07"
     * @return true if the reservation is active and not over yet
     */
    fun isUpcoming(today: String): Boolean = status == "active" && lastNight() >= today

    /**
     * Returns true if this reservation can still be cancelled: it is active and hasn't ended
     * yet (US 01.07.01).
     *
     * @param today today's date, written like "2027-05-07"
     * @return true if the Cancel button should be shown
     */
    fun canCancel(today: String): Boolean = isUpcoming(today)

    /**
     * Returns the message sent to the user after they cancel this reservation (US 01.07.02).
     *
     * @return a sentence naming the campsite and the cancelled dates
     */
    fun cancellationMessage(): String =
        "Your reservation at $campsiteName for ${dates.sorted().joinToString(", ")} was cancelled."

    /**
     * Returns true if the campsite owner can still reject this reservation: it is active and
     * hasn't ended yet (US 02.08.01).
     *
     * @param today today's date, written like "2027-05-07"
     * @return true if the Reject button should be shown to the owner
     */
    fun canReject(today: String): Boolean = isUpcoming(today)

    /**
     * Returns true if this reservation was called off, either cancelled by the camper
     * (US 01.07.01) or rejected by the owner (US 02.08.01).
     *
     * @return true if the status is "cancelled" or "rejected"
     */
    fun isCancelledOrRejected(): Boolean = status == "cancelled" || status == "rejected"

    /**
     * Returns the message sent to the camper when the owner rejects this reservation
     * (US 02.08.01).
     *
     * @param reason the owner's reason, or "" if they didn't give one
     * @return a sentence naming the campsite and dates, plus the reason if there is one
     */
    fun rejectionMessage(reason: String): String {
        val message = "Your reservation at $campsiteName for ${dates.sorted().joinToString(", ")} " +
            "was rejected by the campsite owner."
        return if (reason.isBlank()) message else "$message Reason: ${reason.trim()}"
    }

    /**
     * Returns this reservation as CSV text with a header row (US 01.05.02).
     *
     * @return the CSV text: a header line, then one line for this reservation
     */
    fun toCsv(): String {
        val header = "Reservation ID,Campsite,Dates,Nights,Status"
        val row = "$id,\"$campsiteName\",${dates.sorted().joinToString(" ")},${nights()},$status"
        return header + "\n" + row + "\n"
    }

    /** Helper functions that work on a list of reservations. */
    companion object {
        /**
         * Checks if the user already has an active reservation from a lottery, so a winner
         * only books once per lottery.
         *
         * @param reservations the user's reservations
         * @param lotteryId the lottery to check
         * @return true if one of them is active and came from that lottery
         */
        fun hasActiveReservationFor(reservations: List<Reservation>, lotteryId: String): Boolean =
            reservations.any { it.lotteryId == lotteryId && it.status == "active" }

        /**
         * Returns true if a winner is allowed to reserve: they picked at least one night and
         * ticked the box saying they read the campsite rules and policies (US 02.11.01).
         *
         * @param selectedDates the nights the winner picked
         * @param acceptedRules true if the rules checkbox is ticked
         * @return true if the Reserve button should be enabled
         */
        fun canReserve(selectedDates: List<String>, acceptedRules: Boolean): Boolean =
            selectedDates.isNotEmpty() && acceptedRules

        /**
         * Returns every date taken by an active reservation.
         *
         * @param reservations the reservations for one campsite
         * @return the booked dates, so they can be hidden from other winners
         */
        fun bookedDates(reservations: List<Reservation>): List<String> =
            reservations.filter { it.status == "active" }.flatMap { it.dates }

        /**
         * Returns the reservations that are still coming up, soonest first (US 01.06.01).
         *
         * @param reservations all of a user's reservations
         * @param today today's date, written like "2027-05-07"
         * @return the active reservations, sorted by first night
         */
        fun activeReservations(reservations: List<Reservation>, today: String): List<Reservation> =
            reservations.filter { it.isUpcoming(today) }.sortedBy { it.dates.minOrNull() ?: "" }

        /**
         * Returns the reservations that are over, cancelled or rejected, most recent first
         * (US 01.06.02).
         *
         * @param reservations all of a user's reservations
         * @param today today's date, written like "2027-05-07"
         * @return the past reservations, sorted by last night, newest first
         */
        fun pastReservations(reservations: List<Reservation>, today: String): List<Reservation> =
            reservations.filter { !it.isUpcoming(today) }.sortedByDescending { it.lastNight() }
    }
}
