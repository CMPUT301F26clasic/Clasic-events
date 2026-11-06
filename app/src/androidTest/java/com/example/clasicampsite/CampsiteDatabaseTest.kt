/*
 * Purpose: Tests campsite registration and storage in Firestore (US 02.01.01).
 * Outstanding issues: Uses the real Firestore database, so it needs internet
 * and Firestore rules that allow reads and writes.
 */

package com.example.clasicampsite

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CampsiteDatabaseTest {

    /** US 02.01.01: registering a campsite saves its details to Firestore. */
    @Test
    fun saveCampsiteStoresAndRetrievesCorrectInformation() = runBlocking {
        val database = DatabaseController()

        val campsite = Campsite(
            name = "Database Test Campsite",
            location = "Jasper, Alberta",
            maxOccupancy = 6,
            rating = 0.0,
            availableFrom = LocalDate.of(2027, 5, 1),
            availableTo = LocalDate.of(2027, 9, 30)
        )

        try {
            // Save the campsite.
            database.saveCampsite(campsite)

            // Retrieve the saved campsite.
            val retrieved = database.getCampsite(campsite.id)

            assertNotNull(retrieved)

            assertEquals("Database Test Campsite", retrieved!!.name)
            assertEquals("Jasper, Alberta", retrieved.location)
            assertEquals(6, retrieved.maxOccupancy)
            assertEquals(
                LocalDate.of(2027, 5, 1),
                retrieved.availableFrom
            )
            assertEquals(
                LocalDate.of(2027, 9, 30),
                retrieved.availableTo
            )

            // Verify the actual Firestore document.
            val document = FirebaseFirestore.getInstance()
                .collection("campsites")
                .document(campsite.id)
                .get()
                .await()

            assertEquals("Database Test Campsite", document.getString("name"))
            assertEquals("Jasper, Alberta", document.getString("location"))
            assertEquals("2027-05-01", document.getString("availableFrom"))
            assertEquals("2027-09-30", document.getString("availableTo"))

        } finally {
            // Delete the test campsite so it doesn't remain in the database.
            if (campsite.id.isNotBlank()) {
                FirebaseFirestore.getInstance()
                    .collection("campsites")
                    .document(campsite.id)
                    .delete()
                    .await()
            }
        }
    }
}