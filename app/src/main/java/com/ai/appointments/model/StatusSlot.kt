package com.ai.appointments.model


data class StatusSlot(
    val status: String,
    var isSelected: Boolean = false
)