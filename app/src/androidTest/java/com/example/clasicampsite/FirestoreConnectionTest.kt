/*
 * File: FirestoreConnectionTest.kt
 * Purpose: Checks that the app can write to and read from Firestore, which every
 *          data-saving user story depends on.
 * Outstanding issues: Uses the live database (collection "connectionTest"), so it needs an
 *                     internet connection and Firestore rules that allow reads and writes.
 */
package com.example.clasicampsite

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// Tests that Firebase setup works on real device or emulator
@RunWith(AndroidJUnit4::class)
class FirestoreConnectionTest {

    //Writes timestamp to Firestore, reads it back from server, checks it matches
    @Test
    fun writeThenReadBackFromServer() = runBlocking {
        val doc = FirebaseFirestore.getInstance()
            .collection("connectionTest")
            .document("ping")
        val sentAt = System.currentTimeMillis()

        withTimeout(15_000) {
            doc.set(mapOf("sentAtMillis" to sentAt)).await()
            val saved = doc.get(Source.SERVER).await().getLong("sentAtMillis")
            assertEquals(sentAt, saved)
        }
    }
}