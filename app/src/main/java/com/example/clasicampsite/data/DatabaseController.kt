/*
Purpose: Saves, loads and deletes the app's data in Firebase Firestore. Each part of the app
adds the functions for its own data here.
Outstanding issues: Only users are handled so far.
 */
package com.example.clasicampsite.data

import com.example.clasicampsite.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Reads and writes app data in Firestore. */
class DatabaseController {

    private val users = FirebaseFirestore.getInstance().collection("users")

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
}