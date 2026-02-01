package com.example.mobile_app.data.repository

import com.example.mobile_app.domain.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Ensures only one instance exists for the entire app life-cycle
class AccountRepository @Inject constructor() {

    val currentUser: Flow<User?>
        get() = callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                this.trySend(auth.currentUser.toBoxUser())
            }
            Firebase.auth.addAuthStateListener(listener)
            awaitClose { Firebase.auth.removeAuthStateListener(listener) }
        }

    val currentUserId: String
        get() = Firebase.auth.currentUser?.uid.orEmpty()

    fun hasUser(): Boolean {
        return Firebase.auth.currentUser != null
    }

    fun getUserProfile(): User {
        return Firebase.auth.currentUser.toBoxUser()
    }

    suspend fun updateDisplayName(newDisplayName: String) {
        val user = Firebase.auth.currentUser ?: return

        // 1. Update Profile in Firebase Auth (The Passport)
        val profileUpdates = userProfileChangeRequest {
            displayName = newDisplayName
        }
        user.updateProfile(profileUpdates).await()

        // 2. Update Profile in Firestore (The Database)
        // We also need to keep the database in sync
        Firebase.firestore.collection("users").document(user.uid)
            .update("displayName", newDisplayName).await()
    }

    // Method to create a new account using email/password (Sign Up)
    suspend fun createAccount(email: String, password: String) {
        // 1. Create the user in Authentication
        val authResult = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: return

        // 2. Create the User object for Firestore
        val nameFromEmail = email.substringBefore("@")

        val newUser = User(
            id = firebaseUser.uid,
            email = email,
            provider = "email",
            displayName = nameFromEmail // Name is empty initially for email/password sign up
        )

        // 3. Save the User in Firestore using the SAME UID
        Firebase.firestore.collection("users").document(newUser.id).set(newUser).await()
    }

    // Method for Login or Registration via Google
    suspend fun signInWithGoogle(idToken: String) {
        // 1. Sign In with Google Credentials
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
        val firebaseUser = authResult.user ?: return

        // 2. Check if the user document already exists in Firestore
        val userDocRef = Firebase.firestore.collection("users").document(firebaseUser.uid)
        val userSnapshot = userDocRef.get().await()

        if (!userSnapshot.exists()) {
            // 3. If it does NOT exist (First time user), create the profile
            val newUser = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                provider = "google",
                displayName = firebaseUser.displayName ?: "" // Google usually provides the name
            )
            userDocRef.set(newUser).await()
        }
    }

    // Method for Login using Email/Password (Sign In)
    suspend fun signInWithEmail(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signOut() {
        Firebase.auth.signOut()
    }

    suspend fun deleteAccount() {
        val userId = currentUserId

        // 1. Delete from Authentication
        Firebase.auth.currentUser?.delete()?.await()

        // 2. Delete from Firestore
        if (userId.isNotEmpty()) {
            Firebase.firestore.collection("users").document(userId).delete().await()
        }
    }

    //Used by BoxDetailViewmodel to retrieve the name of the users that shares the box
    suspend fun getUserName(userId: String): String {
        return try {
            val doc = Firebase.firestore.collection("users").document(userId).get().await()
            doc.getString("displayName") ?: "Unknown User"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun FirebaseUser?.toBoxUser(): User {
        return if (this == null) User() else User(
            id = this.uid,
            email = this.email ?: "",
            // Retrieves the actual provider ID (google, password, etc.)
            provider = this.providerData.firstOrNull()?.providerId ?: "",
            displayName = this.displayName ?: "",
        )
    }


}