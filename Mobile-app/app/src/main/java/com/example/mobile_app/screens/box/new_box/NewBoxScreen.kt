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
import androidx.compose.material3.*
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBoxScreen(
    navigate: (String) -> Unit,
    viewModel: NewBoxViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                SnackbarManager.showMessage("Permission needed")

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
                SnackbarManager.showMessage("Permission needed")
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

// UI CONTENT
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0) // Allows header to go behind status bar
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. MODERN HEADER
            ModernCreateHeader()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2. MAIN CARD
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // TEXT FIELDS

                        ModernTextField(
                            value = uiState.title,
                            onValueChange = { viewModel.onTitleChange(it) },
                            label = "Title"
                        )

                        ModernTextField(
                            value = uiState.description,
                            onValueChange = { viewModel.onDescriptionChange(it) },
                            label = "Description",
                            singleLine = false,
                            trailingIcon = {
                                IconButton(onClick = { recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                                    Icon(Icons.Filled.Mic, "Speech to Text", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(2.dp))

                        // 2. IMAGE

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.selectedImageUri != null) {
                                AsyncImage(
                                    model = uiState.selectedImageUri,
                                    contentDescription = "Selected Box Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Edit Overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Tap to add photo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // 4. LOCATION CARD
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SectionHeader(icon = Icons.Default.LocationOn, title = "Location")
                        Spacer(Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { /* handled by input logic */ }
                        ) {
                            ModernTextField(
                                value = uiState.locationAddress,
                                onValueChange = { viewModel.onLocationQueryChange(it) },
                                label = "Address",
                                placeholder = "Start typing street name...",
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = {
                                    Row {
                                        if (uiState.locationAddress.isNotBlank()) {
                                            IconButton(onClick = { viewModel.onLocationQueryChange("") }) {
                                                Icon(Icons.Default.Clear, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        IconButton(onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                                            Icon(Icons.Filled.LocationOn, "Use GPS", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { /* Optional */ },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                viewModel.locationPredictions.forEach { prediction ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(prediction.primaryText, style = MaterialTheme.typography.bodyLarge)
                                                Text(prediction.secondaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = { viewModel.onLocationPredictionSelected(prediction) },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                // 5. STATUS CARD
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SectionHeader(icon = Icons.Default.Info, title = "Status")

                        // Fragile
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer.copy(0.3f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Fragile Item", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = uiState.isFragile,
                                onCheckedChange = { viewModel.onFragileChange(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Fill Status
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Fill Level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip(
                                    text = "Empty", color = Color(0xFF43A047),
                                    isSelected = uiState.fillStatus == "GREEN",
                                    onClick = { viewModel.onFillStatusChange("GREEN") },
                                    modifier = Modifier.weight(1f)
                                )
                                StatusChip(
                                    text = "Half", color = Color(0xFFFFC107),
                                    isSelected = uiState.fillStatus == "YELLOW",
                                    onClick = { viewModel.onFillStatusChange("YELLOW") },
                                    modifier = Modifier.weight(1f)
                                )
                                StatusChip(
                                    text = "Full", color = Color(0xFFE53935),
                                    isSelected = uiState.fillStatus == "RED",
                                    onClick = { viewModel.onFillStatusChange("RED") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // SAVE BUTTON
                Button(
                    onClick = {
                        viewModel.onDoneClick { route ->
                            navigate(route) // Passiamo la rotta ricevuta dal ViewModel
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp,
                        disabledElevation = 0.dp
                    ),
                    enabled = uiState.title.isNotBlank() &&
                            uiState.description.isNotBlank() &&
                            (uiState.locationAddress.isBlank() || uiState.location != null),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Box", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null)
                }

                Spacer(Modifier.height(10.dp))
            }
        }

        //  BOTTOM SHEETS

        // Photo Picker Sheet
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, top = 8.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text("Select Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                    ListItem(
                        headlineContent = { Text("Take a photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showSheet = false; cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showSheet = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                    )
                }
            }
        }

        // Listening Sheet (Overlay)
        if (viewModel.isListeningModeActive) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.stopAndSaveRecording() },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Listening...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))

                    // The Visualizer (Assumption: AudioVisualizer is a custom component you have)
                    AudioVisualizer(loudness = viewModel.currentLoudness)

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = viewModel.popupDisplayText.ifEmpty { "Say something..." },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = { viewModel.stopAndSaveRecording() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Stop & Add Text")
                    }
                    TextButton(onClick = { viewModel.stopRecordingAndDismiss() }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// HELPER COMPONENTS

@Composable
private fun ModernCreateHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 15.dp)
    ) {
        Column {
            Text(
                text = "New Box",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun StatusChip(
    text: String, color: Color, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = textColor, fontWeight = FontWeight.SemiBold)
        }
    }
}