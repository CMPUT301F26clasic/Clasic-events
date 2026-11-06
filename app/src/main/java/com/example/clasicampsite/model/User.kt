/*
Purpose: A person using the app, saved in the Firestore "users" collection under their
device ID (US 01.09.01). Also checks that their details are valid (US 01.07.03).
Outstanding issues: Roles (entrant, owner, admin) are not added yet.*/
package com.example.clasicampsite.model

// The shape of a valid email: text@text.text, with one @ and no spaces.
private val EMAIL_FORMAT = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") // made by claude

/**
 * @property id Device ID, also used as the Firestore document ID
 * @property name Display name
 * @property email Contact email
 * @property phone Phone number, optional
 * @property notificationsEnabled False if the user opted out of notifications
 * @property profileImage Profile picture, shrunk and stored as Base64 text, or "" for none
 */
data class User(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var notificationsEnabled: Boolean = true,
    var profileImage: String = "",
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