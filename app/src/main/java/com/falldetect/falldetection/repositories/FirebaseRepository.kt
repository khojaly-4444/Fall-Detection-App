package com.falldetect.falldetection.repositories


import com.falldetect.falldetection.models.FallEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

// Firebase Operations

class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    // Check if the user is currently authenticated
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    // Login Function
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null) // Success
                } else {
                    callback(false, task.exception?.message ?: "Login failed") // Error
                }
            }
    }

    // Signup Function
    fun signup(email: String, password: String, name: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userMap = mapOf(
                            "name" to name,
                            "email" to email
                        )
                        database.child("users").child(uid).setValue(userMap)
                            .addOnSuccessListener {
                                callback(true, null) // Success
                            }
                            .addOnFailureListener { exception ->
                                callback(false, exception.message ?: "Failed to save user data")
                            }
                    } else {
                        callback(false, "User ID is null")
                    }
                } else {
                    callback(false, task.exception?.message ?: "Signup failed")
                }
            }
    }

    // Link Users Function
    fun linkUsers(currentUid: String, otherUid: String, callback: (Boolean, String) -> Unit) {
        database.child("users").child(otherUid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val updates = mapOf(
                        "linked-users/$currentUid" to otherUid,
                        "linked-users/$otherUid" to currentUid
                    )
                    database.updateChildren(updates)
                        .addOnSuccessListener {
                            callback(true, "Successfully linked to User ID: $otherUid")
                        }
                        .addOnFailureListener { exception ->
                            callback(false, "Failed to link users: ${exception.message}")
                        }
                } else {
                    callback(false, "User ID not found in Database.")
                }
            }
            .addOnFailureListener { exception ->
                callback(false, "Error: ${exception.message}")
            }
    }

    // Signout Function
    fun signout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Function to add fall event and sync with linked user
    suspend fun addFallEvent(userId: String,fallEvent: FallEvent) {
        val currentUid = auth.currentUser?.uid ?: return

        // Store event for the current user
        val eventRef = database.child("fall-data").child(currentUid).push()
        eventRef.setValue(fallEvent).await()

        // Get the linked user ID
        val linkedUidSnapshot = database.child("linked-users").child(currentUid).get().await()
        val linkedUid = linkedUidSnapshot.getValue(String::class.java)

        // Store event for linked user if one exists
        if (!linkedUid.isNullOrEmpty()){
            val linkedEventRef = database.child("fall-data").child(linkedUid).push()
            linkedEventRef.setValue(fallEvent).await()
        }
    }

    // Function to fetch fall data
    suspend fun getFallData(): List<FallEvent> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()

        // Get Linked User ID
        val linkedUidSnapshot = database.child("linked-users").child(currentUid).get().await()
        val linkedUid = linkedUidSnapshot.getValue(String::class.java)

        // Get fall events for the current user
        val currentUserSnapshot = database.child("fall-data").child(currentUid).get().await()
        val currentUserFallData = currentUserSnapshot.children.mapNotNull { it.getValue(FallEvent::class.java) }

        // Get fall events for the linked user if exists
        val linkedUserFallData = if (!linkedUid.isNullOrEmpty()) {
            val linkedUserSnapshot = database.child("fall-data").child(linkedUid).get().await()
            linkedUserSnapshot.children.mapNotNull { it.getValue(FallEvent::class.java) }
        } else {
            emptyList()
        }
        // combine both datasets
        return (currentUserFallData + linkedUserFallData).distinctBy { it.date + it.time + it.heartRate }
    }
}
