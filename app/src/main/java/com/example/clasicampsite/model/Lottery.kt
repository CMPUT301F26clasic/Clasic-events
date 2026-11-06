/*
Purpose: A campsite lottery, saved in the Firestore "lotteries" collection. Stores which
campsites are in it, who won, and which dates winners can book.
Outstanding issues: Creating lotteries and drawing winners are done by other parts of the app.
*/
package com.example.clasicampsite.model

/**
 * A campsite lottery.
 *
 * @property id Firestore document ID
 * @property title Name of the lottery
 * @property campsiteIds The campsites in this lottery (can be more than one)
 * @property winners Device IDs of the entrants who won
 * @property availableDates Dates winners can book, written like "2027-05-07"
 */
data class Lottery(
    var id: String = "",
    var title: String = "",
    var campsiteIds: List<String> = emptyList(),
    var winners: List<String> = emptyList(),
    var availableDates: List<String> = emptyList(),
) {
    /** Returns true if [deviceId] won this lottery */
    fun isWinner(deviceId: String): Boolean = deviceId in winners

    /** Returns true if this lottery has more than one campsite to choose from */
    fun hasMultipleCampsites(): Boolean = campsiteIds.size > 1

    /**
     * Returns the dates that aren't in [bookedDates] and aren't before [today], earliest first
     * (US 01.04.01)
     */
    fun freeDates(today: String, bookedDates: List<String>): List<String> =
        availableDates.filter { it !in bookedDates && it >= today }.sorted()
}
