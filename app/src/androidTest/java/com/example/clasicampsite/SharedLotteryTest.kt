/**
 * Purpose: Intent and database tests for listing multiple campsites together
 * in a shared lottery (#69).
 * Outstanding issues: Uses the real Firestore database, so it needs internet.
 */
package com.example.clasicampsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
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
import java.time.LocalDate
import java.util.UUID

/** Tests creating a shared lottery across multiple campsites owned by the same user. */
@RunWith(AndroidJUnit4::class)
class SharedLotteryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val db = DatabaseController()
    private val firestore = FirebaseFirestore.getInstance()
    private val testOwnerId = "shared_lottery_owner_${UUID.randomUUID()}"
    private val siteOneId = "shared_site_1_${UUID.randomUUID()}"
    private val siteTwoId = "shared_site_2_${UUID.randomUUID()}"

    /** Creates two campsites owned by [testOwnerId] before each test. */
    @Before
    fun setUp() = runBlocking {
        db.saveCampsite(
            Campsite(
                id = siteOneId,
                ownerId = testOwnerId,
                name = "Shared Alpha Site",
                location = "Jasper, AB",
                maxOccupancy = 4,
                availableFrom = LocalDate.of(2027, 6, 1),
                availableTo = LocalDate.of(2027, 8, 31),
            )
        )
        db.saveCampsite(
            Campsite(
                id = siteTwoId,
                ownerId = testOwnerId,
                name = "Shared Beta Site",
                location = "Jasper, AB",
                maxOccupancy = 6,
                availableFrom = LocalDate.of(2027, 6, 1),
                availableTo = LocalDate.of(2027, 8, 31),
            )
        )
    }

    /** Cleans up test campsites and any lotteries created for them. */
    @After
    fun tearDown() = runBlocking {
        val lotteries = firestore.collection("lotteries")
            .whereArrayContains("campsiteIds", siteOneId)
            .get()
            .await()
        for (doc in lotteries.documents) {
            doc.reference.delete().await()
        }
        db.deleteCampsite(siteOneId)
        db.deleteCampsite(siteTwoId)
    }

    /**
     * Waits up to 10 seconds for [text] to appear, because Firestore takes time.
     *
     * @param text the text to wait for
     */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** US 02.04.08: selecting fewer than 2 campsites shows a validation error. */
    @Test
    fun requiresAtLeastTwoCampsites() {
        composeRule.setContent {
            val navController = rememberNavController()
            SharedLotteryScreen(navController = navController, ownerIdOverride = testOwnerId)
        }

        waitForText("Shared Alpha Site")
        composeRule.onNodeWithTag("sharedLotteryTitleField").performTextInput("Summer Dual Lottery")
        composeRule.onNodeWithTag("sharedLotteryDatesField").performTextInput("2027-07-01")
        composeRule.onNodeWithTag("sharedSite_$siteOneId").performClick()
        composeRule.onNodeWithTag("createSharedLotteryButton").performClick()

        composeRule.onNodeWithText("Please select at least 2 campsites.").assertIsDisplayed()
    }

    /** US 02.04.08: owner can select two campsites and save a shared lottery in Firestore. */
    @Test
    fun createsSharedLotteryForTwoCampsites() {
        composeRule.setContent {
            val navController = rememberNavController()
            SharedLotteryScreen(navController = navController, ownerIdOverride = testOwnerId)
        }

        waitForText("Shared Alpha Site")
        composeRule.onNodeWithTag("sharedLotteryTitleField").performTextInput("Jasper Shared Lottery")
        composeRule.onNodeWithTag("sharedLotteryDatesField").performTextInput("2027-07-10, 2027-07-11")
        composeRule.onNodeWithTag("sharedSite_$siteOneId").performClick()
        composeRule.onNodeWithTag("sharedSite_$siteTwoId").performClick()
        composeRule.onNodeWithTag("createSharedLotteryButton").performClick()

        waitForText("Shared lottery created for 2 campsites!")

        val saved = runBlocking { db.getLotteriesForCampsite(siteOneId) }
        assertEquals(1, saved.size)
        assertTrue(saved.first().hasMultipleCampsites())
        assertTrue(siteTwoId in saved.first().campsiteIds)
    }

    /** Entrants who join a campsite in a shared lottery can be drawn as winners and book. */
    @Test
    fun sharedLotteryCanBeJoinedDrawnAndWon() = runBlocking {
        val entrantId = "shared_entrant_${UUID.randomUUID()}"
        db.joinLottery(siteOneId, entrantId)

        val lotteryId = db.createSharedLottery(
            com.example.clasicampsite.model.Lottery(
                title = "Winnable Shared Lottery",
                campsiteIds = listOf(siteOneId, siteTwoId),
                availableDates = listOf("2027-07-15"),
            )
        )

        val created = db.getLottery(lotteryId)!!
        val winners = db.drawSharedLotteryWinners(created, winnerCount = 1)

        assertTrue(entrantId in winners)
        val wonByEntrant = db.getWonLotteriesForUser(entrantId)
        assertTrue(wonByEntrant.any { it.id == lotteryId && it.isWinner(entrantId) })

        db.leaveLottery(siteOneId, entrantId)
    }
}