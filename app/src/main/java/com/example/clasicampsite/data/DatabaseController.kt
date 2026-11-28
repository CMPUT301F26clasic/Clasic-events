/**
 * Purpose: Saves, loads and deletes the app's data in Firebase Firestore. Each part of the app
 * adds the functions for its own data here.
 * Outstanding issues: Booking checks that dates are still free right before saving, but two
 * people booking at the exact same moment could still both succeed (needs a transaction).
 */
package com.example.clasicampsite.data

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.CampsiteReview
import com.example.clasicampsite.model.Lottery
import com.example.clasicampsite.model.LotteryEntrant
import com.example.clasicampsite.model.NotificationLog
import com.example.clasicampsite.model.Reservation
import com.example.clasicampsite.model.SentNotification
import com.example.clasicampsite.model.Review
import com.example.clasicampsite.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import com.google.firebase.firestore.SetOptions
import com.example.clasicampsite.AdminAccess.deviceId

private val DEFAULT_DATE = LocalDate.of(1970,  1, 1)

/** Reads and writes app data in Firestore. */
class DatabaseController {

    private val users = FirebaseFirestore.getInstance().collection("users")
    private val campsites = FirebaseFirestore.getInstance().collection("campsites")
    private val lotteries = FirebaseFirestore.getInstance().collection("lotteries")
    private val reservations = FirebaseFirestore.getInstance().collection("reservations")
    private val notifications = FirebaseFirestore.getInstance().collection("notifications")

    /** Returns user saved under [deviceId], or a new empty user if none is saved yet */
    suspend fun getUser(deviceId: String): User {
        val document = users.document(deviceId).get().await()
        return document.toObject(User::class.java) ?: User(id = deviceId)
    }

    /**
     * Sends a message from a campsite owner to a guest with a reservation there (US 02.12.01).
     * It is saved as a notification, so it shows in the guest's notifications and the admin logs.
     *
     * @param ownerId the owner's device ID
     * @param campsiteId the campsite the guest reserved
     * @param recipientId the guest's device ID
     * @param message the text to send
     * @throws IllegalArgumentException if the message is blank
     * @throws IllegalStateException if the sender isn't the owner or the guest has no reservation
     */
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

        /** reservations store the guest's device ID as "userId" */
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

    /**
     * Fetches every notification sent to [userId], newest first, with all their fields
     * (message, campsiteName, senderId, type...).
     *
     * @param userId the user's device ID
     * @return the notifications as maps of their Firestore fields
     */
    suspend fun getNotifications(userId: String): List<Map<String, Any?>> =
        notifications.whereEqualTo("userId", userId).get().await()
            .documents.mapNotNull { it.data }
            .sortedByDescending { (it["sentAt"] as? Long) ?: 0L }

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

    /** Deletes every campsite owned by [ownerId] (US 03.07.01) */
    suspend fun deleteCampsitesByOwner(ownerId: String) {
        val snapshot = campsites.whereEqualTo("ownerId", ownerId).get().await()
        for (document in snapshot.documents) {
            document.reference.delete().await()
        }
    }

    /** Removes the profile picture of the user saved under [deviceId], keeping their profile (US 03.03.01) */
    suspend fun removeProfileImage(deviceId: String) {
        users.document(deviceId).update("profileImage", "").await()
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
            "image" to campsite.image,
            "latitude" to campsite.latitude,
            "longitude" to campsite.longitude,
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
        image = getString("image") ?: "",
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude"),
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

    /**
     * Fetches every lottery that [deviceId] won, so winners can pick their dates (US 01.04.01).
     *
     * @param deviceId the user's device ID
     * @return the lotteries with this user in their winners
     */
    suspend fun getLotteriesWonBy(deviceId: String): List<Lottery> {
        val snapshot = lotteries.whereArrayContains("winners", deviceId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Lottery::class.java)?.apply { id = doc.id }
        }
    }

