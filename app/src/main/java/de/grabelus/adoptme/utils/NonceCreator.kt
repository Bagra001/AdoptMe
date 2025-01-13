package de.grabelus.adoptme.utils

import java.security.MessageDigest
import java.util.UUID

object NonceCreator {
    fun createNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawNonce.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it)}
    }
}