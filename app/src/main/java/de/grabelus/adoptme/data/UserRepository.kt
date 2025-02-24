package de.grabelus.adoptme.data

import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.data.model.RegisterUserData
import de.grabelus.adoptme.ui.register.RegisterResult

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class UserRepository(val dataSource: UserDataSource) {

    // in-memory cache of the loggedInUser object
    private var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    fun register(registerUserData: RegisterUserData): Result<RegisterResult> {
        return dataSource.register(registerUserData)
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    fun login(email: String, userId: String): Result<LoggedInUser> {
        val result = dataSource.login(email, userId)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }
        return result;
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
    }

    fun getUserId(email: String): String {
        return dataSource.getUserId(email)
    }
}