package de.grabelus.adoptme.data.model

import de.grabelus.adoptme.data.entity.Sex
import de.grabelus.adoptme.data.entity.Status

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val userId: Int,
    val forname: String,
    val lastname: String,
    val username: String,
    val email: String,
    val sex: Sex,
    val status: Status,
    val verified: Boolean
)