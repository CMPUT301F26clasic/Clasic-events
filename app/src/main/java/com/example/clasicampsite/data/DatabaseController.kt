/*
Purpose: Saves, loads and deletes the app's data in Firebase Firestore. Each part of the app
adds the functions for its own data here.
Outstanding issues: Booking checks that dates are still free right before saving, but two
people booking at the exact same moment could still both succeed (needs a transaction).
 */
package com.example.clasicampsite.data

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.Lottery
import com.example.clasicampsite.model.Reservation
import com.example.clasicampsite.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import com.example.clasicampsite.model.LotteryEntrant

/** Reads and writes app data in Firestore. */
class DatabaseController {

    private val users = FirebaseFirestore.getInstance().collection("users")
    private val campsites = FirebaseFirestore.getInstance().collection("campsites")
    private val lotteries = FirebaseFirestore.getInstance().collection("lotteries")
    private val reservations = FirebaseFirestore.getInstance().collection("reservations")
    private val notifications = FirebaseFirestore.getInstance().collection("notifications")

    private val lotteryEntrants = FirebaseFirestore.getInstance().collection("lottery_entrants")

    /** Returns user saved under [deviceId], or a new empty user if none is saved yet */
    suspend fun getUser(deviceId: String): User {
        val document = users.document(deviceId).get().await()
        return document.toObject(User::class.java) ?: User(id = deviceId)
    }

    /** Returns every saved user */
    suspend fun getAllUsers(): List<User> {
        val snapshot = users.get().await()
        return snapshot.toObjects(User::class.java)
    }

    /** Saves [user], replacing what was saved before */
    suspend fun saveUser(user: User) {
        users.document(user.id).set(user).await()
    }

    /** Deletes the user saved under [deviceId] */
    suspend fun deleteUser(deviceId: String) {
        users.document(deviceId).delete().await()
    }

    /** Fetches a single campsite by its [campsiteId] from Firestore */
    suspend fun getCampsite(campsiteId: String): Campsite? {
        val document = campsites.document(campsiteId).get().await()

        if (!document.exists()) return null

        return Campsite(
            id = document.id,
            name = document.getString("name") ?: "",
            location = document.getString("location") ?: "",
            maxOccupancy = document.getLong("maxOccupancy")?.toInt() ?: 0,
            rating = document.getDouble("rating") ?: 0.0,
            availableFrom = LocalDate.parse(
                document.getString("availableFrom")
            ),
            availableTo = LocalDate.parse(
                document.getString("availableTo")
            )
        )
    }

    /** Fetches all campsites from the "campsites" collection */
    suspend fun getCampsites(): List<Campsite> {
        val snapshot = campsites.get().await()

        return snapshot.documents.mapNotNull { document ->
            try {
                Campsite(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    location = document.getString("location") ?: "",
                    maxOccupancy = document.getLong("maxOccupancy")?.toInt() ?: 0,
                    rating = document.getDouble("rating") ?: 0.0,
                    availableFrom = LocalDate.parse(
                        document.getString("availableFrom")
                    ),
                    availableTo = LocalDate.parse(
                        document.getString("availableTo")
                    )
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Saves or updates a campsite in Firestore */
    suspend fun saveCampsite(campsite: Campsite) {

        val document = if (campsite.id.isBlank()) {
            campsites.document()
        } else {
            campsites.document(campsite.id)
        }

        campsite.id = document.id

        val campsiteData = hashMapOf(
            "id" to campsite.id,
            "name" to campsite.name,
            "location" to campsite.location,
            "maxOccupancy" to campsite.maxOccupancy,
            "rating" to campsite.rating,
            "availableFrom" to campsite.availableFrom.toString(),
            "availableTo" to campsite.availableTo.toString()
        )

        document.set(campsiteData).await()
    }

    /** Fetches the lottery with [lotteryId], or null if it doesn't exist */
    suspend fun getLottery(lotteryId: String): Lottery? {
        val document = lotteries.document(lotteryId).get().await()
        return document.toObject(Lottery::class.java)?.apply { id = document.id }
    }

    /** Saves or updates [lottery] */
    suspend fun saveLottery(lottery: Lottery) {
        val docRef = if (lottery.id.isBlank()) lotteries.document() else lotteries.document(lottery.id)
        lottery.id = docRef.id
        docRef.set(lottery).await()
    }

    /** Records that [deviceId] declined the lottery with [lotteryId] (US 01.04.03) */
    suspend fun declineLottery(lotteryId: String, deviceId: String) {
        lotteries.document(lotteryId).update("declined", FieldValue.arrayUnion(deviceId)).await()
    }

    /** Fetches every reservation for the campsite with [campsiteId] */
    suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> {
        val snapshot = reservations.whereEqualTo("campsiteId", campsiteId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Reservation::class.java)?.apply { id = doc.id }
        }
    }

    /** Fetches the reservation with [reservationId], or null if it doesn't exist */
    suspend fun getReservation(reservationId: String): Reservation? {
        val document = reservations.document(reservationId).get().await()
        return document.toObject(Reservation::class.java)?.apply { id = document.id }
    }

    /** Saves a new [reservation] and gives it an ID (US 01.04.02) */
    suspend fun saveReservation(reservation: Reservation) {
        val docRef = if (reservation.id.isBlank()) reservations.document() else reservations.document(reservation.id)
        reservation.id = docRef.id
        docRef.set(reservation).await()
    }

    /** Sends [message] to the user with [deviceId] as an in-app notification (US 01.05.01) */
    suspend fun sendNotification(deviceId: String, message: String) {
        val notification = hashMapOf(
            "userId" to deviceId,
            "message" to message,
            "sentAt" to System.currentTimeMillis(),
        )
        notifications.add(notification).await()
    }

    /** Fetches all entrants for a specific campsite lottery */
    suspend fun getLotteryEntrants(campsiteId: String): List<LotteryEntrant> {
        val snapshot = lotteryEntrants.whereEqualTo("campsiteId", campsiteId).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.toObject(LotteryEntrant::class.java)?.apply { id = doc.id }
        }
    }

    /** Saves a lottery entrant */
    suspend fun saveLotteryEntrant(entrant: LotteryEntrant) {
        val docRef = if (entrant.id.isBlank()) {
            lotteryEntrants.document()
        } else {
            lotteryEntrants.document(entrant.id)
        }

        entrant.id = docRef.id
        docRef.set(entrant).await()
    }

    /** Updates the number of winners for a campsite lottery */
    suspend fun updateNumberOfWinners(campsiteId: String, numberOfWinners: Int) {
        campsites.document(campsiteId).update("numberOfWinners", numberOfWinners).await()
    }

    /** Gets the campsite name and lottery winner count without loading date fields. */
    suspend fun getCampsiteLotteryDetails(campsiteId: String): Pair<String, Int>? {
        val document = campsites.document(campsiteId).get().await()

        if (!document.exists()) return null
        val name = document.getString("name") ?: ""
        val numberOfWinners = (document.getLong("numberOfWinners") ?: 0L).toInt()

        return Pair(name, numberOfWinners)
    }

}
