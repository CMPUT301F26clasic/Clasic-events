/**
 * Purpose: Intent tests for joining and leaving a campsite's lottery from the campsite details
 * screen (#4).
 * Outstanding issues: Uses the real Firestore database, so it needs internet. It saves one test
 * campsite before each test and removes it (and this device's lottery entry) afterwards.
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clasicampsite.data.DatabaseController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests joining and leaving a lottery by using the campsite details screen. */
@RunWith(AndroidJUnit4::class)
class JoinLotteryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testId = "join_test_campsite"
    private val db = DatabaseController()

    private val deviceId: String = Settings.Secure.getString(
        InstrumentationRegistry.getInstrumentation().targetContext.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    /** Saves a campsite to join, and makes sure this device isn't in its lottery yet. */
    @Before
    fun setUp() = runBlocking {
        FirebaseFirestore.getInstance().collection("campsites").document(testId).set(
            mapOf(
                "name" to "Two Jack Lakeside",
                "location" to "Banff, AB",
                "maxOccupancy" to 6,
                "rating" to 4.6,
                "availableFrom" to "2027-06-01",
                "availableTo" to "2027-09-15",
            )
        ).await()
        db.leaveLottery(testId, deviceId)
    }

    /** Takes this device out of the lottery and removes the test campsite. */
    @After
    fun tearDown() = runBlocking {
        db.leaveLottery(testId, deviceId)
        db.deleteCampsite(testId)
    }

    /** Shows the details screen for the test campsite. */
    private fun openDetails() {
        composeRule.setContent {
            val navController = rememberNavController()
            CampsiteDetailScreen(navController, campsiteId = testId)
        }
    }

    /** Waits up to 10 seconds for a node with [tag], because Firestore takes time. */
    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** US 01.01.06: someone who hasn't joined sees a Join button and no Leave button. */
    @Test
    fun notJoinedShowsJoinButton() {
        openDetails()
        waitForTag("joinLotteryButton")
        composeRule.onNodeWithTag("joinLotteryButton").assertIsDisplayed()
        composeRule.onNodeWithTag("leaveLotteryButton").assertDoesNotExist()
    }

    /** US 01.01.06: tapping Join saves this device in the lottery, and the button changes to Leave. */
    @Test
    fun joiningAddsYouToTheLottery() {
        openDetails()
        waitForTag("joinLotteryButton")
        composeRule.onNodeWithTag("joinLotteryButton").performClick()

        waitForTag("leaveLotteryButton")
        composeRule.onNodeWithTag("leaveLotteryButton").assertIsDisplayed()
        assertTrue(runBlocking { db.isUserInLottery(testId, deviceId) })
    }

    /** US 01.01.06: someone already in the lottery sees Leave; tapping it takes them out again. */
    @Test
    fun leavingTakesYouOutOfTheLottery() {
        runBlocking { db.joinLottery(testId, deviceId) }
        openDetails()
        waitForTag("leaveLotteryButton")
        composeRule.onNodeWithTag("leaveLotteryButton").performClick()

        waitForTag("joinLotteryButton")
        composeRule.onNodeWithTag("joinLotteryButton").assertIsDisplayed()
        assertFalse(runBlocking { db.isUserInLottery(testId, deviceId) })
    }

    /** Owners cannot join their own campsite's lottery. */
    @Test
    fun ownerCannotJoinOwnLottery() {
        runBlocking {
            FirebaseFirestore.getInstance().collection("campsites").document(testId)
                .update("ownerId", deviceId).await()
        }
        openDetails()
        waitForTag("ownerLotteryMessage")
        composeRule.onNodeWithTag("joinLotteryButton").assertDoesNotExist()
    }

    /** Once winners are drawn, Join Lottery is closed. */
    @Test
    fun cannotJoinAfterLotteryIsDrawn() {
        runBlocking {
            db.saveLottery(
                com.example.clasicampsite.model.Lottery(
                    id = testId,
                    title = "Two Jack Lakeside",
                    campsiteIds = listOf(testId),
                    winners = listOf("some-winner"),
                    availableDates = listOf("2027-07-01"),
                )
            )
        }
        openDetails()
        waitForTag("lotteryClosedText")
        composeRule.onNodeWithTag("joinLotteryButton").assertDoesNotExist()
    }
}
