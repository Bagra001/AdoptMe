package de.grabelus.adoptme.ui.register

import de.grabelus.adoptme.ui.login.LoggedInUserView

data class RegisterResult(
    val success: Boolean? = false,
    val error: Int? = null
)