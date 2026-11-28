/*
Purpose: Saves, loads and deletes the app's data in Firebase Firestore. Each part of the app
adds the functions for its own data here.
Outstanding issues: Booking checks that dates are still free right before saving, but two
people booking at the exact same moment could still both succeed (needs a transaction).
 */
package com.example.clasicampsite.data

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.Lottery
import com.example.clasicampsite.model.LotteryEntrant
import com.example.clasicampsite.model.NotificationLog
import com.example.clasicampsite.model.Reservation
import com.example.clasicampsite.model.Review
import com.example.clasicampsite.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import com.google.firebase.firestore.SetOptions

private val DEFAULT_DATE = LocalDate.of(1970,  1, 1)

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

    suspend fun sendMessageToReserver(
        ownerId: String,
        campsiteId: String,
        recipientId: String,
        message: String
    ) {
        require(message.isNotBlank()) { "Message can't be empty" }

        val campsite = getCampsite(campsiteId)
        check(campsite != null && campsite.ownerId == ownerId) {
            "Only the campsite owner can send messages"
        }

        // Assumes Reservation stores the reserving user's device ID as "userId"
        val hasReservation = !reservations
            .whereEqualTo("campsiteId", campsiteId)
            .whereEqualTo("userId", recipientId)
            .get().await().isEmpty
        check(hasReservation) { "That user has no reservation for this campsite" }

        notifications.add(
            hashMapOf(
                "userId" to recipientId,
                "message" to message.trim(),
                "sentAt" to System.currentTimeMillis(),
                "senderId" to ownerId,
                "campsiteId" to campsiteId,
                "campsiteName" to campsite.name,
                "type" to "owner_message"
            )
        ).await()
    }
    suspend fun getNotifications(userId: String): List<Map<String, Any?>> =
        notifications.whereEqualTo("userId", userId).get().await()
            .documents.mapNotNull { it.data }
            .sortedByDescending { (it["sentAt"] as? Long) ?: 0L
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
        return if (document.exists()) document.toCampsite() else null
    }

    /** Fetches all campsites from the "campsites" collection */
    suspend fun getCampsites(): List<Campsite> {
        val snapshot = campsites.get().await()

        return snapshot.documents.mapNotNull { document ->
            try {
                document.toCampsite()
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Returns every campsite for the admin list. */
    suspend fun getAllCampsites(): List<Campsite> {
        val snapshot = campsites.get().await()
        return snapshot.documents.map { it.toCampsite() }
    }

    /** Saves or updates a campsite in Firestore */
    suspend fun saveCampsite(campsite: Campsite) {

        val document = if (campsite.id.isBlank()) {
            campsites.document()
        } else {
            campsites.document(campsite.id)
        }

        campsite.id = document.id

        val campsiteData = hashMapOf<String, Any?>(
            "id" to campsite.id,
            "ownerId" to campsite.ownerId,
            "name" to campsite.name,
            "location" to campsite.location,
            "description" to campsite.description,
            "maxOccupancy" to campsite.maxOccupancy,
            "pricePerNight" to campsite.pricePerNight,
            "tags" to campsite.tags,
            "rating" to campsite.rating,
            "numberOfWinners" to campsite.numberOfWinners,
            "availableFrom" to campsite.availableFrom.toString(),
            "availableTo" to campsite.availableTo.toString(),
            "lotteryDeadline" to campsite.lotteryDeadline?.toString()
        )


        document.set(campsiteData, SetOptions.merge()).await()
    }

    /** Deletes the campsite with [campsiteId] from Firestore */
    suspend fun deleteCampsite(campsiteId: String) {
        campsites.document(campsiteId).delete().await()
    }

    /** Builds a Campsite from a Firestore document, reading dates from "yyyy-MM-dd" text. */
    private fun DocumentSnapshot.toCampsite(): Campsite = Campsite(
        id = id,
        ownerId = getString("ownerId") ?: "",
        name = getString("name") ?: "",
        location = getString("location") ?: "",
        description = getString("description") ?: "",
        maxOccupancy = getLong("maxOccupancy")?.toInt() ?: 0,
        pricePerNight = getDouble("pricePerNight") ?: 0.0,
        tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        rating = getDouble("rating") ?: 0.0,
        numberOfWinners = getLong("numberOfWinners")?.toInt() ?: 0,
        availableFrom = runCatching { LocalDate.parse(getString("availableFrom")) }
            .getOrDefault(DEFAULT_DATE),
        availableTo = runCatching { LocalDate.parse(getString("availableTo")) }
            .getOrDefault(DEFAULT_DATE),
        lotteryDeadline = getString("lotteryDeadline")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        lotteryResolved = getBoolean("lotteryResolved") ?: false,
        winners = (get("winners") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
    )

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
            "type" to "system"
        )
        notifications.add(notification).await()
    }

    /** Fetches all entrants for a specific campsite lottery */
    suspend fun getLotteryEntrants(campsiteId: String): List<LotteryEntrant> {
        val snapshot = lotteryEntrants.whereEqualTo("campsiteId", campsiteId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(LotteryEntrant::class.java)?.apply { id = doc.id }
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
    private suspend fun isParticipant(campsiteId: String, userId: String): Boolean {
        if (isUserInLottery(campsiteId, userId)) return true
        return !reservations
            .whereEqualTo("campsiteId", campsiteId)
            .whereEqualTo("userId", userId)
            .get().await().isEmpty
    }
    suspend fun postReview(campsiteId: String, userId: String, userName: String, text: String) {
        require(text.isNotBlank()) { "Review can't be empty" }
        require(text.length <= 1000) { "Review is too long" }
        check(isParticipant(campsiteId, userId)) {
            "Only guests who entered or reserved this campsite can review it"
        }
        campsites.document(campsiteId).collection("reviews").document(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "text" to text.trim(),
                    "postedAt" to System.currentTimeMillis()
                )
            ).await()
    }
    suspend fun getReviews(campsiteId: String): List<Review> =
        campsites.document(campsiteId).collection("reviews").get().await()
            .documents.mapNotNull { it.toObject(Review::class.java) }
            .sortedByDescending { it.postedAt }
    suspend fun sendMessageToOwner(senderId: String, campsiteId: String, message: String) {
        require(message.isNotBlank()) { "Message can't be empty" }

        val campsite = getCampsite(campsiteId)
        check(campsite != null && campsite.ownerId.isNotBlank()) { "This campsite has no owner" }
        check(campsite.ownerId != senderId) { "You can't message yourself" }
        check(isParticipant(campsiteId, senderId)) {
            "Only guests who entered or reserved this campsite can message the owner"
        }

        notifications.add(
            hashMapOf(
                "userId" to campsite.ownerId,
                "message" to message.trim(),
                "sentAt" to System.currentTimeMillis(),
                "senderId" to senderId,
                "campsiteId" to campsiteId,
                "campsiteName" to campsite.name,
                "type" to "entrant_message"
            )
        ).await()
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

    /** Fetches the campsites registered by the owner with [ownerId] (US 02.03.01) */
    suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> {
        val snapshot = campsites.whereEqualTo("ownerId", ownerId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toCampsite()
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Returns this user's saved rating (1-5) for the campsite, or null if they haven't rated it */
    suspend fun getUserRating(campsiteId: String, userId: String): Int? {
        val doc = campsites.document(campsiteId)
            .collection("ratings").document(userId)
            .get().await()
        return doc.getLong("rating")?.toInt()
    }

    /** Saves the user's [rating], then updates the campsite's average. Returns the new average. */
    suspend fun rateCampsite(campsiteId: String, userId: String, rating: Int): Double {
        val campsiteRef = campsites.document(campsiteId)
        val ratingsRef = campsiteRef.collection("ratings")

        //saves the user rating just overriting it
        ratingsRef.document(userId)
            .set(mapOf("userId" to userId, "rating" to rating, "ratedAt" to FieldValue.serverTimestamp()))
            .await()

        //gets all the ratings and average  plus rounds to one decimal place
        val all = ratingsRef.get().await().documents.mapNotNull { it.getLong("rating") }
        val average = Math.round(all.average() * 10) / 10.0

        //saves new average
        campsiteRef.set(mapOf("rating" to average), SetOptions.merge()).await()
        return average
    }

    //checks if user is in lotter by searching through database from user id in the database and returns true if its there
    suspend fun isUserInLottery(campsiteId: String, userId: String): Boolean =
        campsites.document(campsiteId)
            .collection("lottery").document(userId)
            .get().await().exists()

    //joins lottery saving user id,stores time and users
    suspend fun joinLottery(campsiteId: String, userId: String) {
        val camp = getCampsite(campsiteId)
        val closed = camp != null && (camp.lotteryResolved ||
                (camp.lotteryDeadline?.let { LocalDate.now().isAfter(it) } ?: false))
        if (closed) throw IllegalStateException("Registration is closed")

        campsites.document(campsiteId)
            .collection("lottery").document(userId)
            .set(mapOf("userId" to userId, "joinedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    //leaves lottery by deleting users id
    suspend fun leaveLottery(campsiteId: String, userId: String) {
        campsites.document(campsiteId)
            .collection("lottery").document(userId)
            .delete().await()
    }

    private val db = FirebaseFirestore.getInstance()

    /** Entrant user IDs from the same place joinLottery writes to */
    suspend fun getLotteryEntrantIds(campsiteId: String): List<String> =
        campsites.document(campsiteId).collection("lottery").get().await()
            .documents.map { it.id }

    /** Lets the owner set the last day to join */
    suspend fun updateLotteryDeadline(campsiteId: String, deadline: LocalDate) {
        campsites.document(campsiteId).update("lotteryDeadline", deadline.toString()).await()
    }
    suspend fun getAllNotificationLogs(): List<NotificationLog> =
        notifications.get().await().documents.map { doc ->
            NotificationLog(
                id = doc.id,
                recipientId = doc.getString("userId") ?: "",
                senderId = doc.getString("senderId") ?: "",
                message = doc.getString("message") ?: "",
                sentAt = doc.getLong("sentAt") ?: 0L,
                campsiteId = doc.getString("campsiteId") ?: "",
                campsiteName = doc.getString("campsiteName") ?: "",
                type = doc.getString("type") ?: "system"
            )
        }.sortedByDescending { it.sentAt }

    /**
     * Draws winners if the deadline has passed and no draw has happened yet.
     * Returns true only if THIS call performed the draw.
     */
    suspend fun resolveLotteryIfDue(campsiteId: String): Boolean {
        val campRef = campsites.document(campsiteId)
        val entrantIds = getLotteryEntrantIds(campsiteId)

        return db.runTransaction { tx ->
            val camp = tx.get(campRef)
            val deadline = camp.getString("lotteryDeadline")
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val resolved = camp.getBoolean("lotteryResolved") ?: false
            val name = camp.getString("name") ?: "the campsite"

            // No deadline set, still open, or another phone already drew
            if (resolved || deadline == null || !LocalDate.now().isAfter(deadline)) {
                return@runTransaction false
            }

            val count = (camp.getLong("numberOfWinners") ?: 0L).toInt()
            val winners = entrantIds.shuffled().take(count)

            tx.update(campRef, mapOf("lotteryResolved" to true, "winners" to winners))

            // One notification per entrant, written atomically with the draw
            entrantIds.forEach { id ->
                val msg = if (id in winners) "You won the lottery for $name!"
                else "You weren't selected in the lottery for $name."
                tx.set(notifications.document(), hashMapOf(
                    "userId" to id,
                    "message" to msg,
                    "sentAt" to System.currentTimeMillis(),
                    "campsiteId" to campRef.id,
                    "campsiteName" to name,
                    "type" to "lottery_result"
                ))
            }
            true
        }.await()
    }


}
