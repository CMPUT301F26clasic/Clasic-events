/*
Purpose: Saves, loads and deletes the app's data in Firebase Firestore. Each part of the app
adds the functions for its own data here.
 */
package com.example.clasicampsite.data

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Reads and writes app data in Firestore. */
class DatabaseController {

    private val users = FirebaseFirestore.getInstance().collection("users")
    private val campsites = FirebaseFirestore.getInstance().collection("campsites")

    /** Returns user saved under [deviceId], or a new empty user if none is saved yet */
    suspend fun getUser(deviceId: String): User {
        val document = users.document(deviceId).get().await()
        return document.toObject(User::class.java) ?: User(id = deviceId)
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
        return document.toObject(Campsite::class.java)?.apply { id = document.id }
    }

    /** Fetches all campsites from the "campsites" collection */
    suspend fun getCampsites(): List<Campsite> {
        val snapshot = campsites.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Campsite::class.java)?.apply { id = doc.id }
        }
    }

    /** Saves or updates a campsite in Firestore */
    suspend fun saveCampsite(campsite: Campsite) {
        val docRef = if (campsite.id.isBlank()) campsites.document() else campsites.document(campsite.id)
        campsite.id = docRef.id
        docRef.set(campsite).await()
    }
}