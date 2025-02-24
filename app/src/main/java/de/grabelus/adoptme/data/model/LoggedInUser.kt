package de.grabelus.adoptme.data.model

import de.grabelus.adoptme.data.entity.Sex
import de.grabelus.adoptme.data.entity.Status
import java.util.UUID

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val userId: String,
    val forName: String,
    val lastName: String,
    val userName: String,
    val email: String,
    val sex: Sex,
    val status: Status,
    val verified: Boolean
)