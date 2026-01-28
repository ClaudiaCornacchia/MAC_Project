package com.example.mobile_app.screens.box.box_detail

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.example.mobile_app.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.model.Box
import com.example.mobile_app.model.service.AccountService
import com.example.mobile_app.model.service.LocationService
import com.example.mobile_app.model.service.StorageService
import com.example.mobile_app.screens.BoxAppViewModel
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@HiltViewModel
class BoxDetailViewModel @Inject constructor(
    private val storageService: StorageService,
    private val accountService: AccountService,
    private val locationService: LocationService,
    savedStateHandle: SavedStateHandle // Here we get navigation arguments
) : BoxAppViewModel() {

    // "boxId" must match the name in the route definition
    private val boxId: String = savedStateHandle["boxId"] ?: ""

    var box by mutableStateOf(Box())
        private set

    var isUserAuthorized by mutableStateOf(true)
        private set

    var showShareDialog by mutableStateOf(false)
        private set

    var sharedNames by mutableStateOf<List<String>>(emptyList())
        private set


    fun initialize(boxId: String) {
        if (!accountService.hasUser()) {
            isUserAuthorized = false
        }
        else {
            launchCatching {
                // Every time that you open the box detail page update last access
                storageService.updateLastAccess(boxId)

                // Listen for changes in the box
                storageService.getBox(boxId).collect { newBox ->

                    box = newBox

                    // If the box is shared add the names
                    if (newBox.sharedWith.isNotEmpty()) {
                        loadSharedNames()
                    } else {
                        sharedNames = emptyList()
                    }
                }
            }
        }
    }

    // QR CODE
    // Function to download the QR Code using the Android DownloadManager.
    // We pass the Context here because DownloadManager is a system service.
    fun downloadQrCode(context: Context) {
        val url = box.qrCodeUrl
        if (url.isBlank()) return

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Box QR Code")
                .setDescription("Downloading QR Code for ${box.title}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                // Save to the standard "Downloads" folder
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "QR_${box.humanId}.png")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            // Optional: You could trigger a small toast or snackbar here via a state
        } catch (e: Exception) {
            throw Exception("Error downloading QR code.")
        }
    }

    // MAPS, Update location
    fun updateLocation() {
        launchCatching {
            // 1. Get the current GPS Location (High Accuracy)
            val location = locationService.getCurrentLocation()

            if (location != null) {
                // Convert Android Location to Firestore GeoPoint
                val newGeoPoint = GeoPoint(location.latitude, location.longitude)

                // Get the human-readable address (Reverse Geocoding)
                val newAddress = locationService.getAddressFromGeoPoint(newGeoPoint)

                // 2. Create a copy of the current box with new location data
                val updatedBox = box.copy(
                    location = newGeoPoint,
                    locationAddress = newAddress
                )

                // 3. Update the UI State IMMEDIATELY
                // Because 'box' is a mutableState, assigning a new value here
                // triggers a Recomposition in BoxDetailScreen (Map moves, text updates).
                box = updatedBox

                // 4. Save to Firestore Database
                storageService.updateBoxFields(
                    boxId = box.boxId,
                    updates = mapOf(
                        "location" to newGeoPoint,
                        "locationAddress" to newAddress
                    )
                )

            } else {
                // Handle error if GPS is off or signal is lost
                SnackbarManager.showMessage("Could not get GPS location. Please check your settings.")
            }
        }
    }

    // SHARE BOX
    fun onShareClick() {
        showShareDialog = true
    }

    fun onShareDismiss() {
        showShareDialog = false
    }

    fun onShareConfirm(email: String) {
        showShareDialog = false
        launchCatching {
            if (email.isNotBlank()) {
                storageService.shareBoxWithUser(box.boxId, email)
            } else {
                SnackbarManager.showMessage("Email cannot be empty.")
            }
        }
    }

    private fun loadSharedNames() {
        launchCatching {
            val names = box.sharedWith

                .filter { it != accountService.currentUserId }
                .map { memberId ->
                    async { accountService.getUserName(memberId) }
                }
                .awaitAll() // Await for all coroutines to complete
                .filter { it.isNotBlank() }

            sharedNames = names
        }
    }




}