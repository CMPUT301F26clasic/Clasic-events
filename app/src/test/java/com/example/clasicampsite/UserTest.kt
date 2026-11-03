// Purpose: Unit tests for User's validation (US 01.07.03).

package com.example.clasicampsite

import com.example.clasicampsite.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests User.errorMessage(). */
class UserTest {

    private val validUser = User(
        id = "device-1",
        name = "Bob Duncan",
        email = "bob.duncan@ualberta.ca",
        phone = "123-456-7890",
    )

    private val emailError = "Enter a valid email, like name@example.com"
    private val phoneError = "Phone number must have 10 digits"

    @Test
    fun validUserHasNoError() {
        assertNull(validUser.errorMessage())
    }

    @Test
    fun newUserHasNotificationsOn() {
        assertEquals(true, User().notificationsEnabled)
    }

    @Test
    fun blankNameIsAnError() {
        assertEquals("Name is required", validUser.copy(name = "").errorMessage())
        assertEquals("Name is required", validUser.copy(name = "   ").errorMessage())
    }

    @Test
    fun emailWithSpacesAroundIsAccepted() {
        assertNull(validUser.copy(email = "  jordan@example.com  ").errorMessage())
    }

    @Test
    fun emailWithoutAtSignIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan.ualberta.ca").errorMessage())
    }

    @Test
    fun emailWithoutDotAfterAtSignIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan@ualberta").errorMessage())
    }

    @Test
    fun emailWithNothingBeforeAtSignIsAnError() {
        assertEquals(emailError, validUser.copy(email = "@ualberta.ca").errorMessage())
    }

    @Test
    fun emailWithTwoAtSignsIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan@@ualberta.ca").errorMessage())
    }

    @Test
    fun emailWithSpaceInsideIsAnError() {
        assertEquals(emailError, validUser.copy(email = "jordan lee@ualberta.ca").errorMessage())
    }

    @Test
    fun phoneIsOptional() {
        assertNull(validUser.copy(phone = "").errorMessage())
    }

    @Test
    fun phoneWithFormattingIsAccepted() {
        assertNull(validUser.copy(phone = "(780) 555-0123").errorMessage())
        assertNull(validUser.copy(phone = "7805550123").errorMessage())
    }

    @Test
    fun phoneWithTooFewDigitsIsAnError() {
        assertEquals(phoneError, validUser.copy(phone = "555-0123").errorMessage())
    }

    @Test
    fun phoneWithTooManyDigitsIsAnError() {
        assertEquals(phoneError, validUser.copy(phone = "780-555-01234").errorMessage())
    }

    @Test
    fun phoneWithLettersIsAnError() {
        assertEquals(phoneError, validUser.copy(phone = "780-555-012a").errorMessage())
    }
}