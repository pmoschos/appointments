package com.ai.appointments.db.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class NormalUser(
    val firstName: String = "",
    val lastName: String = "",
    val userId: String="",
    val email: String = "",
    val dateOfBirth: Long = 0,
    val language: String = "en",
    val profileImageUrl: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val notificationsEnabled: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "userId" to userId,
        "email" to email,
        "dateOfBirth" to dateOfBirth,
        "language" to language,
        "profileImageUrl" to profileImageUrl,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "notificationsEnabled" to notificationsEnabled
    )
}