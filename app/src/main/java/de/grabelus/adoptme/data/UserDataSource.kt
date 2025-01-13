package de.grabelus.adoptme.data

import de.grabelus.adoptme.data.entity.User
import de.grabelus.adoptme.data.model.LoggedInUser
import de.grabelus.adoptme.data.model.RegisterUserData
import de.grabelus.adoptme.ui.register.RegisterResult
import io.realm.Realm
import io.realm.RealmConfiguration
import java.io.IOException
import java.util.logging.Logger


/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class UserDataSource {

    var logger: Logger = Logger.getLogger("UserDataSource")

    private val realmName: String = "User"
    private val config: RealmConfiguration = RealmConfiguration.Builder().name(realmName).allowWritesOnUiThread(true).build()
    private val backgroundThreadRealm : Realm = Realm.getInstance(config)

    fun register(registerUserData: RegisterUserData): Result<RegisterResult> {
        val user = User(nextID(User::class.java))
        user.email = registerUserData.email
        user.username = registerUserData.username
        user.password = registerUserData.password

        try {
            backgroundThreadRealm.executeTransaction { transactionRealm ->
                transactionRealm.insert(user)
            }
        } catch (e: Exception) {
            logger.throwing("UserDataSource", "register", e)
            return Result.Error(e)
        }
        return Result.Success(RegisterResult(true))
    }

    fun login(email: String, password: String): Result<LoggedInUser> {
        try {
            var user: User? = null
            backgroundThreadRealm.executeTransaction { transactionRealm ->
                user = transactionRealm.where(User::class.java).equalTo("email", email).findFirst()
            }
            if(user == null) {
                throw IllegalAccessException("User does not exists")
            }
            return Result.Success(LoggedInUser(user!!.id, user!!.forname, user!!.lastname, user!!.username, user!!.email, user!!.sexEnum, user!!.statusEnum, user!!.verified))
        } catch (e: Exception) {
            logger.throwing("UserDataSource", "login", e)
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }

    private fun nextID(table: Class<User>?): Int {
        return (table?.let { backgroundThreadRealm.where(it).findAll().max("id")?.toInt() } ?: 0) + 1
    }
}