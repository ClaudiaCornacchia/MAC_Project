package com.example.mobile_app.model.service.impl


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.example.mobile_app.model.User
import com.example.mobile_app.model.service.AccountService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccountServiceImpl @Inject constructor() : AccountService {

    override val currentUser: Flow<User?>
        get() = callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                this.trySend(auth.currentUser.toBoxUser())
            }
            Firebase.auth.addAuthStateListener(listener)
            awaitClose { Firebase.auth.removeAuthStateListener(listener) }
        }

    override val currentUserId: String
        get() = Firebase.auth.currentUser?.uid.orEmpty()

    override fun hasUser(): Boolean {
        return Firebase.auth.currentUser != null
    }

    override fun getUserProfile(): User {
        return Firebase.auth.currentUser.toBoxUser()
    }

    override suspend fun updateDisplayName(newDisplayName: String) {
        val user = Firebase.auth.currentUser ?: return

        val profileUpdates = userProfileChangeRequest {
            displayName = newDisplayName
        }
        user.updateProfile(profileUpdates).await()
    }

    // Metodo per creare un nuovo account con email/password (Sign Up)
    override suspend fun createAccount(email: String, password: String) {
        Firebase.auth.createUserWithEmailAndPassword(email, password).await()
    }

    // Metodo per Login o Registrazione tramite Google
    override suspend fun signInWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(firebaseCredential).await()
    }

    // Metodo per Login con Email/Password (Sign In)
    override suspend fun signInWithEmail(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signOut() {
        Firebase.auth.signOut()
        // Rimosso il login anonimo automatico
    }

    override suspend fun deleteAccount() {
        Firebase.auth.currentUser?.delete()?.await()
    }

    private fun FirebaseUser?.toBoxUser(): User {
        return if (this == null) User() else User(
            id = this.uid,
            email = this.email ?: "",
            provider = this.providerData.firstOrNull()?.providerId ?: "", // Recupera il provider reale (google, password, etc)
            displayName = this.displayName ?: "",
            isAnonymous = this.isAnonymous
        )
    }
}