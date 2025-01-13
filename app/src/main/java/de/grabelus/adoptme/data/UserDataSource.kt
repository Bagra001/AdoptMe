package de.grabelus.adoptme.data

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import de.grabelus.adoptme.data.entity.User
import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.data.model.RegisterUserData
import de.grabelus.adoptme.ui.register.RegisterResult
import java.io.IOException
import java.util.logging.Logger


/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class UserDataSource {

    var logger: Logger = Logger.getLogger("UserDataSource")

    fun register(registerUserData: RegisterUserData): Result<RegisterResult> {
        return Result.Success(RegisterResult(true))
    }

    fun login(email: String?, uid: String?): Result<LoggedInUser> {
        // TODO get user and fill with data
        // TODO check which db you should use (realm or other)
        val result: Result<LoggedInUser>
        try {
            if(email != null) {
                var user: User? = null
                result = Result.Success(
                    LoggedInUser(
                        user!!.id,
                        user!!.forname,
                        user!!.lastname,
                        user!!.username,
                        user!!.email,
                        user!!.sexEnum,
                        user!!.statusEnum,
                        user!!.verified
                    )
                )
            } else {
                result = Result.Error(IOException("email was empty"))
            }
        } catch (e: Exception) {
            logger.throwing("UserDataSource", "login", e)
            return Result.Error(IOException("Error logging in", e))
        }
        return result
    }

    fun logout() {
        Firebase.auth.signOut()
    }
}