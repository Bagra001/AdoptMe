package de.grabelus.adoptme.data

import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.data.model.RegisterUserData
import de.grabelus.adoptme.ui.register.RegisterResult

class UserService(private val userRepository: UserRepository) {

    // in-memory cache of the loggedInUser object
    private var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    fun register(email: String, username: String, password: String, repeatedPassword: String): Result<RegisterResult> {
        //TODO password encryption
        return userRepository.register(RegisterUserData(email, username, password));
    }

    fun login(email: String, password: String): Result<LoggedInUser> {
        return userRepository.loginWithMail(email, password)
    }

    fun loginWithUserId(email: String, userId: String) {
        val result = userRepository.login(email, userId)
        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }
    }

    fun logout() {
        user = null
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
    }
}