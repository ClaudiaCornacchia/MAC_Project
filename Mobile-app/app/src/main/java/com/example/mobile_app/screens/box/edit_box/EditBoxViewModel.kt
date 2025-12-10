package com.example.mobile_app.screens.box.edit_box

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.model.Box
import com.example.mobile_app.model.service.StorageService
import com.example.mobile_app.screens.BoxAppViewModel


@HiltViewModel
class EditBoxViewModel @Inject constructor(
    private val storageService: StorageService
) : BoxAppViewModel() {

    var uiState by mutableStateOf(EditBoxUiState())
        private set

    fun onTitleChange(newValue: String) { uiState = uiState.copy(title = newValue) }
    fun onDescriptionChange(newValue: String) { uiState = uiState.copy(description = newValue) }
    fun onSecretNoteChange(newValue: String) { uiState = uiState.copy(secretNote = newValue) }
    fun onFragileChange(newValue: Boolean) { uiState = uiState.copy(isFragile = newValue) }
    fun onFillStatusChange(newValue: String) { uiState = uiState.copy(fillStatus = newValue) }

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