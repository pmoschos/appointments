package com.ai.appointments.model

import com.ai.appointments.db.models.Appointment
import java.util.UUID

data class P_My_AppointmentItem(
    val serviceName: String,
    val clinicName: String,
    val duration: String,
    val date: String,
    val time: String,
    val status: String,
    val appointmentId: String = UUID.randomUUID().toString() ,// Add unique ID
    val appointment: Appointment? = null
)