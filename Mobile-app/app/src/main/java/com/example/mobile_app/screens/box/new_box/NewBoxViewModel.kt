package com.example.mobile_app.screens.box.new_box

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mobile_app.BOX_DETAIL_SCREEN
import com.example.mobile_app.R
import com.example.mobile_app.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.model.Box
import com.example.mobile_app.model.service.LocationService
import com.example.mobile_app.model.service.SpeechService
import com.example.mobile_app.model.service.SpeechState
import com.example.mobile_app.model.service.StorageService
import com.example.mobile_app.screens.BoxAppViewModel
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.mobile_app.model.service.AutocompleteResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@HiltViewModel
class NewBoxViewModel @Inject constructor(
    private val storageService: StorageService,
    private val speechService: SpeechService,
    private val locationService: LocationService,
    private val application: Application // Context needed for permission check
) : BoxAppViewModel() {

    var uiState by mutableStateOf(NewBoxUiState())
        private set

    // 1. SPEECH TO TEXT STATE
    var isListeningModeActive by mutableStateOf(false)
        private set
    // Volume level for the Visualizer
    var currentLoudness by mutableFloatStateOf(0f)
        private set
    // temporary text: This is what you see ONLY in the popup while speaking.
    var popupDisplayText by mutableStateOf("")
        private set

    // 2. LOCATION STATE
    // List of suggestions from Google
    var locationPredictions by mutableStateOf<List<AutocompleteResult>>(emptyList())
        private set

    var tempPhotoUri: Uri? = null

    // Job to handle search debounce (delay)
    private var searchJob: Job? = null

    fun onTitleChange(newValue: String) { uiState = uiState.copy(title = newValue) }
    fun onDescriptionChange(newValue: String) { uiState = uiState.copy(description = newValue) }
    fun onSecretNoteChange(newValue: String) { uiState = uiState.copy(secretNote = newValue) }
    fun onFragileChange(newValue: Boolean) { uiState = uiState.copy(isFragile = newValue) }
    fun onFillStatusChange(newValue: String) { uiState = uiState.copy(fillStatus = newValue) }

    // 1. SPEECH FUNCTIONS

    fun startListening() {
        isListeningModeActive = true
        popupDisplayText = "" // Reset popup text
        currentLoudness = 0f

        launchCatching {
            speechService.startRecognition().collect { state ->
                // If we closed the popup, ignore any residual events
                if (!isListeningModeActive) return@collect

                when (state) {
                    is SpeechState.Working -> {
                        // 1. Update the Visualizer
                        if (state.rmsDb > 0) currentLoudness = state.rmsDb

                        // 2. Update ONLY the POPUP text in real-time
                        if (state.partialText.isNotEmpty()) {
                            popupDisplayText = state.partialText
                        }
                    }
                    is SpeechState.End -> {
                        // Sentence finished (silence detected by Android).
                        // Update the final text in the popup if available
                        if (state.finalText.isNotEmpty()) {
                            popupDisplayText = state.finalText
                        }
                        // Automatically save and close
                        stopAndSaveRecording()
                    }
                    is SpeechState.Error -> {
                        //Use the message coming from the Service
                        val errorMessage = state.message

                        SnackbarManager.showMessage(errorMessage)
                        stopRecordingAndDismiss()
                    }
                    else -> {}
                }
            }
        }
    }
    // Called when user presses STOP or system finishes automatically
    fun stopAndSaveRecording() {
        isListeningModeActive = false

        // Take the text accumulated in the POPUP
        val newText = popupDisplayText.trim()

        if (newText.isNotBlank()) {
            val oldDescription = uiState.description

            // APPEND LOGIC:
            // If description was empty -> Set new text
            // If it had content -> Add space + new text
            val finalDescription = if (oldDescription.isBlank()) {
                newText
            } else {
                "$oldDescription $newText"
            }

            // ONLY NOW update the main UI
            uiState = uiState.copy(description = finalDescription)
        }

        // Cleanup
        currentLoudness = 0f
        popupDisplayText = ""
    }

    // Called if user presses "Cancel" or clicks outside (without saving)
    fun stopRecordingAndDismiss() {
        isListeningModeActive = false
        currentLoudness = 0f
        popupDisplayText = ""
        // We do NOT update uiState.description here.
    }

    // 2. LOCATION FUNCTIONS
    /**
     * Captures the current GPS location and converts it to an address.
     * Updates the UI State with the result.
     */
    fun captureCurrentLocation() {
        launchCatching {
            // getCurrentLocation is suspend (OK). It waits for a fresh GPS update.
            val location = locationService.getCurrentLocation()

            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)

                // getAddressFromGeoPoint is NOW suspend (OK, because we are in launchCatching)
                // It performs Reverse Geocoding (Lat/Lng -> String address)
                val address = locationService.getAddressFromGeoPoint(geoPoint)

                // Update the UI state with the captured location data
                uiState = uiState.copy(location = geoPoint, locationAddress = address)

            } else {
                SnackbarManager.showMessage("Could not get location. Check GPS.")
            }
        }
    }


    // Called when user types in the Location Address field.
    fun onLocationQueryChange(newQuery: String) {
        // 1. Update text immediately so user sees what they type

        // If user types nothing, reset everything
        if (newQuery.isBlank()) {
            uiState = uiState.copy(
                locationAddress = "",
                location = null // Reset coordinate
            )
            locationPredictions = emptyList()
            searchJob?.cancel()
            return
        }

        // If user is writing, show suggestions and disable save button
        uiState = uiState.copy(
            locationAddress = newQuery,
            location = null // RESET COORDINATE
        )

        // 2. Cancel previous search if user keeps typing
        searchJob?.cancel()

        // 3. Start new search if query is long enough
        if (newQuery.length > 2) {
            searchJob = launchCatching {
                delay(500) // Wait 500ms (Debounce) to save API calls
                val results = locationService.getAutocompletePredictions(newQuery)
                locationPredictions = results
            }
        } else {
            locationPredictions = emptyList()
        }
    }


     // Called when user selects a specific address from the list.
    fun onLocationPredictionSelected(prediction: AutocompleteResult) {
        launchCatching {
            // 1. Update text to the full selected address
            val fullAddress = "${prediction.primaryText}, ${prediction.secondaryText}"
            uiState = uiState.copy(locationAddress = fullAddress)
            locationPredictions = emptyList() // Hide list

            // 2. Fetch Coordinates (Lat/Lng) from Google
            val location = locationService.getPlaceDetails(prediction.placeId)

            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                uiState = uiState.copy(location = geoPoint)
            } else {
                SnackbarManager.showMessage("Could not fetch coordinates for this place.")
            }
        }
    }

    // PHOTOS
    fun onImageSelected(uri: Uri) {
        uiState = uiState.copy(selectedImageUri = uri)
    }

    // 3. SAVE BOX
    fun onDoneClick(navigate: (String) -> Unit) {
        launchCatching {
            val newId = storageService.getNewBoxId()
            val newBox = Box(
                boxId = newId,
                title = uiState.title,
                description = uiState.description,
                isFragile = uiState.isFragile,
                fillStatus = uiState.fillStatus,
                secretNote = uiState.secretNote,
                location = uiState.location,
                locationAddress = uiState.locationAddress

            )
            storageService.saveBox(newBox, uiState.selectedImageUri)
            val targetRoute = if (uiState.selectedImageUri != null) {
                // Codifichiamo l'URI per non rompere la navigazione
                val encodedUri = URLEncoder.encode(
                    uiState.selectedImageUri.toString(),
                    StandardCharsets.UTF_8.toString()
                )
                "BoxDetailScreen/$newId?localUri=$encodedUri"
            } else {
                "BoxDetailScreen/$newId"
            }

            // 5. Andiamo!
            navigate(targetRoute)
        }
    }

}

// Helper data class for the UI State
data class NewBoxUiState(
    val title: String = "",
    val description: String = "",
    val isFragile: Boolean = false,
    val fillStatus: String = "GREEN", // Default Empty
    val secretNote: String = "",
    val location: GeoPoint? = null,
    val locationAddress: String = "",
    val selectedImageUri: Uri? = null
)