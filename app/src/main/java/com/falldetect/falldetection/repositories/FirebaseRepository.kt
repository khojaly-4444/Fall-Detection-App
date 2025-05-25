package com.falldetect.falldetection.repositories

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.falldetect.falldetection.models.FallEvent
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val context: Context
) {

    // Check if a user is logged in
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    // Login logic with FCM Token update
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                            FirebaseMessaging.getInstance().token
                                .addOnSuccessListener { token ->
                                    database.child("users").child(uid).child("fcmToken").setValue(token)
                                        .addOnSuccessListener {
                                            Log.d("FCM", "Token refreshed and saved on login: $token")
                                            callback(true, null)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FCM", "Failed to save token on login", e)
                                            callback(true, null)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM", "Failed to get FCM token on login", e)
                                    callback(true, null)
                                }
                        }
                    } else {
                        callback(true, null)
                    }
                } else {
                    callback(false, task.exception?.message ?: "Login failed")
                }
            }
    }

    // Signup logic with user info + FCM token storage
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
                                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                                    FirebaseMessaging.getInstance().token
                                        .addOnSuccessListener { token ->
                                            database.child("users").child(uid).child("fcmToken").setValue(token)
                                                .addOnSuccessListener {
                                                    Log.d("FCM", "Token saved after signup: $token")
                                                    callback(true, null)
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("FCM", "Failed to save token after signup", e)
                                                    callback(true, null)
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FCM", "Failed to get FCM token after signup", e)
                                            callback(true, null)
                                        }
                                }
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

    // Link two users together by storing their IDs under "linked-users"
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

    // Sign out the user and detach listener flag
    fun signout() {
        auth.signOut()
        isListenerAttached = false
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private var lastFallTimestamp: Long = 0

    // Add a fall event to database and notify linked user
    suspend fun addFallEvent(fallEvent: FallEvent) {
        val now = System.currentTimeMillis()

        // Suppress duplicate fall events within 5 seconds
        if (now - lastFallTimestamp < 5000L) {
            Log.d("FirebaseRepo", "Duplicate fall event suppressed")
            return
        }

        lastFallTimestamp = now

        val currentUid = auth.currentUser?.uid ?: return

        // Add to current user's fall-data only
        val eventRef = database.child("fall-data").child(currentUid).push()
        eventRef.setValue(fallEvent).await()

        // Notify linked user via FCM if not self
        val linkedUidSnapshot = database.child("linked-users").child(currentUid).get().await()
        val linkedUid = linkedUidSnapshot.getValue(String::class.java)

        if (!linkedUid.isNullOrEmpty()) {
            val currentUserTokenSnapshot = database.child("users").child(currentUid).child("fcmToken").get().await()
            val currentUserToken = currentUserTokenSnapshot.getValue(String::class.java)

            val linkedUserTokenSnapshot = database.child("users").child(linkedUid).child("fcmToken").get().await()
            val linkedUserToken = linkedUserTokenSnapshot.getValue(String::class.java)

            if (!linkedUserToken.isNullOrEmpty() && linkedUserToken != currentUserToken) {
                sendFCMNotification(
                    linkedUserToken,
                    "Fall Alert!",
                    "A fall has been detected for your linked user."
                )
            }
        }
    }

    // Retrieve fall data for current user + linked user
    suspend fun getFallData(): List<FallEvent> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        val linkedUidSnapshot = database.child("linked-users").child(currentUid).get().await()
        val linkedUid = linkedUidSnapshot.getValue(String::class.java)

        val currentUserSnapshot = database.child("fall-data").child(currentUid).get().await()
        val currentUserFallData = currentUserSnapshot.children.mapNotNull { it.getValue(FallEvent::class.java) }

        val linkedUserFallData = if (!linkedUid.isNullOrEmpty()) {
            val linkedUserSnapshot = database.child("fall-data").child(linkedUid).get().await()
            linkedUserSnapshot.children.mapNotNull { it.getValue(FallEvent::class.java) }
        } else {
            emptyList()
        }

        // Remove duplicate entries (just in case)
        return (currentUserFallData + linkedUserFallData).distinctBy { it.date + it.time + it.impactSeverity }
    }

    private var isListenerAttached = false

    // Real-time listener for new fall events
    fun listenForFallEvents(onFallEventDetected: (FallEvent) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return
        val fallDataRef = database.child("fall-data").child(currentUid)

        if (isListenerAttached) {
            Log.d("FirebaseRepo", "Listener already attached,skipping.")
            return
        }

        isListenerAttached = true

        fallDataRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val fallEvent = snapshot.getValue(FallEvent::class.java) ?: return

                onFallEventDetected(fallEvent)

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepo", "Listener cancelled: ${error.message}")
                isListenerAttached = false
            }
        })
    }

    // Read Firebase service account token for HTTP FCM call
    private fun getAccessToken(): String {
        return try {
            val credentials = GoogleCredentials
                .fromStream(context.assets.open("service-account.json"))
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e("FCM", "Error getting access token", e)
            ""
        }
    }

    // Send push notification using Firebase HTTP v1 API
    private fun sendFCMNotification(token: String, title: String, message: String) {
        // Log the token before sending
        Log.d("FCM", "Sending to token: $token")

        val jsonObject = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", token)

                put("data", JSONObject().apply {
                    put("title", title)
                    put("message", message)
                })
                put("android", JSONObject().apply {
                    put("priority", "HIGH")
                    put("notification", JSONObject().apply {
                        put("sound", "default")
                    })
                })
            })
        }

        Log.d("FCM", "Sending payload:\n${jsonObject.toString(2)}") // 13 hours to fix this

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "https://fcm.googleapis.com/v1/projects/fall-detection-app-eae40/messages:send",
            jsonObject,
            { response -> Log.d("FCM", "Notification sent successfully: $response") },
            { error ->
                val errorBody = error.networkResponse?.data?.toString(Charsets.UTF_8)
                val status = error.networkResponse?.statusCode
                Log.e("FCM", "FCM ERROR $status: $errorBody")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Authorization" to "Bearer ${getAccessToken()}",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }
}
