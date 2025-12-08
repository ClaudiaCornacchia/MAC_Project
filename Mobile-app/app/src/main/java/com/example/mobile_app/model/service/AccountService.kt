package com.example.mobile_app.model.service

import com.example.mobile_app.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Ensures only one instance exists for the entire app life-cycle
class AccountService @Inject constructor() {

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

        val profileUpdates = userProfileChangeRequest {
            displayName = newDisplayName
        }
        user.updateProfile(profileUpdates).await()
    }

    // Method to create a new account using email/password (Sign Up)
    suspend fun createAccount(email: String, password: String) {
        Firebase.auth.createUserWithEmailAndPassword(email, password).await()
    }

    // Method for Login or Registration via Google
    suspend fun signInWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(firebaseCredential).await()
    }

    // Method for Login using Email/Password (Sign In)
    suspend fun signInWithEmail(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signOut() {
        Firebase.auth.signOut()
    }

    suspend fun deleteAccount() {
        Firebase.auth.currentUser?.delete()?.await()
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