package com.example.mobile_app.model.service


import com.example.mobile_app.model.Box
import com.example.mobile_app.model.QrRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.mobile_app.model.User
class StorageService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val accountService: AccountService,
    private val qrApiService: QrApiService
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

        // FETCH USER DATA (To get the current counter)
        // We need the freshest data from the server to avoid duplicates
        val userDocRef = firestore.collection("users").document(userId)
        val userSnapshot = userDocRef.get().await()
        // Convert the document to our User object
        val currentUser = userSnapshot.toObject(User::class.java) ?: User()

        // CALCULATE THE NEW HUMAN ID
        val nextNumber = currentUser.lastBoxNumber + 1
        val generatedHumanId = "$nextNumber"

        // Generate the document ID locally
        val newDocRef = firestore.collection("boxes").document()
        val generatedId = newDocRef.id

        // Call the server Node.js
        val qrUrl = try {
            qrApiService.generateQr(QrRequest(boxId = generatedId)).qrCodeUrl
        } catch (e: Exception) {
            throw Exception("Error generating QR code.")
        }

        val boxWithInfo = box.copy(
            boxId = generatedId,
            ownerId = userId,
            titleSearch = box.title.lowercase(), // Auto-fill search field
            qrCodeUrl = qrUrl,
            humanId = generatedHumanId
            // createdAt and lastAccess are handled automatically by @ServerTimestamp
        )

        // Save in Firestore
        // We must save the Box AND update the User's counter at the same time.
        // If one fails, both fail. This prevents data inconsistency.
        val batch = firestore.batch()
        // Save the new Box
        batch.set(newDocRef, boxWithInfo)
        // Update the User's lastBoxNumber
        batch.update(userDocRef, "lastBoxNumber", nextNumber)
        // Commit both operations
        batch.commit().await()
    }
}