package com.ai.appointments.db.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class BusinessInfo(
    val businessName: String = "",
    val businessName_en: String = "",
    val specialty: String = "",
    val specialty_en: String = "",
    val address: String = "",
    val address_en: String = "",
    val phone: String = "",
    val bio: String = "",
    val bio_en: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "businessName" to businessName,
        "businessName_en" to businessName_en,
        "specialty" to specialty,
        "specialty_en" to specialty_en,
        "address" to address,
        "address_en" to address_en,
        "phone" to phone,
        "bio" to bio,
        "bio_en" to bio_en
    )
}