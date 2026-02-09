package com.ai.appointments.db.utils


import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.models.AppointmentStatus
import com.ai.appointments.db.models.NormalUser
import com.ai.appointments.db.models.Service
import com.ai.appointments.db.models.ServiceProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.google.firebase.auth.FirebaseAuth
import java.util.Date

class DatabaseHelper private constructor() {
    val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // References
    private val usersRef = database.getReference("users")
    val normalUsersRef = usersRef.child("normal")
    val providersRef = usersRef.child("providers")
    val servicesRef = database.getReference("services")
    val appointmentsRef = database.getReference("appointments")
    val availabilityRef = database.getReference("availability")
    private val indexesRef = database.getReference("indexes")
    val appointmentsByProviderRef = indexesRef.child("appointments_by_provider")
    private val servicesByProviderRef = indexesRef.child("services_by_provider")

    // Singleton
    companion object {
        @Volatile private var instance: DatabaseHelper? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: DatabaseHelper().also { instance = it }
        }
    }

    // ==============================
    // USER MANAGEMENT
    // ==============================
    fun getUserRole(uid: String, callback: (RoleType) -> Unit) {
        normalUsersRef.child(uid).get().addOnSuccessListener { normalSnapshot ->
            if (normalSnapshot.exists()) {
                callback(RoleType.NORMAL_USER)
                return@addOnSuccessListener
            }
            providersRef.child(uid).get().addOnSuccessListener { providerSnapshot ->
                callback(if (providerSnapshot.exists()) RoleType.SERVICE_PROVIDER else RoleType.UNKNOWN)
            }.addOnFailureListener { callback(RoleType.UNKNOWN) }
        }.addOnFailureListener { callback(RoleType.UNKNOWN) }
    }

    fun saveNormalUser(uid: String, user: NormalUser) {
        normalUsersRef.child(uid).setValue(user.toMap())
    }

    fun saveServiceProvider(uid: String, provider: ServiceProvider) {
        providersRef.child(uid).setValue(provider.toMap())
    }

    fun getServiceProvider(uid: String, callback: (ServiceProvider?) -> Unit) {
        providersRef.child(uid).get().addOnSuccessListener { snapshot ->
            callback(snapshot.getValue(ServiceProvider::class.java))
        }.addOnFailureListener { callback(null) }
    }

    // ==============================
    // SERVICE MANAGEMENT
    // ==============================
    fun createService(service: Service, providerId: String = getCurrentUserId(), onSuccess: (String) -> Unit) {
        val newServiceRef = servicesRef.push()
        val serviceId = newServiceRef.key ?: throw IllegalStateException("Failed to generate service ID")
        service.service_id=serviceId;
        newServiceRef.setValue(service.toMap()).addOnSuccessListener {
            // Update indexes
            servicesByProviderRef.child(providerId).child(serviceId).setValue(true)
            onSuccess(serviceId)
        }
    }

    fun getProviderServices(providerId: String, callback: (List<Service>) -> Unit) {
        servicesByProviderRef.child(providerId).get().addOnSuccessListener { snapshot ->
            val serviceIds = snapshot.children.mapNotNull { it.key }
            if (serviceIds.isEmpty()) {
                callback(emptyList())
                return@addOnSuccessListener
            }

            val services = mutableListOf<Service>()
            var remaining = serviceIds.size

            serviceIds.forEach { serviceId ->
                servicesRef.child(serviceId).get().addOnSuccessListener { serviceSnapshot ->
                    serviceSnapshot.getValue(Service::class.java)?.let { services.add(it) }
                    if (--remaining == 0) callback(services.sortedByDescending { it.createdAt })
                }
            }
        }
    }

    fun updateService(serviceId: String, service: Service, onSuccess: () -> Unit) {
        servicesRef.child(serviceId).updateChildren(service.toMap()).addOnSuccessListener {
            onSuccess()
        }
    }

    fun deleteService(serviceId: String, providerId: String, onSuccess: () -> Unit) {
        // Delete service document
        servicesRef.child(serviceId).removeValue().addOnSuccessListener {
            // Remove from indexes
            servicesByProviderRef.child(providerId).child(serviceId).removeValue().addOnSuccessListener {
                onSuccess()
            }
        }
    }

    // ==============================
    // APPOINTMENT MANAGEMENT
    // ==============================
    fun createAppointment(appointment: Appointment, onSuccess: (String) -> Unit) {
        val newAppointmentRef = appointmentsRef.push()
        val appointmentId = newAppointmentRef.key ?: throw IllegalStateException("Failed to generate appointment ID")
        val appointmentWithId = appointment.copy(id = appointmentId)

        newAppointmentRef.setValue(appointmentWithId.toMap()).addOnSuccessListener {
            // Update indexes
            appointmentsByProviderRef.child(appointment.providerId).child(appointmentId)
                .setValue(appointment.appointmentDate)
            appointmentsRef.child("indexes/appointments_by_user")
                .child(appointment.userId).child(appointmentId)
                .setValue(appointment.appointmentDate)
            onSuccess(appointmentId)
        }
    }

    fun getProviderAppointments(
        providerId: String,
        startDate: Long? = null,
        endDate: Long? = null,
        statusFilter: String? = null,
        callback: (List<Appointment>) -> Unit
    ) {
        println("DATABASE HELPER: Fetching appointments for provider: $providerId")

        // Try to get from the index first
        appointmentsByProviderRef.child(providerId).get().addOnSuccessListener { indexSnapshot ->
            if (!indexSnapshot.exists()) {
                println("DATABASE HELPER: No index found for provider")
                callback(emptyList())
                return@addOnSuccessListener
            }

            println("DATABASE HELPER: Index has ${indexSnapshot.childrenCount} entries")

            val appointmentIds = mutableListOf<String>()
            val appointmentDates = mutableMapOf<String, Long>()

            indexSnapshot.children.forEach { child ->
                val appointmentId = child.key ?: return@forEach
                val appointmentDate = child.getValue(Long::class.java) ?: 0L
                appointmentIds.add(appointmentId)
                appointmentDates[appointmentId] = appointmentDate

                println("DATABASE HELPER: Index entry - ID: $appointmentId, Date: ${
                    Date(
                        appointmentDate
                    )
                }")
            }

            // Fetch appointment details
            fetchAppointmentDetails(appointmentIds, appointmentDates, startDate, endDate, statusFilter, callback)

        }.addOnFailureListener { error ->
            println("DATABASE HELPER: Error getting index: ${error.message}")
            // Fallback to direct query
            queryAppointmentsDirectly(providerId, startDate, endDate, statusFilter, callback)
        }
    }

    private fun fetchAppointmentDetails(
        appointmentIds: List<String>,
        appointmentDates: Map<String, Long>,
        startDate: Long?,
        endDate: Long?,
        statusFilter: String?,
        callback: (List<Appointment>) -> Unit
    ) {
        if (appointmentIds.isEmpty()) {
            callback(emptyList())
            return
        }

        val appointments = mutableListOf<Appointment>()
        var remaining = appointmentIds.size

        appointmentIds.forEach { appointmentId ->
            appointmentsRef.child(appointmentId).get().addOnSuccessListener { snapshot ->
                val appointment = snapshot.getValue(Appointment::class.java)

                if (appointment != null) {
                    // Apply filters
                    val shouldInclude = applyFilters(appointment, appointmentDates[appointmentId], startDate, endDate, statusFilter)

                    if (shouldInclude) {
                        appointments.add(appointment.copy(id = appointmentId))
                        println("DATABASE HELPER: Included appointment: ${appointment.serviceName}")
                    } else {
                        println("DATABASE HELPER: Filtered out appointment: ${appointment.serviceName}")
                    }
                }

                if (--remaining == 0) {
                    // Sort by date
                    val sorted = appointments.sortedByDescending { it.appointmentDate }
                    println("DATABASE HELPER: Returning ${sorted.size} filtered appointments")
                    callback(sorted)
                }
            }.addOnFailureListener {
                if (--remaining == 0) {
                    val sorted = appointments.sortedByDescending { it.appointmentDate }
                    callback(sorted)
                }
            }
        }
    }

    private fun applyFilters(
        appointment: Appointment,
        appointmentDate: Long?,
        startDate: Long?,
        endDate: Long?,
        statusFilter: String?
    ): Boolean {
        // Check date filters using the indexed date
        val date = appointmentDate ?: appointment.appointmentDate

        if (startDate != null && date < startDate) {
            println("   ✗ Filtered: Before start date")
            return false
        }

        if (endDate != null && date > endDate) {
            println("   ✗ Filtered: After end date")
            return false
        }

        // Check status filter
        if (statusFilter != null && appointment.status != statusFilter) {
            println("   ✗ Filtered: Status mismatch (${appointment.status} != $statusFilter)")
            return false
        }

        return true
    }

    private fun queryAppointmentsDirectly(
        providerId: String,
        startDate: Long?,
        endDate: Long?,
        statusFilter: String?,
        callback: (List<Appointment>) -> Unit
    ) {
        println("DATABASE HELPER: Querying appointments directly")

        appointmentsRef
            .orderByChild("providerId")
            .equalTo(providerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val appointments = mutableListOf<Appointment>()

                    println("DATABASE HELPER: Direct query found ${snapshot.childrenCount} appointments")

                    for (child in snapshot.children) {
                        val appointment = child.getValue(Appointment::class.java)
                        appointment?.let {
                            if (applyFilters(it, it.appointmentDate, startDate, endDate, statusFilter)) {
                                appointments.add(it.copy(id = child.key ?: ""))
                            }
                        }
                    }

                    val sorted = appointments.sortedByDescending { it.appointmentDate }
                    println("DATABASE HELPER: Direct query returning ${sorted.size} appointments")
                    callback(sorted)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DATABASE HELPER: Direct query error: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    private fun fetchAppointments(
        ids: List<String>,
        statusFilter: String?,
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
                        appointments.add(appointment)
                    }
                }
                if (--remaining == 0) {
                    // Sort by appointment date descending
                    callback(appointments.sortedByDescending { it.appointmentDate })
                }
            }
        }
    }

    fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        onSuccess: () -> Unit
    ) {
        val updates = mapOf(
            "status" to newStatus,
            "updatedAt" to System.currentTimeMillis()
        )

        if (newStatus == AppointmentStatus.COMPLETED.value) {
            updates.plus("completedAt" to System.currentTimeMillis())
        } else if (newStatus == AppointmentStatus.CANCELLED.value) {
            updates.plus("cancelledAt" to System.currentTimeMillis())
        }

        appointmentsRef.child(appointmentId).updateChildren(updates).addOnSuccessListener {
            onSuccess()
        }
    }

    fun deleteAppointment(appointmentId: String, providerId: String, onSuccess: () -> Unit) {
        // Delete appointment
        appointmentsRef.child(appointmentId).removeValue().addOnSuccessListener {
            // Remove from provider index
            appointmentsByProviderRef.child(providerId).child(appointmentId).removeValue().addOnSuccessListener {
                onSuccess()
            }
        }
    }

    // ==============================
    // AVAILABILITY MANAGEMENT
    // ==============================
    fun generateAvailabilitySlots(
        providerId: String,
        date: String, // "YYYY-MM-DD"
        startTime: String, // "09:00"
        endTime: String,   // "18:00"
        slotDuration: Int = 30, // minutes
        buffer: Int = 15 // minutes between slots
    ) {
        val providerSlotsRef = availabilityRef.child(providerId).child(date)

        // Clear existing slots first
        providerSlotsRef.removeValue().addOnSuccessListener {
            val slots = generateTimeSlots(startTime, endTime, slotDuration, buffer)

            slots.forEach { time ->
                providerSlotsRef.child(time).setValue(mapOf(
                    "isAvailable" to true,
                    "duration" to slotDuration
                ))
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

    // ==============================
    // UTILITY METHODS
    // ==============================
    fun getCurrentUserId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("User not authenticated")

    fun getCurrentUserEmail(): String = auth.currentUser?.email
        ?: throw IllegalStateException("User email not available")
}