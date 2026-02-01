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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.mobile_app.model.service.AutocompleteResult

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

    // Update box fields
    var isEditing by mutableStateOf(false)
        private set
    var draftBox by mutableStateOf(Box())
        private set
    var newPhotoUri by mutableStateOf<Uri?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var tempPhotoUri by mutableStateOf<Uri?>(null)
    var locationPredictions by mutableStateOf<List<AutocompleteResult>>(emptyList())
        private set

    var pendingUploadUri: Uri? by mutableStateOf(null)
        private set

    // Delete box
    var showDeleteDialog by mutableStateOf(false)
        private set

    private var searchJob: Job? = null


    fun initialize(boxId: String) {
        if (!accountService.hasUser()) {
            isUserAuthorized = false
        }
        else {

            launchCatching {
                // Every time that you open the box detail page update last access
                storageService.updateLastAccess(boxId)
            }
            launchCatching {
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

            val currentBox = box

            if (location != null && currentBox != null) {
                // Convert Android Location to Firestore GeoPoint
                val newGeoPoint = GeoPoint(location.latitude, location.longitude)

                // Get the human-readable address (Reverse Geocoding)
                val newAddress = locationService.getAddressFromGeoPoint(newGeoPoint)

                // 2. Create a copy of the current box with new location data
                val updatedBox = currentBox.copy(
                    location = newGeoPoint,
                    locationAddress = newAddress
                )

                // 3. Update the UI State IMMEDIATELY
                // Because 'box' is a mutableState, assigning a new value here
                // triggers a Recomposition in BoxDetailScreen (Map moves, text updates).
                box = updatedBox

                // 4. Save to Firestore Database
                storageService.updateBoxFields(
                    boxId = currentBox.boxId,
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

    // UPDATE BOX
    fun startEditing() {
        // When I click on the update button I'm in edit mode
        box?.let { current ->
            draftBox = current.copy()
            isEditing = true
        }
    }

    fun cancelEditing() {
        // If I cancel discard the changes
        isEditing = false
    }

    fun updateDraftTitle(newTitle: String) { draftBox = draftBox.copy(title = newTitle) }
    fun updateDraftDescription(newDesc: String) { draftBox = draftBox.copy(description = newDesc) }
    fun updateDraftFragile(isFragile: Boolean) { draftBox = draftBox.copy(isFragile = isFragile) }
    fun updateDraftStatus(newStatus: String) { draftBox = draftBox.copy(fillStatus = newStatus) }
    fun updateDraftNote(newLocationAddress: String) { draftBox = draftBox.copy(locationAddress = newLocationAddress) }
    fun updateDraftPhoto(uri: Uri) { newPhotoUri = uri }

    fun saveEditChanges() {
        launchCatching {
            isLoading = true


            val updates = mutableMapOf<String, Any>(
                "title" to draftBox.title,
                "titleSearch" to draftBox.title.lowercase(),
                "description" to draftBox.description,
                "fillStatus" to draftBox.fillStatus,
                "fragile" to draftBox.isFragile,
                "locationAddress" to draftBox.locationAddress
            )

            if (draftBox.location != null) {
                updates["location"] = draftBox.location!!
            }

            if (newPhotoUri != null) {
                pendingUploadUri = newPhotoUri
            }

            storageService.updateBoxFast(
                boxId = draftBox.boxId,
                updates = updates,
                newImageUri = newPhotoUri
            )

            val currentBox = box
            if (currentBox != null) {
                box = currentBox.copy(
                    title = draftBox.title,
                    description = draftBox.description,
                    fillStatus = draftBox.fillStatus,
                    isFragile = draftBox.isFragile,
                    secretNote = draftBox.secretNote,
                    locationAddress = draftBox.locationAddress,

                    location = draftBox.location ?: currentBox.location
                )
            }

            newPhotoUri = null
            isEditing = false
            isLoading = false
        }
    }

    fun onLocationQueryChange(newQuery: String) {

        draftBox = draftBox.copy(
            locationAddress = newQuery,
            location = null
        )

        searchJob?.cancel()

        if (newQuery.length > 2) {
            searchJob = launchCatching {
                delay(500) // Debounce
                locationPredictions = locationService.getAutocompletePredictions(newQuery)
            }
        } else {
            locationPredictions = emptyList()
        }
    }

    fun onLocationPredictionSelected(prediction: AutocompleteResult) {
        launchCatching {
            val fullAddress = "${prediction.primaryText}, ${prediction.secondaryText}"

            draftBox = draftBox.copy(locationAddress = fullAddress)
            locationPredictions = emptyList()

            val location = locationService.getPlaceDetails(prediction.placeId)

            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                // Update also the coordinates in the draft
                draftBox = draftBox.copy(location = geoPoint)
            } else {
                SnackbarManager.showMessage("Could not fetch coordinates for this place.")
            }
        }
    }

    fun clearLocationSuggestions() {
        locationPredictions = emptyList()
    }

    // DELETE BOX
    // Called when the user clicks the trash icon
    fun onDeleteClick() {
        showDeleteDialog = true
    }

    // Called when the user clicks "Cancel" in the dialog
    fun onDeleteCancel() {
        showDeleteDialog = false
    }

    // Called when the user confirms deletion
    fun deleteBox(onSuccess: () -> Unit) {
        launchCatching {
            isLoading = true
            // Delete image, QR, and Firestore document
            storageService.deleteBox(boxId, box.ownerId)
            isLoading = false

            // Navigate back to the previous screen
            onSuccess()
        }
    }


}