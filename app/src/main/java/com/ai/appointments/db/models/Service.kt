package com.ai.appointments.db.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Service(
    val providerId: String = "",
    var service_id: String = "",
    val name: String = "",
    val name_el: String = "",
    val description: String = "",
    val description_el: String = "",
    val category: String = ServiceCategory.HEALTH.value,
    val durationMin: Int = 30,
    val durationMax: Int = 60,
    val priceMin: Double = 0.0,
    val priceMax: Double = 0.0,
    val currency: String = "EUR",
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "providerId" to providerId,
        "service_id" to service_id,
        "name" to name,
        "name_el" to name_el,
        "description" to description,
        "description_el" to description_el,
        "category" to category,
        "durationMin" to durationMin,
        "durationMax" to durationMax,
        "priceMin" to priceMin,
        "priceMax" to priceMax,
        "currency" to currency,
        "imageUrl" to imageUrl,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    fun getDisplayName(language: String): String = if (language == "el") name_el else name
    fun getDescription(language: String): String = if (language == "el") description_el else description
    fun getCategoryDisplayName(language: String): String {
        val enum = ServiceCategory.fromValue(category)
        return if (language == "el") enum.displayName_el else enum.displayName
    }
    fun getPriceRange(): String = "$priceMin - $priceMax $currency"
    fun getDurationRange(): String = "$durationMin - $durationMax min"

}