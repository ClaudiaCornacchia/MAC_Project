package com.example.mobile_app.screens.box.edit_box


// Android System & Permissions
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Compose Foundation & Layouts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Compose Material Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic

// Compose Material 3 Components
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState

// Compose Runtime
import androidx.compose.runtime.Composable

// Compose UI Utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Hilt Dependency Injection
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobile_app.R
import com.example.mobile_app.SnackbarManager




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBoxScreen(
    popUpScreen: () -> Unit,
    viewModel: EditBoxViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    // Permission Launcher
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startListening()
            } else {
                // Show snackbar: Permission needed
                SnackbarManager.showMessage(context.getString(R.string.permission_needed))

            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New Box", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Title
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.onTitleChange(it) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        // Description
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.onDescriptionChange(it) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            // Add the Microphone Icon here
            trailingIcon = {
                IconButton(onClick = {
                    // Check for permission before starting
                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Speech to Text",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        // Secret Note (Optional)
        OutlinedTextField(
            value = uiState.secretNote,
            onValueChange = { viewModel.onSecretNoteChange(it) },
            label = { Text("Secret Note (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Fragile Switch
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Is Fragile?")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = uiState.isFragile,
                onCheckedChange = { viewModel.onFragileChange(it) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Fill Status Selection
        Text("Fill Status", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(
                selected = uiState.fillStatus == "GREEN",
                onClick = { viewModel.onFillStatusChange("GREEN") },
                label = { Text("Empty") }
            )
            FilterChip(
                selected = uiState.fillStatus == "YELLOW",
                onClick = { viewModel.onFillStatusChange("YELLOW") },
                label = { Text("Half") }
            )
            FilterChip(
                selected = uiState.fillStatus == "RED",
                onClick = { viewModel.onFillStatusChange("RED") },
                label = { Text("Full") }
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { viewModel.onDoneClick(popUpScreen) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.title.isNotBlank() && uiState.description.isNotBlank()
        ) {
            Text("Save Box")
        }
    }

    // 3. LISTENING SHEET (OVERLAY)
    // This sits outside the Column but inside the Composable function.
    // It appears on top of the UI when viewModel.isListening is true.
    // --- LISTENING SHEET ---
    if (viewModel.isListeningModeActive) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.stopAndSaveRecording() },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Listening...", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(16.dp))

                // The Visualizer with spikes (using loudness from VM)
                AudioVisualizer(loudness = viewModel.currentLoudness)

                Spacer(Modifier.height(16.dp))

                // Show what has been recognized so far in this session
                // We use 'popupDisplayText' here, NOT the main description
                Text(
                    text = viewModel.popupDisplayText.ifEmpty { "Say something..." },
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                // Stop & Save Button
                Button(
                    onClick = { viewModel.stopAndSaveRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Stop & Append Text")
                }

                // Cancel Button (Optional)
                TextButton(onClick = { viewModel.stopRecordingAndDismiss() }) {
                    Text("Cancel")
                }

                // Add some padding at the bottom for navigation bar
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}