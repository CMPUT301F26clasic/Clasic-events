/**
 * Purpose: Unit tests for User's validation (US 01.07.03) and its default settings
 * (US 01.10.01, US 03.11.01, colour blind mode WOW factor).
 */
package com.example.clasicampsite

import com.example.clasicampsite.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for the User model: profile checks (US 01.07.03) and new user defaults. */
class UserTest {

    private val validUser = User(
        id = "device-1",
        name = "Bob Duncan",
        email = "bob.duncan@ualberta.ca",
        phone = "123-456-7890",
    )

    private val emailError = "Enter a valid email, like name@example.com"
    private val phoneError = "Phone number must have 10 digits"

    /** US 01.07.03: a user with valid info has no error */
    @Test
    fun validUserHasNoError() {
        assertNull(validUser.errorMessage())
    }

    /** US 01.10.01: notifications are on for a new user until they opt out */
    @Test
    fun newUserHasNotificationsOn() {
        assertEquals(true, User().notificationsEnabled)
    }

    /** US 03.11.01: a new user is not an admin */
    @Test
    fun newUserIsNotAdmin() {
        assertEquals(false, User().admin)
    }

    /** Colour blind mode (#76, WOW factor): a new user starts with the regular colours */
    @Test
    fun newUserHasRegularColorMode() {
        assertEquals("regular", User().colorMode)
    }

    /** US 01.07.03: a profile needs a name */
    @Test
    fun blankNameIsAnError() {
        assertEquals("Name is required", validUser.copy(name = "").errorMessage())
        assertEquals("Name is required", validUser.copy(name = "   ").errorMessage())
    }

    /** US 01.07.03: spaces around the email are ignored */
    @Test
    fun emailWithSpacesAroundIsAccepted() {
        assertNull(validUser.copy(email = "  jordan@example.com  ").errorMessage())
    }

    /** US 01.07.03: an email without an @ is an error */
    @Test
    fun emailWithoutAtSignIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan.ualberta.ca").errorMessage())
    }

    /** US 01.07.03: an email without a dot after the @ is an error */
    @Test
    fun emailWithoutDotAfterAtSignIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan@ualberta").errorMessage())
    }

    /** US 01.07.03: an email with nothing before the @ is an error */
    @Test
    fun emailWithNothingBeforeAtSignIsAnError() {
        assertEquals(emailError, validUser.copy(email = "@ualberta.ca").errorMessage())
    }

    /** US 01.07.03: an email with two @ signs is an error */
    @Test
    fun emailWithTwoAtSignsIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan@@ualberta.ca").errorMessage())
    }

    /** US 01.07.03: an email with a space inside is an error */
    @Test
    fun emailWithSpaceInsideIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan lee@ualberta.ca").errorMessage())
    }

    /** US 01.07.03: the phone number is optional */
    @Test
    fun phoneIsOptional() {
        assertNull(validUser.copy(phone = "").errorMessage())
    }

    /** US 01.07.03: a phone number with dashes, spaces or brackets is accepted */
    @Test
    fun phoneWithFormattingIsAccepted() {
        assertNull(validUser.copy(phone = "(780) 555-0123").errorMessage())
        assertNull(validUser.copy(phone = "7805550123").errorMessage())
    }

    /** US 01.07.03: a phone number with too few digits is an error */
    @Test
    fun phoneWithTooFewDigitsIsAnError() {
        assertEquals(phoneError, validUser.copy(phone = "555-0123").errorMessage())
    }

    /** US 01.07.03: a phone number with too many digits is an error */
    @Test
    fun phoneWithTooManyDigitsIsAnError() {
        assertEquals(phoneError, validUser.copy(phone = "780-555-01234").errorMessage())
    }

    /** US 01.07.03: a phone number with letters is an error */
    @Test
    fun phoneWithLettersIsAnError() {
        assertEquals(phoneError, validUser.copy(phone = "780-555-012a").errorMessage())
    }
}