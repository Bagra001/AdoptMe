package de.grabelus.adoptme.ui.register

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.grabelus.adoptme.R
import de.grabelus.adoptme.data.Result
import de.grabelus.adoptme.data.UserService

class RegisterViewModel(private val userService: UserService) : ViewModel() {

    private val _registerForm = MutableLiveData<RegisterFormState>()
    val registerFormState: LiveData<RegisterFormState> = _registerForm

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    /**
     * Makes the registration of the user
     */
    fun register(email: String, username: String, password: String, repeatedPassword: String) {
        // can be launched in a separate asynchronous job
        val result = userService.register(email, username, password, repeatedPassword)

        if (result is Result.Success) {
            _registerResult.value =
                RegisterResult(success = true)
        } else {
            _registerResult.value = RegisterResult(error = R.string.registration_failed)
        }
    }

    /**
     * Makes the validation
     */
    fun registerDataChanged(
        email: String,
        username: String,
        password: String,
        repeatedPassword: String
    ) {
        val emailValid = isEmailValid(email)
        val passwordValid = isPasswordValid(password)
        val repeatedPasswordValid = isPasswordValid(repeatedPassword)
        val passwordSame = isPasswordTheSame(password, repeatedPassword)

        if (email.isNotEmpty() && !emailValid) {
            _registerForm.value = RegisterFormState(emailError = R.string.invalid_email)
        } else if (password.isNotEmpty() && !passwordValid) {
            _registerForm.value = RegisterFormState(passwordError = R.string.invalid_password)
        } else if (repeatedPassword.isNotEmpty() && !repeatedPasswordValid) {
            _registerForm.value =RegisterFormState(repeatedPasswordError = R.string.invalid_password)
        } else if (password.isNotEmpty() && repeatedPassword.isNotEmpty() && !passwordSame) {
            _registerForm.value = RegisterFormState(
                    repeatedPasswordError = R.string.not_same_password
                )
        } else {
            _registerForm.value = RegisterFormState(isDataValid = true)
        }
    }

    /**
     * email check
     */
    private fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * password check
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    /**
     * checks if the two passwords are the same
     */
    private fun isPasswordTheSame(password: String, repeatedPassword: String): Boolean {
        return password.isNotBlank() && repeatedPassword.isNotBlank() && password == repeatedPassword
    }
}