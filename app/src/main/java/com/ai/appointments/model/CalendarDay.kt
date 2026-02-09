package com.ai.appointments.model

data class CalendarDay(
    val dayName: String,
    val dayNumber: String,
    val dateString: String, // Add this for actual date storage
    val isToday: Boolean = false,
    val isSelected: Boolean = false
)