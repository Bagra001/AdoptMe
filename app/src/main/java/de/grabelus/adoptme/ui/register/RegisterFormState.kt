package de.grabelus.adoptme.ui.register

data class RegisterFormState(
    val emailError: Int? = null,
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val repeatedPasswordError: Int? = null,
    val isDataValid: Boolean = false
)