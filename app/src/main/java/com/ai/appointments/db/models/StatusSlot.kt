package com.ai.appointments.db.models

data class StatusSlot(
    val status: String,
    val statusValue: String = status.lowercase().replace(" ", "_"),
    val isSelected: Boolean = false
)