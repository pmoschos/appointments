package com.ai.appointments.db.Repository

import android.util.Log
import com.ai.appointments.db.models.AvailabilitySlot
import com.ai.appointments.db.models.SlotGenerationConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

object AvailabilityRepository : BaseRepository() {
    // Updated to match your database structure

    private val availabilitySlotsRef = database.getReference("availability_slots")

    fun generateSlotsForDate(config: SlotGenerationConfig) {
        val providerSlotsRef = availabilityRef.child(config.providerId).child(config.date)

        // Clear existing slots for this date first
        providerSlotsRef.removeValue().addOnSuccessListener {
            val slots = generateTimeSlots(
                config.startTime,
                config.endTime,
                config.duration,
                config.bufferBetweenAppointments
            )

            slots.forEach { time ->
                providerSlotsRef.child(time).setValue(
                    AvailabilitySlot(
                        date = config.date,
                        time = time,
                        isAvailable = true,
                        serviceId = config.serviceId,
                        duration = config.duration
                    )
                )
            }
        }
    }

    private fun generateTimeSlots(
        startTime: String,
        endTime: String,
        duration: Int,
        buffer: Int
    ): List<String> {
        val slots = mutableListOf<String>()
        var currentTime = parseTimeToMinutes(startTime)
        val endTimeMinutes = parseTimeToMinutes(endTime)
        val slotDuration = duration + buffer

        while (currentTime + duration <= endTimeMinutes) {
            slots.add(formatMinutesToTime(currentTime))
            currentTime += slotDuration
        }
        return slots
    }

    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun formatMinutesToTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }


    fun getAvailableNonAvailableSlots(
        providerId: String,
        date: String, // "YYYY-MM-DD"
        serviceId: String,
        callback: (List<AvailabilitySlot>) -> Unit
    ) {
        println("Debug: Getting slots for provider: $providerId, date: $date")

        // First, try the old structure (availability_slots)
        availabilitySlotsRef.child(providerId).child(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        println("Debug: Found slots in availability_slots")
                        val slots = snapshot.children.mapNotNull {
                            it.getValue(AvailabilitySlot::class.java)
                        }.filter {it.serviceId == serviceId }
                        callback(slots)
                    } else {
                        // If not found, try the new structure (availability)
                        getSlotsFromAvailabilityNode(providerId, date, serviceId, callback)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Debug: Error checking availability_slots: ${error.message}")
                    // Try the new structure
                    getSlotsFromAvailabilityNode(providerId, date, serviceId, callback)
                }
            })
    }

    // In AvailabilityRepository.kt
    fun getAvailableSlots(
        providerId: String,
        date: String,
        serviceId: String,
        callback: (List<AvailabilitySlot>) -> Unit
    ) {
        println("Debug: Getting slots for provider: $providerId, date: $date")

        // First, try the old structure (availability_slots)
        availabilitySlotsRef.child(providerId).child(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        println("Debug: Found slots in availability_slots")
                        val slots = snapshot.children.mapNotNull {
                            it.getValue(AvailabilitySlot::class.java)
                        }.filter { it.serviceId == serviceId } // Filter by service but don't filter by availability
                        callback(slots)
                    } else {
                        // If not found, try the new structure (availability)
                        getSlotsFromAvailabilityNode(providerId, date, serviceId, callback)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Debug: Error checking availability_slots: ${error.message}")
                    // Try the new structure
                    getSlotsFromAvailabilityNode(providerId, date, serviceId, callback)
                }
            })
    }

    private fun getSlotsFromAvailabilityNode(
        providerId: String,
        date: String,
        serviceId: String,
        callback: (List<AvailabilitySlot>) -> Unit
    ) {
        println("Debug: Checking availability node for provider: $providerId, date: $date")

        availabilityRef.child(providerId).child(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("Debug: Snapshot exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")

                    if (!snapshot.exists()) {
                        println("Debug: No slots found for date $date")
                        callback(emptyList())
                        return
                    }

                    val slots = mutableListOf<AvailabilitySlot>()

                    for (timeSlot in snapshot.children) {
                        val time = timeSlot.key ?: continue

                        // Check if the slot data exists
                        if (timeSlot.value is Map<*, *>) {
                            val slotData = timeSlot.value as Map<*, *>
                            val isAvailable = slotData["isAvailable"] as? Boolean ?: true
                            val duration = (slotData["duration"] as? Long)?.toInt() ?: 30
                            val slotServiceId = slotData["serviceId"] as? String ?: serviceId
                            val appointmentId = slotData["appointmentId"] as? String ?: ""

                            // Always add the slot, regardless of availability
                            slots.add(
                                AvailabilitySlot(
                                    date = date,
                                    time = time,
                                    isAvailable = isAvailable,
                                    serviceId = slotServiceId,
                                    duration = duration,
                                    appointmentId = if (appointmentId.isNotEmpty()) appointmentId else null
                                )
                            )
                        } else {
                            // Simple structure: just the time slot
                            slots.add(
                                AvailabilitySlot(
                                    date = date,
                                    time = time,
                                    isAvailable = true,
                                    serviceId = serviceId,
                                    duration = 30 // Default duration
                                )
                            )
                        }
                    }

                    // Filter by serviceId but NOT by availability
                    val filteredSlots = slots.filter { it.serviceId == serviceId }

                    // Sort slots by time
                    val sortedSlots = filteredSlots.sortedBy { it.time }
                    println("Debug: Found ${sortedSlots.size} slots (including unavailable)")
                    sortedSlots.forEach { slot ->
                        println("Debug: Slot: ${slot.time}, available: ${slot.isAvailable}, appointmentId: ${slot.appointmentId}")
                    }
                    callback(sortedSlots)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Debug: Error getting availability: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    fun bookSlot(
        providerId: String,
        date: String,
        time: String,
        appointmentId: String,
        onSuccess: () -> Unit
    ) {
        Log.d("AvailabilityRepo", "Booking slot: provider=$providerId, date=$date, time=$time")

        val slotRef = availabilityRef.child(providerId).child(date).child(time)

        // First check if the slot exists
        slotRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Log.w("AvailabilityRepo", "Slot doesn't exist at path: ${slotRef.path}")
                onSuccess() // Still proceed with appointment creation
                return@addOnSuccessListener
            }

            // Check if slot is already booked
            val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: true
            if (!isAvailable) {
                Log.w("AvailabilityRepo", "Slot is already booked: $time on $date")
                onSuccess() // Still proceed with appointment
                return@addOnSuccessListener
            }

            // Prepare updates - maintain existing data and add appointmentId
            val updates = mutableMapOf<String, Any?>()

            // Copy all existing fields
            val value = snapshot.value
            if (value is Map<*, *>) {
                for ((key, v) in value) {
                    if (key is String) {
                        updates[key] = v
                    }
                }
            }

            // Update the necessary fields
            updates["isAvailable"] = false
            updates["appointmentId"] = appointmentId

            Log.d("AvailabilityRepo", "Updating slot with: $updates")

            // Apply the update
            slotRef.setValue(updates)
                .addOnSuccessListener {
                    Log.d("AvailabilityRepo", "Slot successfully booked and marked as unavailable")
                    onSuccess()
                }
                .addOnFailureListener { error ->
                    Log.e("AvailabilityRepo", "Failed to update slot: ${error.message}")
                    onSuccess() // Still proceed with appointment
                }
        }.addOnFailureListener { error ->
            Log.e("AvailabilityRepo", "Error checking slot: ${error.message}")
            onSuccess() // Still proceed with appointment
        }
    }

    // Helper method to get provider's working hours
    fun getProviderWorkingHours(providerId: String, callback: (Map<String, String>?) -> Unit) {
        database.getReference("users/providers/$providerId/workingHours")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val workingHours = mutableMapOf<String, String>()
                    for (day in snapshot.children) {
                        val dayName = day.key ?: continue
                        val hours = day.getValue(Map::class.java) as? Map<String, String>
                        if (hours != null) {
                            workingHours[dayName] = "${hours["start"]} - ${hours["end"]}"
                        }
                    }
                    callback(workingHours)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}