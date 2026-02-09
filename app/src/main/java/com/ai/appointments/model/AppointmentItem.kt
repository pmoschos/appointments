package com.ai.appointments.model

import com.ai.appointments.db.models.Appointment

data class AppointmentItem(
    val serviceName: String,
    val clinicName: String,
    val date: String,
    val time: String,
    val notes: String,
    val appointment: Appointment? = null
)