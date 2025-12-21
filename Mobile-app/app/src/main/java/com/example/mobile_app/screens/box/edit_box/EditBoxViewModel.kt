package com.example.mobile_app.screens.box.edit_box

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mobile_app.R
import com.example.mobile_app.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.model.Box
import com.example.mobile_app.model.service.SpeechService
import com.example.mobile_app.model.service.SpeechState
import com.example.mobile_app.model.service.StorageService
import com.example.mobile_app.screens.BoxAppViewModel



@HiltViewModel
class EditBoxViewModel @Inject constructor(
    private val storageService: StorageService,
    private val speechService: SpeechService,
    private val application: Application // Context needed for permission check
) : BoxAppViewModel() {

    var uiState by mutableStateOf(EditBoxUiState())
        private set

    // --- SPEECH TO TEXT STATE ---

    // Controls the visibility of the bottom sheet (Popup)
    // True = Listening loop is active. False = Stopped.
    // State for Speech UI
    // If true, show the BottomSheet (Popup)
    var isListeningModeActive by mutableStateOf(false)
        private set

    // Volume level for the Visualizer
    var currentLoudness by mutableFloatStateOf(0f)
        private set

    // TEMPORARY TEXT: This is what you see ONLY in the popup while speaking.
    // It does NOT touch uiState.description until you save.
    var popupDisplayText by mutableStateOf("")
        private set

    fun onTitleChange(newValue: String) { uiState = uiState.copy(title = newValue) }
    fun onDescriptionChange(newValue: String) { uiState = uiState.copy(description = newValue) }
    fun onSecretNoteChange(newValue: String) { uiState = uiState.copy(secretNote = newValue) }
    fun onFragileChange(newValue: Boolean) { uiState = uiState.copy(isFragile = newValue) }
    fun onFillStatusChange(newValue: String) { uiState = uiState.copy(fillStatus = newValue) }

    // --- SPEECH FUNCTIONS ---

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
                        val errorMessage = application.getString(R.string.generic_error)

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

    // Save box logic
    fun onDoneClick(popUpScreen: () -> Unit) {
        launchCatching {
            val newBox = Box(
                title = uiState.title,
                description = uiState.description,
                isFragile = uiState.isFragile,
                fillStatus = uiState.fillStatus,
                secretNote = uiState.secretNote
            )
            storageService.saveBox(newBox)
            popUpScreen() // Go back to list
        }
    }

}

// Helper data class for the UI State
data class EditBoxUiState(
    val title: String = "",
    val description: String = "",
    val isFragile: Boolean = false,
    val fillStatus: String = "GREEN", // Default Empty
    val secretNote: String = ""
)