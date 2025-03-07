package de.grabelus.adoptme.utils

import android.util.Patterns
import de.grabelus.adoptme.R

class InputFieldValidator {
    companion object {
        private val passwordRegex: Regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).+$")

        fun emailValid(email: String): String? {
            var errorText: String? = null
            if (email.isBlank()) {
                errorText = R.string.empty_email.toString()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                errorText = R.string.invalid_email.toString()
            }
            return errorText
        }

        fun passwordValid(password: String, strengthValidation: Boolean = false): String? {
            var errorText: String? = null
            if (password.isBlank()) {
                errorText = R.string.empty_password.toString()
            } else if (password.length <= 8) {
                errorText = R.string.invalid_password_length.toString()
            } else if(strengthValidation && passwordRegex.matches(password)) {
                errorText = R.string.invalid_password.toString()
            }
            return errorText
        }
    }
}