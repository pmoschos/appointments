package com.ai.appointments.db.models

data class AppointmentHistoricity(
    val serviceName: String,
    val category: String,
    val duration: String,
    val date: String,
    val price: String,
    val appointment: Appointment? = null, // Store original appointment for reference
    val imageUrl: String = "" // For provider/service image
)