/**
 * Purpose: Holds the admin's list of campsite reviews/ratings and removes ones that
 * violate app policy (#71).
 * Outstanding issues: None.
 */
package com.example.clasicampsite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.CampsiteReview
import kotlinx.coroutines.launch

/** Holds data and actions for the admin campsite reviews moderation screen. */
class AdminReviewsViewModel : ViewModel() {

    private val database = DatabaseController()

    var reviews by mutableStateOf(listOf<CampsiteReview>())
    var message by mutableStateOf("")
    var loaded by mutableStateOf(false)

    init {
        load()
    }

    /** Loads all reviews across every campsite from Firestore. */
    fun load() {
        viewModelScope.launch {
            loaded = false
            try {
                reviews = database.getAllCampsiteReviews()
                message = ""
            } catch (e: Exception) {
                message = "Couldn't load reviews"
            }
            loaded = true
        }
    }

    /** Deletes [review] from Firestore, recalculates the campsite's rating, and updates the list. */
    fun removeReview(review: CampsiteReview) {
        viewModelScope.launch {
            try {
                database.deleteCampsiteReview(review.campsiteId, review.userId)
                reviews = reviews.filterNot {
                    it.campsiteId == review.campsiteId && it.userId == review.userId
                }
                message = "Review removed"
            } catch (e: Exception) {
                message = "Couldn't remove review"
            }
        }
    }
}