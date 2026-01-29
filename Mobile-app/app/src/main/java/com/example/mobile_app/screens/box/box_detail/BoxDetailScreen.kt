package com.example.mobile_app.screens.box.box_detail

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.mobile_app.BOXES_SCREEN
import com.example.mobile_app.SIGN_IN_SCREEN
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale

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

    val currentImage = viewModel.newPhotoUri ?: (if (isEditing) draft.imageUrl else box?.imageUrl)

    var showPhotoSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Permission launcher
    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.updateLocation()
    }

    // Delete dialog
    val showDeleteDialog = viewModel.showDeleteDialog

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
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)

            // Save URI in ViewModel and launch
            viewModel.tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            android.widget.Toast.makeText(context, "Camera permission needed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteCancel() },
            title = { Text(text = "Delete Box") },
            text = {
                Text("Are you sure you want to delete this box? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Perform delete and navigate back upon success
                        viewModel.deleteBox { openScreen(BOXES_SCREEN)}
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDeleteCancel() }) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) }
        )
    }


    // Share dialog
    if (viewModel.showShareDialog) {
        ModernShareDialog(
            onDismiss = { viewModel.onShareDismiss() },
            onConfirm = { email -> viewModel.onShareConfirm(email) }
        )
    }

    // Photo bottom sheet
    if (showPhotoSheet) {
        PhotoBottomSheet(
            sheetState = sheetState,
            onDismiss = { showPhotoSheet = false },
            onGalleryClick = {
                showPhotoSheet = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCameraClick = {
                showPhotoSheet = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }


    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ModernDetailTopBar(
                isEditing = isEditing,
                isLoading = isLoading,
                hasBox = box != null,
                onBackClick = popUpScreen,
                onEditClick = { viewModel.startEditing() },
                onCancelClick = { viewModel.cancelEditing() },
                onSaveClick = { viewModel.saveEditChanges() },
                onDeleteClick = { viewModel.onDeleteClick() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (box != null) {
            BoxDetailContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                box = box,
                draft = draft,
                isEditing = isEditing,
                titleToShow = titleToShow,
                descToShow = descToShow,
                addressToShow = addressToShow,
                currentImage = currentImage,
                viewModel = viewModel,
                onPhotoClick = { showPhotoSheet = true },
                gpsPermissionLauncher = gpsPermissionLauncher
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDetailTopBar(
    isEditing: Boolean,
    isLoading: Boolean,
    hasBox: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isEditing)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.background
        )
    )

    TopAppBar(
        modifier = Modifier.background(headerGradient),
        windowInsets = WindowInsets(0),

        title = {
            Text(
                text = if (isEditing) "Editing Box" else "Box Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            if (isEditing) {
                // Cancel button
                IconButton(onClick = onCancelClick) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
                // Save button
                IconButton(onClick = onSaveClick, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (hasBox) {
                // Edit button
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                // Delete button
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Box",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )

    )
}

@Composable
private fun BoxDetailContent(
    modifier: Modifier = Modifier,
    box: com.example.mobile_app.model.Box,
    draft: com.example.mobile_app.model.Box,
    isEditing: Boolean,
    titleToShow: String,
    descToShow: String,
    addressToShow: String,
    currentImage: Any?,
    viewModel: BoxDetailViewModel,
    onPhotoClick: () -> Unit,
    gpsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Info Card
        ModernInfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title
                ModernEditableField(
                    label = "Title",
                    value = titleToShow,
                    isEditing = isEditing,
                    onValueChange = { viewModel.updateDraftTitle(it) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Image
                BoxImageSection(
                    currentImage = currentImage,
                    isEditing = isEditing,
                    onPhotoClick = onPhotoClick
                )

                // Description
                ModernEditableField(
                    label = "Description",
                    value = descToShow,
                    isEditing = isEditing,
                    onValueChange = { viewModel.updateDraftDescription(it) },
                    maxLines = 5
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Status Section
                BoxStatusSection(
                    box = box,
                    draft = draft,
                    isEditing = isEditing,
                    viewModel = viewModel
                )

                // Human ID (when not editing)
                if (!isEditing && box.humanId.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ModernLabelValue(label = "Box Number", value = "#${box.humanId}")
                }
            }
        }

        // Sharing Card
        if (!isEditing) {
            ModernSharingCard(
                sharedNames = viewModel.sharedNames,
                onShareClick = { viewModel.onShareClick() }
            )
        }

        // Location Card
        ModernLocationCard(
            box = box,
            draft = draft,
            isEditing = isEditing,
            addressToShow = addressToShow,
            viewModel = viewModel,
            gpsPermissionLauncher = gpsPermissionLauncher
        )

        // QR Code Card
        if (!isEditing && box.qrCodeUrl.isNotBlank()) {
            ModernQRCodeCard(
                qrCodeUrl = box.qrCodeUrl,
                onDownload = { viewModel.downloadQrCode(context) }
            )
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ModernInfoCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun BoxImageSection(
    currentImage: Any?,
    isEditing: Boolean,
    onPhotoClick: () -> Unit
) {
    val imageUrlString = currentImage.toString()
    val scale by animateFloatAsState(
        targetValue = if (isEditing) 0.98f else 1f,
        animationSpec = tween(200),
        label = "imageScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable(enabled = isEditing) { onPhotoClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUrlString == "UPLOADING" -> {
                UploadingIndicator()
            }
            imageUrlString.isNotEmpty() && imageUrlString != "null" -> {
                SubcomposeAsyncImage(
                    model = currentImage,
                    contentDescription = "Box Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { LoadingImagePlaceholder() },
                    error = { ErrorImagePlaceholder() }
                )
            }
            else -> {
                EmptyImagePlaceholder()
            }
        }

        // Edit overlay
        if (isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        "Tap to change photo",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxStatusSection(
    box: com.example.mobile_app.model.Box,
    draft: com.example.mobile_app.model.Box,
    isEditing: Boolean,
    viewModel: BoxDetailViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Box Status",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        if (isEditing) {
            // Fragile switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (draft.isFragile) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Fragile Item",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = draft.isFragile,
                        onCheckedChange = { viewModel.updateDraftFragile(it) }
                    )
                }
            }

            // Fill status chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOptions = listOf(
                    "GREEN" to Pair("Empty", Color(0xFF43A047)),
                    "YELLOW" to Pair("Half", Color(0xFFFFC107)),
                    "RED" to Pair("Full", Color(0xFFE53935))
                )
                statusOptions.forEach { (dbValue, labelData) ->
                    val (labelText, color) = labelData
                    FilterChip(
                        selected = draft.fillStatus == dbValue,
                        onClick = { viewModel.updateDraftStatus(dbValue) },
                        label = { Text(labelText, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = if (draft.fillStatus == dbValue) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color,
                            selectedLeadingIconColor = color
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = draft.fillStatus == dbValue,
                            borderColor = color.copy(alpha = 0.3f),
                            selectedBorderColor = color,
                            borderWidth = 1.5.dp
                        )
                    )
                }
            }
        } else {
            // Display mode
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (box.isFragile) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Fragile", fontWeight = FontWeight.SemiBold) },
                        icon = {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.error,
                            iconContentColor = MaterialTheme.colorScheme.error
                        )
                    )
                }

                val (statusColor, statusText) = when (box.fillStatus) {
                    "GREEN" -> Pair(Color(0xFF43A047), "Empty")
                    "YELLOW" -> Pair(Color(0xFFFFC107), "Half")
                    "RED" -> Pair(Color(0xFFE53935), "Full")
                    else -> Pair(Color.Gray, "Unknown")
                }

                SuggestionChip(
                    onClick = {},
                    icon = {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = statusColor)
                        }
                    },
                    label = { Text(statusText, fontWeight = FontWeight.SemiBold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = statusColor.copy(alpha = 0.15f),
                        labelColor = statusColor
                    ),
                    border = BorderStroke(1.dp, statusColor)
                )
            }
        }
    }
}

@Composable
private fun ModernSharingCard(
    sharedNames: List<String>,
    onShareClick: () -> Unit
) {
    ModernInfoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Shared with",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                if (sharedNames.isEmpty()) {
                    Text(
                        "Private (Only you)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            FilledIconButton(
                onClick = onShareClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (sharedNames.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sharedNames.forEach { name ->
                    AssistChip(
                        onClick = {},
                        label = { Text(name) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernLocationCard(
    box: com.example.mobile_app.model.Box,
    draft: com.example.mobile_app.model.Box,
    isEditing: Boolean,
    addressToShow: String,
    viewModel: BoxDetailViewModel,
    gpsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    ModernInfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isEditing) {
                LocationEditSection(
                    addressToShow = addressToShow,
                    viewModel = viewModel
                )
            } else {
                ModernLabelValue(label = "Address", value = addressToShow.ifEmpty { "No address set" })

                if (box.location != null) {
                    Spacer(Modifier.height(8.dp))
                    val targetPosition = LatLng(box.location!!.latitude, box.location!!.longitude)
                    MapSection(targetPosition = targetPosition, boxTitle = box.title, address = box.locationAddress)
                }

                Button(
                    onClick = { gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Update GPS Position")
                }
            }
        }
    }
}

@Composable
private fun LocationEditSection(
    addressToShow: String,
    viewModel: BoxDetailViewModel
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column {
        OutlinedTextField(
            value = addressToShow,
            onValueChange = { viewModel.onLocationQueryChange(it) },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
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
            shape = RoundedCornerShape(12.dp),
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
                    .padding(top = 8.dp)
                    .heightIn(max = 200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
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
                        if (prediction != viewModel.locationPredictions.last()) {
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapSection(targetPosition: LatLng, boxTitle: String, address: String) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetPosition, 15f)
    }
    val markerState = rememberMarkerState(position = targetPosition)

    LaunchedEffect(targetPosition) {
        markerState.position = targetPosition
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(targetPosition, 15f))
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp)),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = markerState,
            title = boxTitle,
            snippet = address
        )
    }
}

@Composable
private fun ModernQRCodeCard(
    qrCodeUrl: String,
    onDownload: () -> Unit
) {
    ModernInfoCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                AsyncImage(
                    model = qrCodeUrl,
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp)
                )
            }

            OutlinedButton(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download QR Code")
            }
        }
    }
}

@Composable
private fun ModernEditableField(
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
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    } else {
        ModernLabelValue(label = label, value = value.ifEmpty { "—" }, textStyle = textStyle)
    }
}

@Composable
private fun ModernLabelValue(
    label: String,
    value: String,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Image placeholders
@Composable
private fun UploadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Text(
            "Uploading photo...",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoadingImagePlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(32.dp))
    }
}

@Composable
private fun ErrorImagePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.BrokenImage,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Failed to load image",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyImagePlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            "No image",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// Dialogs
@Composable
private fun ModernDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp).size(32.dp)
                )
            }
        },
        title = { Text("Delete Box?", fontWeight = FontWeight.Bold) },
        text = { Text("This action cannot be undone. The box and all its data will be permanently deleted.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ModernShareDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).size(28.dp)
                )
            }
        },
        title = { Text("Share Box", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the email address of the person you want to share this box with:")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                "Update Photo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Surface(
                onClick = onGalleryClick,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp).size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Choose from Gallery",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = onCameraClick,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(12.dp).size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Take a Photo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}