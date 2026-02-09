package com.ai.appointments.db.models

enum class ServiceCategory(val value: String, val displayName: String, val displayName_el: String) {
    HEALTH("health", "Health", "Υγεία"),
    WELLNESS("wellness", "Wellness", "Ευεξία"),
    TECHNICAL("technical", "Technical Services", "Τεχνικές Υπηρεσίες"),
    EDUCATIONAL("educational", "Educational", "Εκπαιδευτικές"),
    AUTO("auto", "Auto Services", "Αυτοκίνητο");

    companion object {
        fun fromValue(value: String) = values().firstOrNull { it.value == value } ?: HEALTH
    }
}

