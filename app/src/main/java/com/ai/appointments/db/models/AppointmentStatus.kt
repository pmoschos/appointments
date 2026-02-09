package com.ai.appointments.db.models

enum class AppointmentStatus(val value: String) {
    CONFIRMED("confirmed"),
    CANCELLED("cancelled"),
    COMPLETED("completed"),
    NO_SHOW("no_show");

    companion object {
        fun fromValue(value: String) = values().firstOrNull { it.value == value } ?: CONFIRMED
    }
}