    /**
     * Checks if a user still wants notifications from owners and lottery results (US 01.10.01).
     * Users who never saved a profile get them.
     *
     * @param deviceId the user's device ID
     * @return false if the user turned notifications off
     */
    suspend fun wantsNotifications(deviceId: String): Boolean = getUser(deviceId).notificationsEnabled

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

    /**
     * Fetches every reservation made by one user (US 01.06.01, US 01.06.02).
     *
     * @param userId the user's device ID
     * @return all of that user's reservations, with their IDs filled in
     */
    suspend fun getReservationsForUser(userId: String): List<Reservation> {
        val snapshot = reservations.whereEqualTo("userId", userId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Reservation::class.java)?.apply { id = doc.id }
        }
    }

    /** Saves a new [reservation] and gives it an ID (US 01.04.02) */
    suspend fun saveReservation(reservation: Reservation) {
        val docRef = if (reservation.id.isBlank()) reservations.document() else reservations.document(reservation.id)
        reservation.id = docRef.id
        docRef.set(reservation).await()
    }

    /**
     * Marks a reservation as cancelled, which frees its dates for other winners (US 01.07.01).
     *
     * @param reservationId the Firestore ID of the reservation to cancel
     */
    suspend fun cancelReservation(reservationId: String) {
        reservations.document(reservationId).update("status", "cancelled").await()
    }

    /**
     * Marks a reservation as rejected by the campsite owner, which frees its dates for other
     * winners (US 02.08.01).
     *
     * @param reservationId the Firestore ID of the reservation to reject
     */
    suspend fun rejectReservation(reservationId: String) {
        reservations.document(reservationId).update("status", "rejected").await()
    }

    /** Sends [message] to the user with [deviceId] as an in-app notification (US 01.05.01) */
    /** Sends [message] to the user with [deviceId] as an in-app notification if enabled (US 01.05.01, US 01.10.01) */
    suspend fun sendNotification(deviceId: String, message: String) {
        if (!getUser(deviceId).notificationsEnabled) return
        val notification = hashMapOf(
            "userId" to deviceId,
            "message" to message,
            "sentAt" to System.currentTimeMillis(),
            "type" to "system"
        )
        notifications.add(notification).await()
    }
    /**
     * One in-app notification shown to a user.
     *
     * @property message The text of the notification
     * @property sentAt When it was sent, in milliseconds since 1970
     */
    data class UserNotification(
        val message: String = "",
        val sentAt: Long = 0L,
    )
    /**
     * Fetches in-app notifications for [deviceId], newest first.
     *
     * @param deviceId the user's device ID
     * @return the user's notifications, newest first
     */
    suspend fun getNotificationsForUser(deviceId: String): List<UserNotification> {
        val snapshot = notifications.whereEqualTo("userId", deviceId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val msg = doc.getString("message") ?: return@mapNotNull null
            val time = doc.getLong("sentAt") ?: 0L
            UserNotification(message = msg, sentAt = time)
        }.sortedByDescending { it.sentAt }
    }

    /**
     * Sends [message] from the campsite owner [ownerId] to the user with [deviceId], and also
     * saves who sent it and for which campsite, so sent notifications can be listed later
     * (US 02.10.01, US 02.10.02). Entrants see it like any other notification.
     */
    suspend fun sendOwnerNotification(
        deviceId: String,
        message: String,
        ownerId: String,
        campsiteId: String,
        sentAt: Long = System.currentTimeMillis(),
    ) {
        val notification = hashMapOf(
            "userId" to deviceId,
            "message" to message,
            "sentAt" to sentAt,
            "senderId" to ownerId,
            "campsiteId" to campsiteId,
        )
        notifications.add(notification).await()
    }

    /**
     * Fetches every notification the campsite owner [ownerId] sent with [sendOwnerNotification],
     * one per entrant who got it (US 02.10.02).
     */
    suspend fun getNotificationsSentBy(ownerId: String): List<SentNotification> {
        val snapshot = notifications.whereEqualTo("senderId", ownerId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val text = doc.getString("message") ?: return@mapNotNull null
            SentNotification(
                userId = doc.getString("userId") ?: "",
                message = text,
                sentAt = doc.getLong("sentAt") ?: 0L,
                campsiteId = doc.getString("campsiteId") ?: "",
            )
        }
    }

    /**
     * Sends win/loss notifications to entrants based on [winnerUserIds] (US 01.03.01). Entrants who
     * turned notifications off are skipped (US 01.10.01).
     *
     * @param campsiteName the campsite named in the message
     * @param entrantUserIds everyone who was in the lottery
     * @param winnerUserIds the entrants who won
     * @return how many entrants were skipped because they turned notifications off
     */
    suspend fun sendLotteryOutcomeNotifications(
        campsiteName: String,
        entrantUserIds: List<String>,
        winnerUserIds: List<String>,
    ): Int {
        val distinctEntrants = entrantUserIds.distinct()
        val winnerSet = winnerUserIds.toSet()
        var skipped = 0

        for (userId in distinctEntrants) {
            if (!wantsNotifications(userId)) {
                skipped++
            } else if (userId in winnerSet) {
                sendNotification(
                    deviceId = userId,
                    message = "Congrats! You won the lottery for $campsiteName. " +
                        "Open Reservations to pick your dates.",
                )
            } else {
                sendNotification(
                    deviceId = userId,
                    message = "Sorry, you were not selected in the lottery for $campsiteName.",
                )
            }
        }
        return skipped
    }

    /** Fetches all entrants for a specific campsite lottery */
    suspend fun getLotteryEntrants(campsiteId: String): List<LotteryEntrant> {
        val snapshot = campsites.document(campsiteId).collection("lottery").get().await()

        return snapshot.documents.map { doc ->
            LotteryEntrant(id = doc.id, campsiteId = campsiteId, userId = doc.getString("userId") ?: doc.id)
        }
    }

    /** Saves a lottery entrant under the campsite's lottery subcollection */
    suspend fun saveLotteryEntrant(entrant: LotteryEntrant) {
        val docRef = campsites.document(entrant.campsiteId).collection("lottery").document(entrant.userId)

        docRef.set(mapOf("userId" to entrant.userId, "joinedAt" to FieldValue.serverTimestamp())).await()

        entrant.id = docRef.id
    }

    /**
     * Checks if a user joined a campsite's lottery or has a reservation there.
     *
     * @param campsiteId the campsite's Firestore ID
     * @param userId the user's device ID
     * @return true if they entered or reserved it
     */
    private suspend fun isParticipant(campsiteId: String, userId: String): Boolean {
        if (isUserInLottery(campsiteId, userId)) return true
        return !reservations
            .whereEqualTo("campsiteId", campsiteId)
            .whereEqualTo("userId", userId)
            .get().await().isEmpty
    }

    /**
     * Saves a written review of a campsite (US 01.13.02). Each user has one review per campsite,
     * so posting again replaces it. Only people who entered or reserved the campsite can review it.
     *
     * @param campsiteId the campsite's Firestore ID
     * @param userId the reviewer's device ID
     * @param userName the name shown with the review
     * @param text the review, up to 1000 characters
     * @throws IllegalArgumentException if the text is blank or too long
     * @throws IllegalStateException if the user never entered or reserved the campsite
     */
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

    /**
     * Fetches the written reviews of a campsite, newest first (US 01.13.02).
     *
     * @param campsiteId the campsite's Firestore ID
     * @return the reviews
     */
    suspend fun getReviews(campsiteId: String): List<Review> =
        campsites.document(campsiteId).collection("reviews").get().await()
            .documents.mapNotNull { it.toObject(Review::class.java) }
            .sortedByDescending { it.postedAt }

    /**
     * Sends a message from an entrant or guest to the campsite owner (US 01.14.01). It is saved as a
     * notification for the owner.
     *
     * @param senderId the sender's device ID
     * @param campsiteId the campsite the message is about
     * @param message the text to send
     * @throws IllegalArgumentException if the message is blank
     * @throws IllegalStateException if the campsite has no owner, the sender is the owner, or
     * the sender never entered or reserved the campsite
     */
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

        /** saves the user's rating, replacing any old one */
        ratingsRef.document(userId)
            .set(mapOf("userId" to userId, "rating" to rating, "ratedAt" to FieldValue.serverTimestamp()))
            .await()

        /** averages all the ratings, rounded to one decimal place */
        val all = ratingsRef.get().await().documents.mapNotNull { it.getLong("rating") }
        val average = Math.round(all.average() * 10) / 10.0

        /** saves the new average */
        campsiteRef.set(mapOf("rating" to average), SetOptions.merge()).await()
        return average
    }

    /** Creates a shared [lottery] covering multiple campsites (#69) */
    suspend fun createSharedLottery(lottery: Lottery): String {
        require(lottery.campsiteIds.distinct().size >= 2) {
            "A shared lottery must include at least two campsites."
        }
        val cleanLottery = lottery.copy(campsiteIds = lottery.campsiteIds.distinct())
        saveLottery(cleanLottery)
        return cleanLottery.id
    }

    /** Fetches all lotteries that include [campsiteId] */
    suspend fun getLotteriesForCampsite(campsiteId: String): List<Lottery> {
        val snapshot = lotteries.whereArrayContains("campsiteIds", campsiteId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Lottery::class.java)?.apply { id = doc.id }
        }
    }

    /** Fetches every rating/review across all campsites for admin moderation (#71) */
    suspend fun getAllCampsiteReviews(): List<CampsiteReview> {
        val allSites = campsites.get().await()
        val results = mutableListOf<CampsiteReview>()

        for (siteDoc in allSites.documents) {
            val siteName = siteDoc.getString("name") ?: ""
            val ratingsSnapshot = siteDoc.reference.collection("ratings").get().await()
            for (ratingDoc in ratingsSnapshot.documents) {
                val stars = ratingDoc.getLong("rating")?.toInt() ?: 0
                val userId = ratingDoc.getString("userId") ?: ratingDoc.id
                val comment = ratingDoc.getString("comment")
                    ?: ratingDoc.getString("review")
                    ?: ""
                results.add(
                    CampsiteReview(
                        id = ratingDoc.id,
                        campsiteId = siteDoc.id,
                        campsiteName = siteName,
                        userId = userId,
                        rating = stars,
                        comment = comment,
                    )
                )
            }
        }
        return results
    }

    /** Deletes a review by [userId] on [campsiteId] and recalculates the campsite's average rating (#71) */
    suspend fun deleteCampsiteReview(campsiteId: String, userId: String): Double {
        val campsiteRef = campsites.document(campsiteId)
        val ratingsRef = campsiteRef.collection("ratings")

        ratingsRef.document(userId).delete().await()

        val remaining = ratingsRef.get().await().documents.mapNotNull { it.getLong("rating") }
        val newAverage = if (remaining.isEmpty()) {
            0.0
        } else {
            Math.round(remaining.average() * 10) / 10.0
        }

        campsiteRef.set(mapOf("rating" to newAverage), SetOptions.merge()).await()
        return newAverage
    }

    /**
     * Checks if a user has joined a campsite's lottery (US 01.01.06).
     *
     * @param campsiteId the campsite's Firestore ID
     * @param userId the user's device ID
     * @return true if the user is in the lottery
     */
    suspend fun isUserInLottery(campsiteId: String, userId: String): Boolean =
        campsites.document(campsiteId)
            .collection("lottery").document(userId)
            .get().await().exists()

    /**
     * Adds a user to a campsite's lottery (US 01.01.06). Joining is closed once winners have been
     * drawn or the lottery deadline has passed.
     *
     * @param campsiteId the campsite's Firestore ID
     * @param userId the user's device ID
     * @throws IllegalStateException if registration is closed
     */
    suspend fun joinLottery(campsiteId: String, userId: String) {
        val camp = getCampsite(campsiteId)
        val deadlinePassed = camp?.lotteryDeadline?.let { LocalDate.now().isAfter(it) } ?: false
        val drawn = camp?.lotteryResolved == true || getLottery(campsiteId)?.hasBeenDrawn() == true
        if (deadlinePassed || drawn) throw IllegalStateException("Registration is closed")

        saveLotteryEntrant(LotteryEntrant(campsiteId = campsiteId, userId = userId))
    }

    /**
     * Takes a user out of a campsite's lottery (US 01.01.06).
     *
     * @param campsiteId the campsite's Firestore ID
     * @param userId the user's device ID
     */
    suspend fun leaveLottery(campsiteId: String, userId: String) {
        campsites.document(campsiteId)
            .collection("lottery").document(userId)
            .delete().await()
    }

    /**
     * Fetches the device IDs of everyone in a campsite's lottery.
     *
     * @param campsiteId the campsite's Firestore ID
     * @return the entrants' device IDs
     */
    suspend fun getLotteryEntrantIds(campsiteId: String): List<String> =
        campsites.document(campsiteId).collection("lottery").get().await()
            .documents.map { it.id }

    /**
     * Sets the last day people can join a campsite's lottery (US 02.05.01).
     *
     * @param campsiteId the campsite's Firestore ID
     * @param deadline the last day to join
     */
    suspend fun updateLotteryDeadline(campsiteId: String, deadline: LocalDate) {
        campsites.document(campsiteId).update("lotteryDeadline", deadline.toString()).await()
    }

    /**
     * Marks a campsite's lottery as drawn, so nobody else can join and it isn't drawn again.
     *
     * @param campsiteId the campsite's Firestore ID
     * @param winners the device IDs that won
     */
    suspend fun markLotteryResolved(campsiteId: String, winners: List<String>) {
        campsites.document(campsiteId)
            .set(mapOf("lotteryResolved" to true, "winners" to winners), SetOptions.merge()).await()
    }

    /**
     * Fetches every notification ever sent, newest first, for the admin logs (US 03.08.01,
     * US 03.12.01).
     *
     * @return one log entry per notification
     */
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
     * Draws winners automatically once the lottery deadline has passed (US 02.05.01). It runs in a
     * transaction so two phones opening the campsite at once can't both draw. The winners are
     * also saved as a lottery, so they can pick their dates in Reservations.
     *
     * @param campsiteId the campsite's Firestore ID
     * @return true only if this call did the draw
     */
    suspend fun resolveLotteryIfDue(campsiteId: String): Boolean {
        val campRef = campsites.document(campsiteId)
        val lotteryRef = lotteries.document(campsiteId)
        val entrantIds = getLotteryEntrantIds(campsiteId)

        return FirebaseFirestore.getInstance().runTransaction { tx ->
            val camp = tx.get(campRef)
            val alreadyDrawn = (tx.get(lotteryRef).get("winners") as? List<*>)?.isNotEmpty() == true
            val deadline = camp.getString("lotteryDeadline")
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val resolved = camp.getBoolean("lotteryResolved") ?: false

            /** no deadline set, still open, or it was already drawn (here, by the owner, or by another phone) */
            if (!camp.exists() || resolved || alreadyDrawn || deadline == null || !LocalDate.now().isAfter(deadline)) {
                return@runTransaction false
            }

            val site = camp.toCampsite()
            val winners = Lottery.pickWinners(entrantIds, site.numberOfWinners)

            tx.update(campRef, mapOf("lotteryResolved" to true, "winners" to winners))
            tx.set(lotteryRef, Lottery.forCampsite(site, winners, LocalDate.now()))

            /** one notification per entrant, saved together with the draw */
            entrantIds.forEach { id ->
                val msg = if (id in winners) {
                    "Congrats! You won the lottery for ${site.name}. Open Reservations to pick your dates."
                } else {
                    "Sorry, you were not selected in the lottery for ${site.name}."
                }
                tx.set(notifications.document(), hashMapOf(
                    "userId" to id,
                    "message" to msg,
                    "sentAt" to System.currentTimeMillis(),
                    "campsiteId" to campRef.id,
                    "campsiteName" to site.name,
                    "type" to "lottery_result"
                ))
            }
            true
        }.await()
    }
}
