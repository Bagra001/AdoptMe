package de.grabelus.adoptme.data

import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.data.model.RegisterUserData
import de.grabelus.adoptme.ui.register.RegisterResult

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class UserRepository(val dataSource: UserDataSource) {

    fun register(registerUserData: RegisterUserData): Result<RegisterResult> {
        return dataSource.register(registerUserData)
    }

    fun login(email: String, userId: String): Result<LoggedInUser> {
        return dataSource.login(email, userId)
    }

    fun loginWithMail(email: String, password: String) Result<LoggedInUser> {
        return dataSource.loginWithMail(email, password)
    }

    fun getUserId(email: String): String {
        return dataSource.getUserId(email)
    }
}