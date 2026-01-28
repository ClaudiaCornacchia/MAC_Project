package com.example.mobile_app.screens.box.new_box


// Android System & Permissions
import android.Manifest
import android.net.Uri
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.LocationOn

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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType

// Compose Runtime
import androidx.compose.runtime.Composable

// Compose UI Utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

// Hilt Dependency Injection
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobile_app.R
import com.example.mobile_app.SnackbarManager


import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBoxScreen(
    popUpScreen: () -> Unit,
    viewModel: NewBoxViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    // State to control if the dropdown Google Places is expanded (visible)
    // It should be expanded if we have predictions and the user is typing
    val isDropdownExpanded = viewModel.locationPredictions.isNotEmpty()

    // 1. Audio Permission Launcher
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

    // 2. Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // If granted, call the ViewModel to capture the GPS coordinates
                viewModel.captureCurrentLocation()
            } else {
                SnackbarManager.showMessage(context.getString(R.string.permission_needed))
            }
        }
    )

    //3. Camera permission launcher

    // Let the user pick an image from their gallery without permissions
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        // If the user selected an image, update the ViewModel
        if (uri != null) viewModel.onImageSelected(uri)
    }

    // Launch the camera when the user clicks the camera icon
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && viewModel.tempPhotoUri != null) {
            viewModel.onImageSelected(viewModel.tempPhotoUri!!)
        }
    }

    // Temporary URI
    fun launchCamera() {
        try {
            // Create temporary file
            val tempFile = java.io.File.createTempFile("temp_box_image", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }

            // Generate URI using file provider
            val authority = "${com.example.mobile_app.BuildConfig.APPLICATION_ID}.fileprovider"

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                authority,
                tempFile
            )

            // Save URI in ViewModel and launch
            viewModel.tempPhotoUri = uri
            cameraLauncher.launch(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error launching camera: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            android.widget.Toast.makeText(context, "Camera permission needed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }


    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New Box", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // 1. Title
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.onTitleChange(it) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Description
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

        // 3. Secret Note (Optional)
        OutlinedTextField(
            value = uiState.secretNote,
            onValueChange = { viewModel.onSecretNoteChange(it) },
            label = { Text("Secret Note (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // 4. PHOTO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showSheet = true }, // selector gallery or camera
            contentAlignment = Alignment.Center
        ) {
            if (uiState.selectedImageUri != null) {
                // if we have a selected image, show it
                AsyncImage(
                    model = uiState.selectedImageUri,
                    contentDescription = "Selected Box Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )


                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Tap to change",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                // if we don't have a selected image, show a placeholder
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Add a photo", color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // selector gallery or camera
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, top = 8.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "Select Photo",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Take a photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showSheet = false
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    )

                    // Option gallery
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showSheet = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4. Location
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { /* handled by input logic */ }
        ) {
            // The Input Field
            OutlinedTextField(
                value = uiState.locationAddress,
                onValueChange = { viewModel.onLocationQueryChange(it) },
                label = { Text("Location Address") },
                placeholder = { Text("Start typing street name...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryEditable,
                        enabled = true
                    ),
                trailingIcon = {
                    Row {
                        // Clear button
                        if (uiState.locationAddress.isNotBlank()) {
                            IconButton(onClick = {
                                // Resetta testo e coordinate
                                viewModel.onLocationQueryChange("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                        // GPS button!
                        IconButton(onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Use GPS",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true
            )

            // The Suggestions List
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { /* Optional: clear list on dismiss */ }
            ) {
                viewModel.locationPredictions.forEach { prediction ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(prediction.primaryText, style = MaterialTheme.typography.bodyLarge)
                                Text(prediction.secondaryText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        },
                        onClick = {
                            viewModel.onLocationPredictionSelected(prediction)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 5. Fragile Switch
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Is Fragile?")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = uiState.isFragile,
                onCheckedChange = { viewModel.onFragileChange(it) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // 6. Fill Status Selection
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
            enabled = uiState.title.isNotBlank() &&
                    uiState.description.isNotBlank() &&
                    (uiState.locationAddress.isBlank() || uiState.location != null)
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