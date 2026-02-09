package com.ai.appointments.db.models

data class FilterSelection(
    val timeRange: String? = null,
    val category: String? = null,
    val status: String? = null,
    val dateRange: String? = null
)