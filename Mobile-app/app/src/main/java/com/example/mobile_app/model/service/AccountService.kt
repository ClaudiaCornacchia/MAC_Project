package com.example.mobile_app.model.service

import com.example.mobile_app.model.User
import kotlinx.coroutines.flow.Flow

interface AccountService {
    val currentUser: Flow<User?>
    val currentUserId: String

    fun hasUser(): Boolean
    fun getUserProfile(): User

    suspend fun updateDisplayName(newDisplayName: String)

    // Rimosso createAnonymousAccount e linkAccount...
    // Aggiunto createAccount per la registrazione standard
    suspend fun createAccount(email: String, password: String)

    suspend fun signInWithGoogle(idToken: String)
    suspend fun signInWithEmail(email: String, password: String)

    suspend fun signOut()
    suspend fun deleteAccount()
}