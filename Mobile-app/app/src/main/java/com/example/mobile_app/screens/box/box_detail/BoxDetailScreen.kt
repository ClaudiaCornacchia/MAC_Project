package com.example.mobile_app.screens.box.box_detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.KeyboardArrowDown // Icon for download
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import com.example.mobile_app.SIGN_IN_SCREEN
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Refresh
import android.Manifest
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.draw.clip
import coil.compose.SubcomposeAsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.key

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(
    boxId: String,
    openScreen: (String) -> Unit,
    popUpScreen: () -> Unit,
    viewModel: BoxDetailViewModel = hiltViewModel()
) {
    val box = viewModel.box
    val context = LocalContext.current

    // update box
    val draft = viewModel.draftBox
    val isEditing = viewModel.isEditing
    val isLoading = viewModel.isLoading

    if (!viewModel.isUserAuthorized) {
        LaunchedEffect(Unit) {
            openScreen(SIGN_IN_SCREEN)
        }
        return
    }

    // Initialize the ViewModel
    LaunchedEffect(boxId) {
        viewModel.initialize(boxId)
    }

    val titleToShow = if (isEditing) draft.title else box?.title ?: ""
    val descToShow = if (isEditing) draft.description else box?.description ?: ""
    val secretNoteToShow = if (isEditing) draft.secretNote else box?.secretNote ?: ""
    val addressToShow = if (isEditing) draft.locationAddress else box?.locationAddress ?: ""

    val currentImage = viewModel.newPhotoUri
        ?: (if (isEditing) draft.imageUrl else box?.imageUrl)

    var showPhotoSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Permission launcher
    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.updateLocation()
    }

    // PHOTO

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.updateDraftPhoto(uri) }

    // Launch the camera when the user clicks the camera icon
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && viewModel.tempPhotoUri != null) {
            viewModel.updateDraftPhoto(viewModel.tempPhotoUri!!)
        }
    }

    // Temporary URI
    fun launchCamera() {
        try {
            // Create temporary file
            val tempFile =
                java.io.File.createTempFile("temp_box_image", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit() // Si cancella da solo se chiudi l'app
                }

            // Generate URI using file provider
            val authority = "${context.packageName}.fileprovider"

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
            android.widget.Toast.makeText(
                context,
                "Error: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            android.widget.Toast.makeText(
                context,
                "Camera permission needed",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editing Box" else "Box Details") },
                navigationIcon = {
                    IconButton(onClick = popUpScreen) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {

                    if (isEditing) {
                        IconButton(onClick = { viewModel.cancelEditing() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = { viewModel.saveEditChanges() }) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    } else {

                        if (box != null) {
                            IconButton(onClick = { viewModel.startEditing() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (box != null) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 1. TITLE
                        EditableTextField(
                            label = "Title",
                            value = titleToShow,
                            isEditing = isEditing,
                            onValueChange = { viewModel.updateDraftTitle(it) },
                            textStyle = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(8.dp))

                        //2. IMAGE
                        val imageUrlString = currentImage.toString()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = isEditing) { showPhotoSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                // CASE A: image is uploading
                                imageUrlString == "UPLOADING" -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Uploading photo...",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                // CASE B: image is available, we just need to wait for the download, use SubcomposeAsyncImage
                                imageUrlString.isNotEmpty() && imageUrlString != "null" -> {
                                    SubcomposeAsyncImage(
                                        model = currentImage,
                                        contentDescription = "Box Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(Modifier.size(32.dp))
                                            }
                                        },
                                        error = {
                                            Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.BrokenImage,
                                                    contentDescription = "Error",
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                    )
                                }

                                // CASE C: no image to display
                                else -> {
                                }
                            }
                            // Overlay  when editing
                            if (isEditing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PhotoCamera,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "Tap to change",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // 3. DESCRIPTION
                        EditableTextField(
                            label = "Description",
                            value = descToShow,
                            isEditing = isEditing,
                            onValueChange = { viewModel.updateDraftDescription(it) },
                            maxLines = 5
                        )
                        Spacer(modifier = Modifier.height(16.dp))


                        // 4. STATUS INFO
                        if (isEditing) {
                            Text(
                                "Box Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Fragile Item", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = draft.isFragile,
                                    onCheckedChange = { viewModel.updateDraftFragile(it) }
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statusOptions = listOf(
                                    "GREEN" to "Empty",
                                    "YELLOW" to "Half",
                                    "RED" to "Full"
                                )
                                statusOptions.forEach { (dbValue, labelText) ->
                                    FilterChip(

                                        selected = draft.fillStatus == dbValue,
                                        onClick = { viewModel.updateDraftStatus(dbValue) },
                                        label = { Text(labelText) },

                                        leadingIcon = if (draft.fillStatus == dbValue) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null,

                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when(dbValue) {
                                                "GREEN" -> Color.Green.copy(alpha = 0.2f)
                                                "YELLOW" -> Color.Yellow.copy(alpha = 0.2f)
                                                "RED" -> Color.Red.copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.secondaryContainer
                                            }
                                        )
                                    )
                                }
                            }

                        } else {

                            Row {
                                if (box.isFragile) {
                                    SuggestionChip(onClick = {}, label = { Text("Fragile ⚠️") })
                                    Spacer(Modifier.width(8.dp))
                                }

                                val (statusColor, statusText) = when (box.fillStatus) {
                                    "GREEN" -> Pair(Color.Green, "Empty")
                                    "YELLOW" -> Pair(Color.Yellow, "Half Empty") // As requested
                                    "RED" -> Pair(Color.Red, "Full")
                                    else -> Pair(Color.Gray, "Unknown")
                                }

                                SuggestionChip(
                                    onClick = {},
                                    // The Icon parameter allows us to put the Dot to the left of the text
                                    icon = {
                                        // Draw a simple circle
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier.size(10.dp), // Size of the dot
                                            onDraw = {
                                                drawCircle(color = statusColor)
                                            }
                                        )
                                    },
                                    label = { Text(statusText) }
                                )
                            }
                        }

                        // 5. HumanId (Number of the box)
                        if(!isEditing) {
                            if (box.humanId.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Text("Number:", style = MaterialTheme.typography.labelMedium)
                                Text(box.humanId, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(24.dp))

                        // 6. SHARE
                        if (!isEditing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Shared with:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = { viewModel.onShareClick() }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Add Person",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (viewModel.sharedNames.isNotEmpty()) {
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    viewModel.sharedNames.forEach { name ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(name) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Private (Only you)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic
                                )
                            }

                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(24.dp))
                        }

                        // 7. MAP SECTION
                        Text("Location", style = MaterialTheme.typography.titleMedium)

                        if (isEditing) {
                            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

                            Column {
                                OutlinedTextField(
                                    value = addressToShow,
                                    onValueChange = { viewModel.onLocationQueryChange(it) },
                                    label = { Text("Address") },
                                    modifier = Modifier
                                        .fillMaxWidth(),

                                    singleLine = true,
                                    trailingIcon = {

                                        if (addressToShow.isNotEmpty()) {
                                            IconButton(onClick = {

                                                if (viewModel.locationPredictions.isNotEmpty()) {
                                                    viewModel.clearLocationSuggestions()
                                                } else {
                                                    viewModel.onLocationQueryChange("")
                                                }
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear")
                                            }
                                        }
                                    },

                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        viewModel.clearLocationSuggestions()
                                        focusManager.clearFocus()
                                    })
                                )

                                AnimatedVisibility(visible = viewModel.locationPredictions.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .heightIn(max = 200.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        LazyColumn {
                                            items(viewModel.locationPredictions) { prediction ->
                                                ListItem(
                                                    headlineContent = { Text(prediction.primaryText) },
                                                    supportingContent = { Text(prediction.secondaryText) },
                                                    leadingContent = { Icon(Icons.Default.Place, null) },
                                                    modifier = Modifier.clickable {
                                                        viewModel.onLocationPredictionSelected(prediction)
                                                        focusManager.clearFocus()
                                                    }
                                                )
                                                HorizontalDivider(thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Address",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = addressToShow.ifEmpty { "-" },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }


                        Spacer(Modifier.height(8.dp))

                            if (box.location != null) {
                                val targetPosition = LatLng(box.location!!.latitude, box.location!!.longitude)


                                    // Camera State (Zoom level)
                                    val cameraPositionState = rememberCameraPositionState {
                                        position =
                                            CameraPosition.fromLatLngZoom(targetPosition, 15f)
                                    }

                                    val markerState = rememberMarkerState(position = targetPosition)

                                    LaunchedEffect(targetPosition) {
                                        // Animate the camera to the new location
                                        markerState.position = targetPosition
                                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(targetPosition, 15f))
                                    }

                                    // Map Component
                                    GoogleMap(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        cameraPositionState = cameraPositionState
                                    ) {
                                        Marker(
                                            state = markerState,
                                            title = box.title,
                                            snippet = box.locationAddress
                                        )
                                    }

                            } else {
                                Text(
                                    "No location data available.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.height(16.dp))

                            // Update Location button
                            Button(
                                onClick = { gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Update GPS Position Here")
                            }
                        }


                        // 8. QR CODE
                        if (!isEditing) {
                            Spacer(Modifier.height(16.dp))
                            if (box.qrCodeUrl.isNotBlank()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("QR Code", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))

                                    // Display the Image from URL
                                    AsyncImage(
                                        model = box.qrCodeUrl,
                                        contentDescription = "QR Code of the Box",
                                        modifier = Modifier
                                            .size(200.dp) // Fixed size for the QR
                                            .padding(4.dp),
                                    )

                                    Spacer(Modifier.height(16.dp))

                                    // Download Button
                                    OutlinedButton(
                                        onClick = { viewModel.downloadQrCode(context) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Download QR Code")
                                    }
                                }
                            } else {
                                // Fallback if QR is missing (e.g., server error during creation)
                                Text(
                                    text = "QR Code not available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                    }
                }
            }
        }

        if (viewModel.showShareDialog) {
            ShareBoxDialog(
                onDismiss = { viewModel.onShareDismiss() },
                onConfirm = { email -> viewModel.onShareConfirm(email) }
            )
        }
        // Photo Bottom Sheet
        if (showPhotoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhotoSheet = false },
                sheetState = sheetState
            ) {
                Column(Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                    Text("Update Photo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            showPhotoSheet = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Take a Photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showPhotoSheet = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        }
    }
}

    @Composable
    fun EditableTextField(
        label: String,
        value: String,
        isEditing: Boolean,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        maxLines: Int = 1,
        textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = modifier.fillMaxWidth(),
                maxLines = maxLines
            )
        } else {
            Column(modifier = modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = value.ifEmpty { "-" }, style = textStyle)
            }
        }
    }

    @Composable
    fun ShareBoxDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
        var email by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Share Box") },
            text = {
                Column {
                    Text("Enter user email:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { onConfirm(email) }) { Text("Share") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }