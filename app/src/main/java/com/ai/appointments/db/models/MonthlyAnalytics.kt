package com.ai.appointments.db.models

data class MonthlyAnalytics(
    val month: String = "", // "2026-02"
    val totalAppointments: Int = 0,
    val totalDuration: Int = 0, // minutes
    val totalCost: Double = 0.0,
    val currency: String = "EUR",
    val byCategory: Map<String, CategoryAnalytics> = emptyMap() // key = category value
) {
    fun getTotalHours(): Double = totalDuration.toDouble() / 60.0
}