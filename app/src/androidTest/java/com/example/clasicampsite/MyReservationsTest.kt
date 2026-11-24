/*
Purpose: Intent tests for the My Reservations screen: seeing active reservations (US 01.06.01)
and past reservations (US 01.06.02).
Outstanding issues: Uses the real Firestore database, so it needs internet. It saves two test
reservations for this device before each test and deletes them afterwards.
*/
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the My Reservations screen with one upcoming and one finished reservation. */
@RunWith(AndroidJUnit4::class)
class MyReservationsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val upcomingId = "ui-test-res-upcoming"
    private val finishedId = "ui-test-res-finished"

    private val deviceId: String = Settings.Secure.getString(
        InstrumentationRegistry.getInstrumentation().targetContext.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    /** Saves an upcoming and a finished reservation for this device */
    @Before
    fun setUp() = runBlocking {
        val db = DatabaseController()
        db.saveReservation(
            Reservation(
                id = upcomingId,
                lotteryId = "test-lottery",
                campsiteId = "ui-test-camp-a",
                campsiteName = "Two Jack Lakeside",
                userId = deviceId,
                dates = listOf("2099-08-01", "2099-08-02"),
            )
        )
        db.saveReservation(
            Reservation(
                id = finishedId,
                lotteryId = "test-lottery",
                campsiteId = "ui-test-camp-b",
                campsiteName = "Lake Louise Campground",
                userId = deviceId,
                dates = listOf("2020-08-14"),
            )
        )
    }

    /** Deletes the two test reservations */
    @After
    fun tearDown() = runBlocking {
        val reservations = FirebaseFirestore.getInstance().collection("reservations")
        reservations.document(upcomingId).delete().await()
        reservations.document(finishedId).delete().await()
        Unit
    }

    /** Opens the My Reservations screen and waits for the list to load */
    private fun openScreen() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "reservations") {
                composable("reservations") { CampSiteReservationScreen(navController) }
                composable("reservation_confirmation/{reservationId}") { backStackEntry ->
                    val reservationId = backStackEntry.arguments?.getString("reservationId") ?: ""
                    ReservationConfirmationScreen(navController, reservationId = reservationId)
                }
            }
        }
        waitFor("reservationsList")
    }

    private fun waitFor(tag: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Scrolls the list to the reservation with [id], since this device may have others too */
    private fun scrollTo(id: String) {
        composeRule.onNodeWithTag("reservationsList").performScrollToNode(hasTestTag("reservation_$id"))
    }

    /** US 01.06.01: an upcoming reservation shows in the list */
    @Test
    fun activeReservationIsShown() {
        openScreen()
        composeRule.onNodeWithTag("activeHeader").assertIsDisplayed()
        scrollTo(upcomingId)
        composeRule.onNodeWithTag("reservation_$upcomingId").assertIsDisplayed()
    }

    /** US 01.06.02: a reservation whose dates have passed shows in the list under Past */
    @Test
    fun pastReservationIsShown() {
        openScreen()
        composeRule.onNodeWithTag("reservationsList").performScrollToNode(hasTestTag("pastHeader"))
        composeRule.onNodeWithTag("pastHeader").assertIsDisplayed()
        scrollTo(finishedId)
        composeRule.onNodeWithTag("reservation_$finishedId").assertIsDisplayed()
    }

    /** US 01.06.01: tapping an active reservation opens its confirmation */
    @Test
    fun tappingActiveReservationOpensConfirmation() {
        openScreen()
        scrollTo(upcomingId)
        composeRule.onNodeWithTag("reservation_$upcomingId").performClick()

        waitFor("confirmedTitle")
        composeRule.onNodeWithTag("confirmedTitle").assertIsDisplayed()
    }
}
