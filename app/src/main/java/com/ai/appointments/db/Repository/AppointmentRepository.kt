package com.ai.appointments.db.Repository

import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.models.AppointmentStatus
import com.ai.appointments.db.models.CategoryAnalytics
import com.ai.appointments.db.models.MonthlyAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

object AppointmentRepository : BaseRepository() {

    fun createAppointment(
        appointment: Appointment,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val newAppointmentRef = appointmentsRef.push()
        val appointmentId = newAppointmentRef.key ?: throw IllegalStateException("Failed to generate appointment ID")
        val appointmentWithId = appointment.copy(id = appointmentId)

        // Transactional write with indexes
        newAppointmentRef.setValue(appointmentWithId).addOnSuccessListener {
            // Update indexes
            appointmentsByUserRef.child(appointment.userId).child(appointmentId)
                .setValue(appointment.appointmentDate)
            appointmentsByProviderRef.child(appointment.providerId).child(appointmentId)
                .setValue(appointment.appointmentDate)

            // Update analytics
            updateAnalytics(appointment)
            onSuccess(appointmentId)
        }.addOnFailureListener(onError)
    }

    private fun updateAnalytics(appointment: Appointment) {
        if (appointment.status != AppointmentStatus.COMPLETED.value) return

        val monthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            .format(java.util.Date(appointment.completedAt ?: appointment.appointmentDate))

        val analyticsPath = analyticsRef.child(appointment.userId).child(monthKey)
        analyticsPath.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val current = mutableData.getValue(MonthlyAnalytics::class.java)
                    ?: MonthlyAnalytics(month = monthKey, currency = appointment.currency)

                val updated = current.copy(
                    totalAppointments = current.totalAppointments + 1,
                    totalDuration = current.totalDuration + appointment.duration,
                    totalCost = current.totalCost + appointment.price,
                    byCategory = updateCategoryAnalytics(
                        current.byCategory,
                        appointment.category,
                        appointment.duration,
                        appointment.price
                    )
                )
                mutableData.value = updated
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) = Unit
        })
    }

    private fun updateCategoryAnalytics(
        current: Map<String, CategoryAnalytics>,
        category: String,
        duration: Int,
        cost: Double
    ): Map<String, CategoryAnalytics> {
        val existing = current[category] ?: CategoryAnalytics()
        return current.toMutableMap().apply {
            this[category] = existing.copy(
                count = existing.count + 1,
                duration = existing.duration + duration,
                cost = existing.cost + cost
            )
        }
    }

    fun getUserAppointments(
        userId: String = getCurrentUserId(),
        startDate: Long? = null,
        endDate: Long? = null,
        statusFilter: String? = null,
        categoryFilter: String? = null,
        callback: (List<Appointment>) -> Unit
    ) {
        appointmentsByUserRef.child(userId).get().addOnSuccessListener { indexSnapshot ->
            val appointmentIds = indexSnapshot.children.mapNotNull {
                val appointmentDate = it.value as? Long ?: return@mapNotNull null

                // Filter by start date
                if (startDate != null && appointmentDate < startDate) return@mapNotNull null

                // Filter by end date
                if (endDate != null && appointmentDate > endDate) return@mapNotNull null

                it.key
            }

            fetchAppointmentsWithFilters(
                appointmentIds,
                statusFilter,
                categoryFilter,
                callback
            )
        }
    }

    private fun fetchAppointmentsWithFilters(
        ids: List<String>,
        statusFilter: String?,
        categoryFilter: String?,
        callback: (List<Appointment>) -> Unit
    ) {
        if (ids.isEmpty()) {
            callback(emptyList())
            return
        }

        val appointments = mutableListOf<Appointment>()
        var remaining = ids.size

        ids.forEach { id ->
            appointmentsRef.child(id).get().addOnSuccessListener { snapshot ->
                snapshot.getValue(Appointment::class.java)?.let { appointment ->
                    if (statusFilter == null || appointment.status == statusFilter) {
                        if (categoryFilter == null || appointment.category == categoryFilter) {
                            appointments.add(appointment)
                        }
                    }
                }
                if (--remaining == 0) callback(appointments.sortedByDescending { it.appointmentDate })
            }
        }
    }

    fun cancelAppointment(
        appointmentId: String,
        cancelledBy: String, // "user" or "provider"
        reason: String?,
        onSuccess: () -> Unit
    ) {
        val appointmentRef = appointmentsRef.child(appointmentId)
        appointmentRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val appointment = mutableData.getValue(Appointment::class.java)
                    ?: return Transaction.abort()

                if (appointment.status != AppointmentStatus.CONFIRMED.value) {
                    return Transaction.abort()
                }

                val now = System.currentTimeMillis()
                mutableData.value = appointment.copy(
                    status = AppointmentStatus.CANCELLED.value,
                    cancelledAt = now,
                    cancelledBy = cancelledBy,
                    cancelledReason = reason,
                    updatedAt = now
                )
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (committed) {
                    // Release the slot back to availability
                    val appt = dataSnapshot?.getValue(Appointment::class.java)
                    appt?.let {
                        AvailabilityRepository.bookSlot(
                            it.providerId,
                            it.scheduledDateTime.substring(0, 10), // Extract date
                            it.scheduledDateTime.substring(11, 16), // Extract time
                            appointmentId = "", // Empty to mark as available
                            onSuccess = onSuccess
                        )
                    } ?: onSuccess()
                }
            }
        })
    }

    fun deleteAppointment(
        appointmentId: String,
        providerId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // First, get the appointment details to release the slot
        appointmentsRef.child(appointmentId).get().addOnSuccessListener { snapshot ->
            val appointment = snapshot.getValue(Appointment::class.java)

            val updates = mutableMapOf<String, Any?>()

            // 1. Remove from appointments
            updates["appointments/$appointmentId"] = null

            // 2. Remove from provider indexes
            updates["indexes/appointments_by_provider/$providerId/$appointmentId"] = null

            // 3. Remove from user indexes
            updates["indexes/appointments_by_user/$userId/$appointmentId"] = null

            // 4. Release the time slot if appointment exists
            appointment?.let { appt ->
                val scheduledDateTime = appt.scheduledDateTime
                if (scheduledDateTime.isNotEmpty() && scheduledDateTime.contains(" ")) {
                    val parts = scheduledDateTime.split(" ")
                    if (parts.size >= 2) {
                        val date = parts[0]
                        val time = parts[1]

                        // Add slot update to mark as available
                        updates["availability/${appt.providerId}/$date/$time"] = mapOf(
                            "isAvailable" to true,
                            "appointmentId" to null,
                            "duration" to appt.duration,
                            // Note: We're not including appointmentId
                        )
                    }
                }
            }



            // Apply all updates in batch
            database.reference.updateChildren(updates)
                .addOnSuccessListener {
                    println("DEBUG: Appointment deleted successfully")
                    onSuccess()
                }
                .addOnFailureListener { error ->
                    println("ERROR: Failed to delete appointment: ${error.message}")
                    onError(error as? Exception ?: Exception(error.message))
                }

        }.addOnFailureListener { error ->
            onError(error as? Exception ?: Exception(error.message))
        }
    }

    fun getUserMonthlyAnalytics(
        userId: String = getCurrentUserId(),
        month: String, // "2026-02"
        callback: (MonthlyAnalytics?) -> Unit
    ) {
        analyticsRef.child(userId).child(month).get().addOnSuccessListener { snapshot ->
            callback(snapshot.getValue(MonthlyAnalytics::class.java))
        }
    }

    // Helper method for analytics deletion (optional - if you want to handle analytics)

    private fun updateCategoryAnalyticsForDeletion(
        current: Map<String, CategoryAnalytics>,
        category: String,
        duration: Int,
        cost: Double
    ): Map<String, CategoryAnalytics> {
        val existing = current[category] ?: return current

        return current.toMutableMap().apply {
            this[category] = existing.copy(
                count = (existing.count - 1).coerceAtLeast(0),
                duration = (existing.duration - duration).coerceAtLeast(0),
                cost = (existing.cost - cost).coerceAtLeast(0.0)
            )

            // Remove category if count is 0
            if (this[category]?.count == 0) {
                remove(category)
            }
        }
    }
}