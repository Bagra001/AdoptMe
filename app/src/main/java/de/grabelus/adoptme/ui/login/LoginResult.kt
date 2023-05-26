package de.grabelus.adoptme.ui.login

/**
 * Authentication result : success (user details) or error message.
 */
data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: Int? = null,
    val email: String? = null,
    val userId: Int? = null
)