/**
 * Purpose: A campsite lottery, saved in the Firestore "lotteries" collection. Stores which
 * campsites are in it, who won, who declined, and which dates winners can book.
 * Owners make one for a single campsite by drawing winners (US 02.05.01), or one for several
 * campsites with the shared lottery screen (US 02.04.08).
 * Outstanding issues: A campsite's lottery can only be drawn once.
 */
package com.example.clasicampsite.model

import java.time.LocalDate
import kotlin.random.Random

/**
 * A campsite lottery.
 *
 * @property id Firestore document ID
 * @property title Name of the lottery
 * @property campsiteIds The campsites in this lottery (can be more than one)
 * @property winners Device IDs of the entrants who won
 * @property declined Device IDs of winners who declined
 * @property availableDates Dates winners can book, written like "2027-05-07"
 */
data class Lottery(
    var id: String = "",
    var title: String = "",
    var campsiteIds: List<String> = emptyList(),
    var winners: List<String> = emptyList(),
    var declined: List<String> = emptyList(),
    var availableDates: List<String> = emptyList(),
) {
    /** Returns true if [deviceId] won this lottery and hasn't declined */
    fun isWinner(deviceId: String): Boolean = deviceId in winners && deviceId !in declined

    /** Returns true if [deviceId] declined this lottery */
    fun hasDeclined(deviceId: String): Boolean = deviceId in declined

    /** Returns true if this lottery has more than one campsite to choose from */
    fun hasMultipleCampsites(): Boolean = campsiteIds.size > 1

    /**
     * Returns the dates that aren't in [bookedDates] and aren't before [today], earliest first
     * (US 01.04.01)
     */
    fun freeDates(today: String, bookedDates: List<String>): List<String> =
        availableDates.filter { it !in bookedDates && it >= today }.sorted()

    /** Returns true once winners have been drawn for this lottery, so it can't be drawn again */
    fun hasBeenDrawn(): Boolean = winners.isNotEmpty()

    companion object {
        /**
         * Picks winners at random for a draw (US 02.05.01). Blank IDs and people who entered
         * twice are only counted once.
         *
         * @param entrantIds device IDs of everyone in the lottery
         * @param count how many winners the owner wants
         * @param random where the randomness comes from, so tests can use a fixed seed
         * @return up to [count] different winners
         */
        fun pickWinners(entrantIds: List<String>, count: Int, random: Random = Random): List<String> =
            entrantIds.filter { it.isNotBlank() }.distinct().shuffled(random).take(count.coerceAtLeast(0))

        /**
         * Lists every night from [from] to [to], leaving out nights before [today].
         *
         * @param from first available night
         * @param to last available night
         * @param today nights before this can't be booked any more
         * @return the nights written like "2027-05-07", earliest first
         */
        fun datesBetween(from: LocalDate, to: LocalDate, today: LocalDate): List<String> {
            val dates = mutableListOf<String>()
            var date = if (from.isBefore(today)) today else from
            while (!date.isAfter(to)) {
                dates.add(date.toString())
                date = date.plusDays(1)
            }
            return dates
        }

        /**
         * Builds the lottery saved when an owner draws winners for one campsite. It uses the
         * campsite's ID, so each campsite has one lottery and it can't be drawn twice.
         *
         * @param campsite the campsite the winners were drawn for
         * @param winners the device IDs that won
         * @param today nights before this are left out of the dates winners can book
         * @return the lottery to save
         */
        fun forCampsite(campsite: Campsite, winners: List<String>, today: LocalDate): Lottery =
            Lottery(
                id = campsite.id,
                title = campsite.name,
                campsiteIds = listOf(campsite.id),
                winners = winners,
                availableDates = datesBetween(campsite.availableFrom, campsite.availableTo, today),
            )
    }
}
