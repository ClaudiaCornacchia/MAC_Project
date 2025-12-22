package com.example.mobile_app.model.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Google Places Client
    private val placesClient = Places.createClient(context)
    private var sessionToken: AutocompleteSessionToken? = null


    // 1. Get the CURRENT precise location (Lat, Lng)
    @SuppressLint("MissingPermission") // Permissions are handled in the UI layer
    suspend fun getCurrentLocation(): Location? {
        return try {
            // Priority.PRIORITY_HIGH_ACCURACY: Use GPS to get the most precise location.
            // CancellationToken is null because we don't need to cancel it manually here.
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception ("Couldn't get the current location")
        }
    }

    // 2. Reverse Geocoding: From (Lat, Lng) to "Human-readable Address"
    // This function must be 'suspend' to handle the asynchronous nature of the new API 33 Geocoder.
    suspend fun getAddressFromGeoPoint(geoPoint: GeoPoint): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // --- API 33+ Implementation (Android 13 and above) ---
                    // We use suspendCancellableCoroutine to convert the Callback/Listener into a return value
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(
                            geoPoint.latitude,
                            geoPoint.longitude,
                            1
                        ) { addresses ->
                            // This listener runs on a background thread
                            val result = formatAddress(addresses)
                            continuation.resume(result)
                        }
                    }
                } else {
                    // --- Legacy Implementation (Android 12 and below) ---
                    // This method is deprecated in API 33, but it is the ONLY way for older phones.
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                    formatAddress(addresses)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                throw  Exception("Couldn't get the address")
            }
        }
    }

    // Helper function to format the address list into a string
    private fun formatAddress(addresses: List<Address>?): String {
        return if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            // Format: Street 12, City, Country
            val street = address.thoroughfare ?: ""
            val number = address.subThoroughfare ?: ""
            val city = address.locality ?: ""

            // Clean up string to avoid extra spaces if some fields are null
            "$street $number, $city".trim().removePrefix(",").trim()
        } else {
            "Unknown Address"
        }
    }

    // 3. SEARCH SUGGESTIONS (Autocomplete)
    suspend fun getAutocompletePredictions(query: String): List<AutocompleteResult> {
        return suspendCancellableCoroutine { continuation ->
            // Create a new session token if needed (saves money on API calls)
            if (sessionToken == null) sessionToken = AutocompleteSessionToken.newInstance()

            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build()

            android.util.Log.d("PlacesDebug", "Starting search for: $query")

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val suggestions = response.autocompletePredictions.map { prediction ->
                        AutocompleteResult(
                            placeId = prediction.placeId,
                            primaryText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString()
                        )
                    }
                    android.util.Log.d("PlacesDebug", "Found ${suggestions.size} results")

                    continuation.resume(suggestions)
                }
                .addOnFailureListener { exception ->
                    if (exception is com.google.android.gms.common.api.ApiException) {
                        android.util.Log.e("PlacesDebug", "Place API Error: ${exception.statusCode} - ${exception.message}")
                    } else {
                        android.util.Log.e("PlacesDebug", "Generic Error: ${exception.message}")
                    }
                    continuation.resume(emptyList())
                }
        }
    }

    // 4. GET COORDINATES FROM PLACE ID (When user clicks a suggestion)
    suspend fun getPlaceDetails(placeId: String): Location? {
        return suspendCancellableCoroutine { continuation ->
            // We need ID and Lat/Lng
            val placeFields = listOf(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken) // End the session here
                .build()

            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val latLng = place.latLng
                    if (latLng != null) {
                        val location = Location("PlacesAPI")
                        location.latitude = latLng.latitude
                        location.longitude = latLng.longitude

                        // Reset session token for next search
                        sessionToken = null

                        continuation.resume(location)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}

// Simple data class for suggestions
data class AutocompleteResult(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)