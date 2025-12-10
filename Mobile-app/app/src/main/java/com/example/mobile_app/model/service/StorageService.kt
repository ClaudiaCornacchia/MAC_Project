package com.example.mobile_app.model.service

import com.example.mobile_app.model.Box
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StorageService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val accountService: AccountService
) {

    // 1. READ ALL: Get all boxes for the current user
    @OptIn(ExperimentalCoroutinesApi::class)
    val userBoxes: Flow<List<Box>>
        get() = accountService.currentUser.flatMapLatest { user ->
            // If user is null (not logged), return empty flow. Otherwise query Firestore
            val userId = user?.id ?: ""

            firestore.collection("boxes")
                .whereEqualTo("ownerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .dataObjects() // This is a specialized Flow builder for Firestore
        }

    // 2. READ ONE: Get a specific box by ID
    suspend fun getBox(boxId: String): Box? {
        return firestore.collection("boxes").document(boxId).get().await().toObject()
    }

    // 3. CREATE: Save a new box
    suspend fun saveBox(box: Box) {
        val userId = accountService.currentUserId

        // Security check: ensure the ownerId is the current user
        val boxWithInfo = box.copy(
            ownerId = userId,
            titleSearch = box.title.lowercase() // Auto-fill search field
            // createdAt and lastAccess are handled automatically by @ServerTimestamp
        )

        firestore.collection("boxes").add(boxWithInfo).await()
    }
}