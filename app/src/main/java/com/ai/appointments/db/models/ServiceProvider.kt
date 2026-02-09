package com.ai.appointments.db.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ServiceProvider(
    val firstName: String = "",
    val lastName: String = "",
    val userId: String="",
    val email: String = "",
    val dateOfBirth: Long = 0,
    val language: String = "en",
    val profileImageUrl: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val notificationsEnabled: Boolean = true,
    val businessInfo: BusinessInfo = BusinessInfo(),
    val workingHours: Map<String, WorkingHours> = mapOf(),
    val isActive: Boolean = true
) {
    fun toMap(): Map<String, Any?> {
        val workingHoursMap = workingHours.mapValues { it.value.toMap() }
        return mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "userId" to userId,
            "dateOfBirth" to dateOfBirth,
            "language" to language,
            "profileImageUrl" to profileImageUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "notificationsEnabled" to notificationsEnabled,
            "businessInfo" to businessInfo.toMap(),
            "workingHours" to workingHoursMap,
            "isActive" to isActive
        )
    }
}