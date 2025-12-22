package com.example.mobile_app.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import com.google.firebase.firestore.PropertyName

data class Box(
    // Document ID (Firestore automatically populates this when reading,
    // but it is useful to have it in the body if you create it manually with a UUID)
    @DocumentId val boxId: String = "",

    // THE LINK: The user's UID goes here (e.g., "Abc123XY...")
    // This is essential for Security Rules and query filtering (My Boxes vs Others).
    val ownerId: String = "",

    // Content
    val humanId: String = "",
    val title: String = "",
    // Important: populate this field with title.lowercase() before saving!
    // This allows for easier, case-insensitive searching later.
    val titleSearch: String = "",
    val description: String = "",

    // Media URLs (pointing to Firebase Storage)
    val audioUrl: String = "",
    val imageUrl: String = "",
    val qrCodeUrl: String = "",

    // Status and Attributes
    @get:PropertyName("fragile")
    @set:PropertyName("fragile")
    var isFragile: Boolean = false,

    val fillStatus: String = "GREEN", // Values: "GREEN" (empty), "YELLOW" (half), "RED" (full)
    val secretNote: String = "",

    // Location (can be null if the box hasn't been geotagged)
    val location: GeoPoint? = null,  //firestore type for geo-coordinates
    val locationAddress: String = "", // Human-readable address cache

    // Dates
    // @ServerTimestamp tells Firestore: "Use the actual server time when saving this document"
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val lastAccess: Date? = null
)