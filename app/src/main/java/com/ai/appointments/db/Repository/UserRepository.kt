package com.ai.appointments.db.Repository

import com.ai.appointments.db.models.NormalUser
import com.ai.appointments.db.models.ServiceProvider

object UserRepository : BaseRepository() {
    private val normalUsersRef = database.getReference("users/normal")
    private val providersRef = database.getReference("providers")

    fun saveNormalUser(user: NormalUser, uid: String = getCurrentUserId()) {
        normalUsersRef.child(uid).setValue(user)
    }

    fun saveServiceProvider(provider: ServiceProvider, uid: String = getCurrentUserId()) {
        providersRef.child(uid).setValue(provider)
    }

    fun getNormalUser(uid: String = getCurrentUserId(), callback: (NormalUser?) -> Unit) {
        normalUsersRef.child(uid).get().addOnSuccessListener { snapshot ->
            callback(snapshot.getValue(NormalUser::class.java))
        }.addOnFailureListener { callback(null) }
    }

    fun getServiceProvider(uid: String = getCurrentUserId(), callback: (ServiceProvider?) -> Unit) {
        providersRef.child(uid).get().addOnSuccessListener { snapshot ->
            callback(snapshot.getValue(ServiceProvider::class.java))
        }.addOnFailureListener { callback(null) }
    }

    fun isServiceProvider(uid: String = getCurrentUserId(), callback: (Boolean) -> Unit) {
        providersRef.child(uid).get().addOnSuccessListener { snapshot ->
            callback(snapshot.exists())
        }
    }
}