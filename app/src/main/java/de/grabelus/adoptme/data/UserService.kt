package de.grabelus.adoptme.data

import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.data.model.RegisterUserData
import de.grabelus.adoptme.ui.register.RegisterResult

class UserService(private val userRepository: UserRepository) {

    fun register(email: String, username: String, password: String, repeatedPassword: String): Result<RegisterResult> {
        //TODO password encryption
        return userRepository.register(RegisterUserData(email, username, password));
    }

    fun login(username: String, password: String): Result<LoggedInUser> {
        return userRepository.login(username, password)
    }
}