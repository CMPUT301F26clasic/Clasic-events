/**
 * Purpose: Intent tests for the campsite list: search bar, filter dialog, and opening details.
 * Outstanding issues: The date filter is tested in CampsiteSearchFilterTest, because picking
 * dates through the date picker dialog is slow and fragile.
 */
package com.example.clasicampsite

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the campsite list screen by using it. */
@RunWith(AndroidJUnit4::class)
class CampsiteListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Opens the campsite list with the sample campsites, with the details screen reachable from
     * it. The details screen shows its built-in sample, since the sample IDs aren't in Firestore.
     */
    @Before
    fun openList() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "campsites") {
                composable("campsites") { CampsiteListScreen(navController, campsites = sampleCampsites) }
                composable("campsite_detail/{campsiteId}") { CampsiteDetailScreen(navController) }
            }
        }
    }

    /**
     * Types [text] into the search bar.
     *
     * @param text what to search for
     */
    private fun search(text: String) {
        composeRule.onNodeWithTag("searchField").performTextInput(text)
    }

    /** Checks exactly [count] campsite cards are on screen. Only use for 2 or fewer cards. */
    private fun assertResultCount(count: Int) {
        composeRule.onAllNodes(hasText("Max occupancy", substring = true)).assertCountEquals(count)
    }

    /** Opens the filter dialog, fills in what is given, and applies it. */
    private fun applyFilter(location: String = "", guests: String = "") {
        composeRule.onNodeWithTag("filterButton").performClick()
        if (location.isNotEmpty()) composeRule.onNodeWithTag("filterLocation").performTextInput(location)
        if (guests.isNotEmpty()) composeRule.onNodeWithTag("filterGuests").performTextInput(guests)
        composeRule.onNodeWithTag("applyFilterButton").performClick()
    }

    /** ----- Search bar ----- */

    /** US 01.01.01: with nothing searched, every campsite is listed */
    @Test
    fun listShowsAllCampsitesByDefault() {
        composeRule.onNodeWithText("Pine Ridge").assertExists()
        composeRule.onNodeWithTag("campsiteList").performScrollToNode(hasText("River Bend"))
        composeRule.onNodeWithText("River Bend").assertExists()
    }

    /** US 01.01.01: searching a name shows only the campsites with that name */
    @Test
    fun searchByNameShowsOnlyMatches() {
        search("Pine")
        composeRule.onNodeWithText("Pine Ridge").assertExists()
        assertResultCount(1)
    }

    /** US 01.01.01: searching a location shows only the campsites there */
    @Test
    fun searchByLocationShowsOnlyMatches() {
        search("Banff")
        composeRule.onNodeWithText("Lakeside Haven").assertExists()
        assertResultCount(1)
    }

    /** US 01.01.01: search ignores upper and lower case */
    @Test
    fun searchIgnoresCase() {
        search("cedar")
        composeRule.onNodeWithText("Cedar Hollow").assertExists()
        assertResultCount(1)
    }

    /** US 01.01.01: searching a number shows the campsites that fit that many guests */
    @Test
    fun searchByOccupancyShowsCampsitesThatFitThatManyGuests() {
        search("8")
        composeRule.onNodeWithText("Cedar Hollow").assertExists()
        composeRule.onNodeWithText("Sunset Meadows").assertExists()
        assertResultCount(2)
    }

    /** US 01.01.01: a search with no matches shows the no results message */
    @Test
    fun searchWithNoMatchShowsNoResultsMessage() {
        search("zzzz")
        composeRule.onNodeWithTag("noResultsText").assertIsDisplayed()
        composeRule.onNodeWithTag("noResultsText").assertTextEquals("No campsites found")
    }

    /** US 01.01.01: clearing the search shows every campsite again */
    @Test
    fun clearingSearchShowsCampsitesAgain() {
        search("zzzz")
        composeRule.onNodeWithTag("noResultsText").assertIsDisplayed()
        composeRule.onNodeWithTag("searchField").performTextClearance()
        composeRule.onNodeWithTag("noResultsText").assertDoesNotExist()
        composeRule.onNodeWithText("Pine Ridge").assertExists()
    }

    /** ----- Filter ----- */

    /** US 01.01.02: filtering by location shows only the campsites in that location */
    @Test
    fun filterByLocationShowsOnlyThatLocation() {
        applyFilter(location = "Banff")
        composeRule.onNodeWithText("Lakeside Haven").assertExists()
        assertResultCount(1)
        composeRule.onNodeWithTag("filterButton").assertTextEquals("Filter (1)")
    }

    /** US 01.01.02: filtering by guests shows only the campsites with room for them */
    @Test
    fun filterByGuestsShowsOnlyCampsitesWithRoom() {
        applyFilter(guests = "8")
        composeRule.onNodeWithText("Cedar Hollow").assertExists()
        composeRule.onNodeWithText("Sunset Meadows").assertExists()
        assertResultCount(2)
    }

    /** US 01.01.02: asking for more guests than any campsite holds shows no results */
    @Test
    fun filterWithTooManyGuestsShowsNoResults() {
        applyFilter(guests = "20")
        composeRule.onNodeWithTag("noResultsText").assertIsDisplayed()
    }

    /** US 01.01.02: the location and guests filters work together */
    @Test
    fun locationAndGuestsFiltersWorkTogether() {
        applyFilter(location = "Canmore", guests = "10")
        composeRule.onNodeWithText("Cedar Hollow").assertExists()
        assertResultCount(1)
        composeRule.onNodeWithTag("filterButton").assertTextEquals("Filter (2)")
    }

    /** US 01.01.02: a location and guest count that no campsite matches shows no results */
    @Test
    fun locationAndGuestsFilterThatCantBothMatchShowsNoResults() {
        applyFilter(location = "Canmore", guests = "12")
        composeRule.onNodeWithTag("noResultsText").assertIsDisplayed()
    }

    /** US 01.01.02: Reset clears the filter */
    @Test
    fun resetClearsTheFilter() {
        applyFilter(location = "Banff")
        composeRule.onNodeWithTag("filterButton").performClick()
        composeRule.onNodeWithTag("resetFilterButton").performClick()
        composeRule.onNodeWithTag("filterButton").assertTextEquals("Filter")
        composeRule.onNodeWithTag("noResultsText").assertDoesNotExist()
        composeRule.onNodeWithTag("campsiteList").performScrollToNode(hasText("Pine Ridge"))
        composeRule.onNodeWithText("Pine Ridge").assertExists()
    }

    /** US 01.01.01, US 01.01.02: search and filter narrow the list together */
    @Test
    fun searchAndFilterNarrowTheListTogether() {
        applyFilter(guests = "6")
        search("Pine")
        composeRule.onNodeWithText("Pine Ridge").assertExists()
        assertResultCount(1)
    }

    /** ----- Opening details ----- */

    /** US 01.01.04: tapping a campsite opens its details */
    @Test
    fun tappingACampsiteOpensDetails() {
        composeRule.onNodeWithText("Pine Ridge").performClick()
        composeRule.onNodeWithTag("campsiteDetailsTitle").assertIsDisplayed()
    }

    /** US 01.01.04: going back from the details returns to the list */
    @Test
    fun backFromDetailsReturnsToList() {
        composeRule.onNodeWithText("Pine Ridge").performClick()
        composeRule.onNodeWithTag("backButton").performClick()
        composeRule.onNodeWithTag("searchField").assertIsDisplayed()
    }
}