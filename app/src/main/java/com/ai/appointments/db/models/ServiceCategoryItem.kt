package com.ai.appointments.db.models

data class ServiceCategoryItem(
    val name: String,
    val value: String,
    val isSelected: Boolean = false
)