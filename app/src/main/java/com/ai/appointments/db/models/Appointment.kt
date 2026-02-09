package com.ai.appointments.db.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Appointment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerName_en: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val serviceName_el: String = "",
    val category: String = ServiceCategory.HEALTH.value,
    val appointmentDate: Long = 0,
    val scheduledDateTime: String = "",
    val duration: Int = 0,
    val price: Double = 0.0,
    val currency: String = "EUR",
    val status: String = AppointmentStatus.CONFIRMED.value,
    val notes: String = "",
    val notes_en: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val completedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancelledBy: String? = null,
    val cancelledReason: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "providerId" to providerId,
        "providerName" to providerName,
        "providerName_en" to providerName_en,
        "serviceId" to serviceId,
        "serviceName" to serviceName,
        "serviceName_el" to serviceName_el,
        "category" to category,
        "appointmentDate" to appointmentDate,
        "scheduledDateTime" to scheduledDateTime,
        "duration" to duration,
        "price" to price,
        "currency" to currency,
        "status" to status,
        "notes" to notes,
        "notes_en" to notes_en,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "completedAt" to completedAt,
        "cancelledAt" to cancelledAt,
        "cancelledBy" to cancelledBy,
        "cancelledReason" to cancelledReason
    )

    fun getStatusEnum(): AppointmentStatus = AppointmentStatus.fromValue(status)
    fun getServiceDisplayName(language: String): String =
        if (language == "el") serviceName_el else serviceName

    fun getProviderDisplayName(language: String): String =
        if (language == "el") providerName else providerName_en.ifEmpty { providerName }

    fun getNotes(language: String): String =
        if (language == "el") notes else notes_en.ifEmpty { notes }

    fun getCategoryDisplayName(language: String): String {
        val enum = ServiceCategory.fromValue(category)
        return if (language == "el") enum.displayName_el else enum.displayName
    }

    fun isPastAppointment(): Boolean = appointmentDate < System.currentTimeMillis()
    fun isUpcoming(): Boolean = status == AppointmentStatus.CONFIRMED.value &&
            appointmentDate > System.currentTimeMillis()
}