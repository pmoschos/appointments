package com.ai.appointments.model

data class ServiceDetailsItem(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val duration: String = "",
    val category: String = "",
    val price: String = "",
    val imageUrl: String = "", // or use R.drawable.xxx if using resources
)