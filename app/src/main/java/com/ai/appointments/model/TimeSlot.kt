package com.ai.appointments.model


data class TimeSlot(
    val time: String, // Display time (e.g., "9:00 AM")
    val originalTime: String, // Original time in 24h format (e.g., "09:00")
    val isAvailable: Boolean = true,
    val isSelected: Boolean = false,
    val value: String = ""
)