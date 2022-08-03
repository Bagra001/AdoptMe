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

    fun registerDataChanged(
        email: String,
        username: String,
        password: String,
        repeatedPassword: String
    ) {
        if (email.isNotEmpty()) {
            if (!isEmailValid(email)) {
                _registerForm.value = RegisterFormState(emailError = R.string.invalid_username)
            }
        } else if (username.isNotEmpty()) {
            if (!isUserNameValid(username)) {
                _registerForm.value = RegisterFormState(usernameError = R.string.invalid_username)
            }
        } else if (password.isNotEmpty()) {
            if (!isPasswordValid(password)) {
                _registerForm.value = RegisterFormState(passwordError = R.string.invalid_password)
            }
        } else if(repeatedPassword.isNotEmpty()) {
            if (!isPasswordValid(repeatedPassword)) {
                _registerForm.value = RegisterFormState(repeatedPasswordError = R.string.invalid_password)
            }
        } else if(password.isNotEmpty() && repeatedPassword.isNotEmpty()) {
            if (!isPasswordTheSame(password, repeatedPassword)) {
                _registerForm.value = RegisterFormState(passwordError = R.string.not_same_password)
            }
        } else {
            _registerForm.value = RegisterFormState(isDataValid = true)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    private fun isPasswordTheSame(password: String, repeatedPassword: String): Boolean {
        return password.isNotBlank() && repeatedPassword.isNotBlank() && password == repeatedPassword
    }
}