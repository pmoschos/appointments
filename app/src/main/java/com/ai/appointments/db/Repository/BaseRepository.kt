package com.ai.appointments.db.Repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

abstract class BaseRepository {
    protected val database = FirebaseDatabase.getInstance()
    protected val auth = FirebaseAuth.getInstance()

    // Add these references so they're available in all repositories
    protected val availabilityRef = database.getReference("availability")
    protected val appointmentsRef = database.getReference("appointments")
    protected val appointmentsByUserRef = database.getReference("indexes/appointments_by_user")
    protected val appointmentsByProviderRef = database.getReference("indexes/appointments_by_provider")
    protected val analyticsRef = database.getReference("analytics/user_monthly")

    protected fun <T> handleResult(
        task: Task<Void>,
        onSuccess: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        task.addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { error -> onError?.invoke(error) }
    }

    protected fun getCurrentUserId(): String = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
}