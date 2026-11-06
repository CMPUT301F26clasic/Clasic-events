/*
Purpose: Saves, loads and deletes the app's data in Firebase Firestore. Each part of the app
adds the functions for its own data here.
Outstanding issues: Only users and campsites are handled so far.
 */
package com.example.clasicampsite.data

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import com.google.firebase.firestore.DocumentSnapshot

private val DEFAULT_DATE = LocalDate.of(1970, 1, 1)

/** Reads and writes app data in Firestore. */
class DatabaseController {

    private val users = FirebaseFirestore.getInstance().collection("users")
    private val campsites = FirebaseFirestore.getInstance().collection("campsites")

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
        return if (document.exists()) document.toCampsite() else null
    }

    /** Fetches all campsites from the "campsites" collection */
    suspend fun getCampsites(): List<Campsite> {
        val snapshot = campsites.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Campsite::class.java)?.apply { id = doc.id }
        }
    }

    /** Returns every campsite for the admin list. Dates are read from "yyyy-MM-dd" text. */
    /** Returns every campsite for the admin list. */
    suspend fun getAllCampsites(): List<Campsite> {
        val snapshot = campsites.get().await()
        return snapshot.documents.map { it.toCampsite() }
    }

    /** Saves or updates a campsite in Firestore */
    suspend fun saveCampsite(campsite: Campsite) {
        val docRef = if (campsite.id.isBlank()) campsites.document() else campsites.document(campsite.id)
        campsite.id = docRef.id
        docRef.set(campsite).await()
    }

    /** Deletes the campsite with [campsiteId] from Firestore */
    suspend fun deleteCampsite(campsiteId: String) {
        campsites.document(campsiteId).delete().await()
    }

    /** Builds a Campsite from a Firestore document, reading dates from "yyyy-MM-dd" text. */
    private fun DocumentSnapshot.toCampsite(): Campsite = Campsite(
        id = id,
        name = getString("name") ?: "",
        location = getString("location") ?: "",
        maxOccupancy = getLong("maxOccupancy")?.toInt() ?: 0,
        rating = getDouble("rating") ?: 0.0,
        availableFrom = runCatching { LocalDate.parse(getString("availableFrom")) }
            .getOrDefault(DEFAULT_DATE),
        availableTo = runCatching { LocalDate.parse(getString("availableTo")) }
            .getOrDefault(DEFAULT_DATE),
    )
}
