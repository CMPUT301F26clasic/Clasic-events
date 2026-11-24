/*
Purpose: A reservation a lottery winner makes, saved in the Firestore "reservations" collection.
Outstanding issues: Cancelling and rejecting reservations are not done yet.
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
    /** Returns the number of nights reserved */
    fun nights(): Int = dates.size

    /** Returns the last night of this reservation, or "" if it has no dates */
    fun lastNight(): String = dates.maxOrNull() ?: ""

    /**
     * Returns true if this reservation is still coming up: it is active and its last night is
     * on or after [today] (US 01.06.01). [today] is written like "2027-05-07".
     */
    fun isUpcoming(today: String): Boolean = status == "active" && lastNight() >= today

    /** Returns this reservation as CSV text with a header row (US 01.05.02) */
    fun toCsv(): String {
        val header = "Reservation ID,Campsite,Dates,Nights,Status"
        val row = "$id,\"$campsiteName\",${dates.sorted().joinToString(" ")},${nights()},$status"
        return header + "\n" + row + "\n"
    }

    companion object {
        /** Returns every date taken by an active reservation in [reservations] */
        fun bookedDates(reservations: List<Reservation>): List<String> =
            reservations.filter { it.status == "active" }.flatMap { it.dates }

        /** Returns the reservations in [reservations] that are still coming up, soonest first (US 01.06.01) */
        fun activeReservations(reservations: List<Reservation>, today: String): List<Reservation> =
            reservations.filter { it.isUpcoming(today) }.sortedBy { it.dates.minOrNull() ?: "" }

        /**
         * Returns the reservations in [reservations] that are over, cancelled or rejected, most
         * recent first (US 01.06.02)
         */
        fun pastReservations(reservations: List<Reservation>, today: String): List<Reservation> =
            reservations.filter { !it.isUpcoming(today) }.sortedByDescending { it.lastNight() }
    }
}
