package com.ai.appointments.db.models

data class BaseUser(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val dateOfBirth: Long = 0,
    val language: String = "en", // "en" or "el"
    val profileImageUrl: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val notificationsEnabled: Boolean = true
)