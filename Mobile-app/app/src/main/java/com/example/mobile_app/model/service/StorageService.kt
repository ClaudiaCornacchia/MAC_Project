package com.example.mobile_app.model.service


import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
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
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class StorageService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val accountService: AccountService,
    private val qrApiService: QrApiService,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context,
    private val applicationScope: CoroutineScope
) {

    // 1. READ ALL: Get all boxes for the current user
    @OptIn(ExperimentalCoroutinesApi::class)
    val userBoxes: Flow<List<Box>>
        get() = accountService.currentUser.flatMapLatest { user ->
            // If user is null (not logged), return empty flow. Otherwise query Firestore
            val userId = user?.id ?: ""

            firestore.collection("boxes")
                .whereArrayContains("sharedWith", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .dataObjects() // This is a specialized Flow builder for Firestore
        }

    // 2. READ ONE: Get a specific box by ID
    fun getBox(boxId: String): Flow<Box> {
        return firestore.collection("boxes")
            .document(boxId)
            .snapshots() // This is a specialized Flow builder for Firestore
            .map { snapshot ->
                // Convert the DocumentSnapshot to our Box object
                snapshot.toObject(Box::class.java) ?: Box()
            }
    }

    // 3. CREATE: Save a new box
    fun saveBox(box: Box, imageUri: Uri?) {


        // 3. Background upload, use applicationScope.launch
        applicationScope.launch {
            try {
                val userId = accountService.currentUserId

                // 1. FETCH USER DATA, we need the freshest data from the server to avoid duplicates
                val userDocRef = firestore.collection("users").document(userId)
                val userSnapshot = userDocRef.get().await()

                val currentUser = userSnapshot.toObject(User::class.java) ?: User()
                // Human readable id
                val nextNumber = currentUser.lastBoxNumber + 1
                val generatedHumanId = "$nextNumber"

                val newDocRef = firestore.collection("boxes").document()
                val generatedId = newDocRef.id

                val initialStatusImage = if (imageUri != null) "UPLOADING" else ""

                // 2. Initial (fast) save
                val initialBox = box.copy(
                    boxId = generatedId,
                    ownerId = userId,
                    sharedWith = listOf(userId),
                    titleSearch = box.title.lowercase(),
                    humanId = generatedHumanId,
                    imageUrl = initialStatusImage,
                    qrCodeUrl = ""
                )

                val batch = firestore.batch()
                batch.set(newDocRef, initialBox)
                batch.update(userDocRef, "lastBoxNumber", nextNumber)

                batch.commit().await()

                // A. Upload the image to Firebase Storage
                var finalImageUrl = ""
                if (imageUri != null) {
                    val compressedData = getCompressedImage(imageUri)
                    if (compressedData != null) {
                        val storageRef = storage.reference.child("box_images/$userId/$generatedId.jpg")
                        storageRef.putBytes(compressedData).await()
                        finalImageUrl = storageRef.downloadUrl.await().toString()
                    }
                }

                // B. CALL THE SERVER NODE.JS to create qrcode and save qrcode link
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                val tokenResult = user?.getIdToken(true)?.await()
                val tokenString = "Bearer ${tokenResult?.token}"

               val qrUrl = try {
                    qrApiService.generateQr(tokenString, QrRequest(boxId = generatedId)).qrCodeUrl
               } catch (e: Exception) {
                    throw Exception("Error generating QR code.")
                }

                // Final update
                firestore.collection("boxes").document(generatedId).update(
                    mapOf(
                        "imageUrl" to finalImageUrl,
                        "qrCodeUrl" to qrUrl
                    )
                ).await()

                android.util.Log.d("BACKGROUND_UPLOAD", "Upload completato in background!")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    // 4. UPDATE: Update an existing box
    suspend fun updateBoxFields(boxId: String, updates: Map<String, Any>) {
        // "update" only changes the fields provided in the map.
        // It fails if the document does not exist.
        firestore.collection("boxes").document(boxId).update(updates).await()
    }

    suspend fun updateBoxFast(
        boxId: String,
        updates: MutableMap<String, Any>,
        newImageUri: Uri?
    ) {
        val userId = accountService.currentUserId

        // 1. update text image url to uploading
        if (newImageUri != null) {
            updates["imageUrl"] = "UPLOADING"
        }
        updates["lastEdited"] = com.google.firebase.Timestamp.now()

        firestore.collection("boxes").document(boxId).update(updates).await()

        // 2. Background image loading
        if (newImageUri != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    // Compress image
                    val compressedData = getCompressedImage(newImageUri)

                    if (compressedData != null) {
                        val storageRef = storage.reference.child("box_images/$userId/$boxId.jpg")
                        storageRef.putBytes(compressedData).await()

                        val finalImageUrl = storageRef.downloadUrl.await().toString()

                        // Update firestore with the real url
                        firestore.collection("boxes").document(boxId).update("imageUrl", finalImageUrl).await()

                        android.util.Log.d("UPDATE_BG", "Foto aggiornata in background!")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // firestore.collection("boxes").document(boxId).update("imageUrl", "").await()
                }
            }
        }
    }

    suspend fun uploadImageAndGetUrl(uri: Uri, boxId: String): String? {
        val userId = accountService.currentUserId

        val compressedData = getCompressedImage(uri) ?: return null

        val storageRef = storage.reference.child("box_images/$userId/$boxId.jpg")

        // Upload
        storageRef.putBytes(compressedData).await()

        // Return Download URL
        return storageRef.downloadUrl.await().toString()
    }

    // 5. SHARE BOX
    suspend fun shareBoxWithUser(boxId: String, email: String) {
        // 1. Search for the target user by email in the 'users' collection
        val usersQuery = firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .await()

        if (usersQuery.isEmpty) {
            throw Exception("Invalid email")
        }

        // 2. Get the ID of the found user
        val friendId = usersQuery.documents.first().id

        // 3. Atomically update the box (Concurrency safe)
        // FieldValue.arrayUnion adds the element to the array ONLY if it's not already there.
        // This prevents duplicates and handles concurrent edits safely.
        firestore.collection("boxes").document(boxId)
            .update("sharedWith", FieldValue.arrayUnion(friendId))
            .await()
    }

    // 6. Update last access
    suspend fun updateLastAccess(boxId: String) {
        val updates = mapOf("lastAccess" to com.google.firebase.Timestamp.now())
        firestore.collection("boxes").document(boxId).update(updates).await()
    }

    // 7. Delete box
    suspend fun deleteBox(boxId: String, ownerId: String) {
        // 1. Delete the Box Image from Storage (if it exists)
        try {
            Firebase.storage.reference.child("box_images/$ownerId/$boxId.jpg").delete().await()
        } catch (e: Exception) {
            // Ignore error if the image does not exist
        }

        // 2. Delete the QR Code from Storage (if it exists)
        try {
            Firebase.storage.reference.child("qrcodes/$boxId.png").delete().await()
        } catch (e: Exception) {
            // Ignore error if the QR does not exist
        }

        // 3. Delete the document from Firestore Database
        Firebase.firestore.collection("boxes").document(boxId).delete().await()
    }




    // Compress the image
    private fun getCompressedImage(uri: Uri): ByteArray? {
        return try {
            // 1. Decode the image from the URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // 2. Resize the image if necessary
            val maxDimension = 1024
            var width = originalBitmap.width
            var height = originalBitmap.height

            if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                if (width > height) {
                    width = maxDimension
                    height = (width / ratio).toInt()
                } else {
                    height = maxDimension
                    width = (height * ratio).toInt()
                }
            }
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            // 3. Compress the image to a ByteArray
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}