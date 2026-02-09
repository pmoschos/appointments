package com.ai.appointments.db.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class WorkingHours(
    val start: String = "09:00",
    val end: String = "17:00"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "start" to start,
        "end" to end
    )
}