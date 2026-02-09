package com.ai.appointments.db.models

data class AvailabilitySlot(
    val date: String = "", // Format: "YYYY-MM-DD"
    val time: String = "", // Format: "HH:mm"
    val isAvailable: Boolean = true,
    val serviceId: String = "",
    val duration: Int = 0, // minutes
    val appointmentId: String? = null // null if available
)

// Helper for slot generation
data class SlotGenerationConfig(
    val providerId: String,
    val date: String, // "YYYY-MM-DD"
    val serviceId: String,
    val duration: Int,
    val startTime: String, // "09:00"
    val endTime: String,   // "18:00"
    val bufferBetweenAppointments: Int = 15 // minutes
)