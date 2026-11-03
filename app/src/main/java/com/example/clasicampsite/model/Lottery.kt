package com.example.clasicampsite.model

data class Lottery(
    var id: String = "",
    var title: String = "",
    var campsiteId: String = "",
    var winners: List<String> = emptyList(),
    var availableDates: List<String> = emptyList(),
    var bookedDates: List<String> = emptyList(),
) {
    fun isWinner(deviceId: String): Boolean = deviceId in winners

    fun freeDates(today: String): List<String> =
        availableDates.filter { it !in bookedDates && it >= today }.sorted()
}