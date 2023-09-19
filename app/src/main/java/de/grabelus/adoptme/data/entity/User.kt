package de.grabelus.adoptme.data.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class User(): RealmObject() {
    @PrimaryKey
    var id: Int = 0
    @Required
    var password: String = ""
    @Required
    var username: String = ""
    var forname: String = ""
    var lastname: String = ""
    @Required
    var email: String = ""
    var sex: String = ""
    var sexEnum: Sex
        get() {
            // because status is actually a String and another client could assign an invalid value,
            // default the status to "Open" if the status is unreadable
            return try {
                Sex.valueOf(status)
            } catch (e: IllegalArgumentException) {
                Sex.Divers
            }
        }
        set(value) { status = value.name }
    @Required
    var status: String = ""
    var statusEnum: Status
        get() {
            // because status is actually a String and another client could assign an invalid value,
            // default the status to "Open" if the status is unreadable
            return try {
                Status.valueOf(status)
            } catch (e: IllegalArgumentException) {
                Status.IN_VERIFICATION
            }
        }
        set(value) { status = value.name }
    var verified: Boolean = false

    constructor(id: Int = 0) : this() {
        this.id = id
    }
}
