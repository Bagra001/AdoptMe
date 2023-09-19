package de.grabelus.adoptme.data.entity

enum class Status(val displayName: String) {
    IN_VERIFICATION("In verification"),
    IN_REGISTRATION("In activation"),
    ACTIVE("Active"),
    INACTIVATED("Inactivated"),
    BLOCKED("Blocked"),
    DELETION_REQUESTED("Deletion requested"),
    IN_DELETION("In deletion"),
    DELETED("Deleted") // anonymization
}