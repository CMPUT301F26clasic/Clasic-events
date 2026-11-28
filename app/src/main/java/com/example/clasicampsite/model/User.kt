/**
 * Purpose: A person using the app, saved in the Firestore "users" collection under their
 * device ID (US 01.09.01). Also checks that their details are valid (US 01.07.03).
 * Admins are marked with admin = true, set by hand in Firebase (US 03.11.01). Everyone can use
 * entrant and owner features.
 * Outstanding issues: Firestore rules don't yet stop users from setting admin on themselves.
 */
package com.example.clasicampsite.model

import com.google.firebase.firestore.IgnoreExtraProperties

/** The shape of a valid email: text@text.text, with one @ and no spaces. */
/** made by claude */
private val EMAIL_FORMAT = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/**
 * @property id Device ID, also used as the Firestore document ID
 * @property name Display name
 * @property email Contact email
 * @property phone Phone number, optional
 * @property notificationsEnabled False if the user opted out of notifications
 * @property profileImage Profile picture, shrunk and stored as Base64 text, or "" for none
 * @property admin True if this user is an admin. Only set by hand in Firebase (US 03.11.01)
 * @property colorMode Color blind mode: "regular", "protanopia", "deuteranopia" or "tritanopia" */

@IgnoreExtraProperties
data class User(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var notificationsEnabled: Boolean = true,
    var profileImage: String = "",
    var admin: Boolean = false,
    var colorMode: String = "regular",
) {
    /**
     * Checks the user's details (US 01.07.03).
     * - name can't be blank.
     * - email must look like name@domain.com.
     * - phone is optional, but if given it must have exactly 10 digits and no letters.
     *   Spaces, dashes and brackets are allowed, e.g. "(780) 555-0123".
     * @return What is wrong with the details, or null if they are all valid.
     */
    fun errorMessage(): String? {
        if (name.isBlank()) {
            return "Name is required"
        }
        if (!EMAIL_FORMAT.matches(email.trim())) {
            return "Enter a valid email, like name@example.com"
        }
        if (phone.isNotBlank()) {
            val digits = phone.filter {it.isDigit()}
            val hasLetters = phone.any {it.isLetter()}
            if (digits.length != 10 || hasLetters) {
                return "Phone number must have 10 digits"
            }
        }
        return null
    }
}