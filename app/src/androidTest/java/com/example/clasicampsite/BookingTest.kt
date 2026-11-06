/*
Purpose: Intent tests for booking a campsite after winning a lottery: seeing available dates
(US 01.04.01), picking a campsite and dates (US 01.04.02), declining (US 01.04.03), and getting
the confirmation with a CSV download (US 01.05.01, US 01.05.02).
Outstanding issues: Uses the real Firestore database, so it needs internet. It saves a test
lottery and deletes its test reservations before each test.
*/
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Lottery
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookingTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testLotteryId = "ui-test-lottery"
    private val campsiteA = "ui-test-camp-a"
    private val campsiteB = "ui-test-camp-b"

    private val deviceId: String = Settings.Secure.getString(
        InstrumentationRegistry.getInstrumentation().targetContext.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    /** Saves a lottery this device won with two campsites, and removes old test reservations */
    @Before
    fun setUp() = runBlocking {
        DatabaseController().saveLottery(
            Lottery(
                id = testLotteryId,
                title = "Test lottery",
                campsiteIds = listOf(campsiteA, campsiteB),
                winners = listOf(deviceId),
                availableDates = listOf("2099-07-01", "2099-07-02", "2099-07-03"),
            )
        )
        val reservations = FirebaseFirestore.getInstance().collection("reservations")
        for (campsiteId in listOf(campsiteA, campsiteB)) {
            val old = reservations.whereEqualTo("campsiteId", campsiteId).get().await()
            old.documents.forEach { it.reference.delete().await() }
        }
    }

    /** Opens the available dates screen for the test lottery and waits for it to load */
    private fun openScreen() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "dates") {
                composable("dates") { AvailableDatesScreen(navController, lotteryId = testLotteryId) }
                composable("reservation_confirmation/{reservationId}") { backStackEntry ->
                    val reservationId = backStackEntry.arguments?.getString("reservationId") ?: ""
                    ReservationConfirmationScreen(navController, reservationId = reservationId)
                }
            }
        }
        waitFor("date_2099-07-01")
    }

    private fun waitFor(tag: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun winnerSeesAvailableDates() {
        openScreen()
        composeRule.onNodeWithTag("date_2099-07-01").assertIsDisplayed()
        composeRule.onNodeWithTag("date_2099-07-02").assertIsDisplayed()
        composeRule.onNodeWithTag("date_2099-07-03").assertIsDisplayed()
    }

    @Test
    fun winnerCanPickACampsiteAndDates() {
        openScreen()
        composeRule.onNodeWithTag("campsite_$campsiteA").assertIsDisplayed()
        composeRule.onNodeWithTag("campsite_$campsiteB").performClick()
        waitFor("date_2099-07-01")

        composeRule.onNodeWithTag("reserveButton").assertIsNotEnabled()
        composeRule.onNodeWithTag("date_2099-07-01").performClick()
        composeRule.onNodeWithTag("date_2099-07-02").performClick()

        composeRule.onNodeWithTag("selectedCountText").assertTextContains("2 night(s) selected")
        composeRule.onNodeWithTag("reserveButton").assertIsEnabled()
    }

    @Test
    fun reservingShowsConfirmationWithCsvDownload() {
        openScreen()
        composeRule.onNodeWithTag("date_2099-07-01").performClick()
        composeRule.onNodeWithTag("reserveButton").performClick()

        waitFor("confirmedTitle")
        composeRule.onNodeWithTag("confirmedTitle").assertIsDisplayed()
        composeRule.onNodeWithTag("downloadCsvButton").assertIsDisplayed()
    }

    @Test
    fun winnerCanDecline() {
        openScreen()
        composeRule.onNodeWithTag("declineButton").performClick()
        composeRule.onNodeWithTag("confirmDeclineButton").performClick()

        waitFor("declinedText")
        composeRule.onNodeWithTag("declinedText").assertIsDisplayed()
    }
}
