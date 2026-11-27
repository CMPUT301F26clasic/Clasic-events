/**
 * Purpose: Intent tests for a campsite owner rejecting a reservation from the Bookings &
 * Reservations screen (US 02.08.01): the Reject button, saving the reservation as rejected,
 * freeing its dates, and notifying the camper.
 * Outstanding issues: Uses the real Firestore database, so it needs internet. It saves a test
 * campsite and one test reservation before each test and deletes them, and the test
 * notification, afterwards.
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests rejecting one upcoming reservation at a test campsite. */
@RunWith(AndroidJUnit4::class)
class OwnerRejectReservationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val campsiteId = "ui-test-reject-camp"
    private val campsiteName = "UI Test Reject Camp"
    private val reservationId = "ui-test-reject-res"

    private val deviceId: String = Settings.Secure.getString(
        InstrumentationRegistry.getInstrumentation().targetContext.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    /** Saves a test campsite and one upcoming reservation for it, booked by this device */
    @Before
    fun setUp() = runBlocking {
        FirebaseFirestore.getInstance().collection("campsites").document(campsiteId)
            .set(mapOf("name" to campsiteName, "location" to "Banff, AB")).await()
        DatabaseController().saveReservation(
            Reservation(
                id = reservationId,
                campsiteId = campsiteId,
                campsiteName = campsiteName,
                userId = deviceId,
                dates = listOf("2099-08-01", "2099-08-02"),
            )
        )
    }

    /** Deletes the test reservation, the test campsite and any test rejection notifications */
    @After
    fun tearDown() = runBlocking {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("reservations").document(reservationId).delete().await()
        firestore.collection("campsites").document(campsiteId).delete().await()
        val sent = firestore.collection("notifications").whereEqualTo("userId", deviceId).get().await()
        sent.documents
            .filter { (it.getString("message") ?: "").contains(campsiteName) }
            .forEach { it.reference.delete().await() }
        Unit
    }

    /** Opens the Bookings & Reservations screen for the test campsite and waits for it to load */
    private fun openScreen() {
        composeRule.setContent {
            OwnerCampsiteScheduleScreen(rememberNavController(), campsiteId = campsiteId)
        }
        waitFor("rejectButton_$reservationId")
    }

    /**
     * Waits up to 15 seconds for a node with [tag], because Firestore takes time.
     *
     * @param tag the test tag to wait for
     */
    private fun waitFor(tag: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Taps Reject, types [reason] and confirms, then waits for the result message.
     *
     * @param reason the reason typed into the popup
     */
    private fun rejectWithReason(reason: String) {
        composeRule.onNodeWithTag("rejectButton_$reservationId").performClick()
        composeRule.onNodeWithTag("rejectReasonField").performTextInput(reason)
        composeRule.onNodeWithTag("confirmRejectButton").performClick()
        waitFor("rejectMessage")
    }

    /** US 02.08.01: an upcoming reservation has a Reject button */
    @Test
    fun upcomingReservationShowsRejectButton() {
        openScreen()
        composeRule.onNodeWithTag("rejectButton_$reservationId").assertIsDisplayed()
    }

    /** US 02.08.01: rejecting saves the reservation as rejected and notifies the camper */
    @Test
    fun rejectingSavesStatusAndNotifiesCamper() {
        openScreen()
        rejectWithReason("Site closed for repairs")

        val saved = runBlocking { DatabaseController().getReservation(reservationId) }
        assertEquals("rejected", saved?.status)

        val messages = runBlocking {
            FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", deviceId).get().await()
                .documents.mapNotNull { it.getString("message") }
        }
        assertTrue(messages.any { it.contains(campsiteName) && it.contains("Site closed for repairs") })
    }

    /** US 02.08.01: a rejected reservation frees its dates and moves to the Cancelled filter */
    @Test
    fun rejectedReservationFreesDatesAndShowsUnderCancelled() {
        openScreen()
        rejectWithReason("Double booked")

        composeRule.onNodeWithTag("noBookedDatesText").assertIsDisplayed()
        composeRule.onNodeWithTag("filter_cancelled").performClick()
        composeRule.onNodeWithText("Status: Rejected").assertIsDisplayed()
    }
}
