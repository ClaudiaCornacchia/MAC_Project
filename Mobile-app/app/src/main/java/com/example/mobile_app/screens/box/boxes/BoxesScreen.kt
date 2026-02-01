package com.example.mobile_app.screens.box.boxes


import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobile_app.NEW_BOX_SCREEN
import com.example.mobile_app.SCAN_QR_SCREEN
import com.example.mobile_app.model.Box
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesScreen(
    openScreen: (String) -> Unit,
    viewModel: BoxesViewModel = hiltViewModel()
) {
    // 1. Observe the FILTERED list (Logic is handled in the ViewModel)
    val boxes by viewModel.boxes.collectAsState()

    // 2. Observe UI states for the Search Bar and Filter Chip
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isUnusedFilterActive by viewModel.showUnusedOnly.collectAsState()

    // Animation state for FAB appearance
    var fabVisible by remember { mutableStateOf(false) }

    val currentUserId = viewModel.currentUserId
    val ownerNames = viewModel.ownerNames

    LaunchedEffect(Unit) {
        fabVisible = true
    }

    // Scan qr and add box buttons
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButtons(
                    onScanClick = { openScreen(SCAN_QR_SCREEN) },
                    onAddClick = { openScreen(NEW_BOX_SCREEN) }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)

        ) {
            // Header section with gradient
            ModernHeader()

            // Search and Filter section
            SearchAndFilterSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                isUnusedFilterActive = isUnusedFilterActive,
                onToggleUnusedFilter = { viewModel.toggleUnusedFilter() }
            )

            // Boxes list or empty state
            if (boxes.isEmpty()) {
                EmptyState(hasActiveFilters = searchQuery.isNotEmpty() || isUnusedFilterActive)
            } else {
                BoxesList(
                    boxes = boxes,
                    currentUserId = currentUserId,
                    ownerNames = ownerNames,
                    onBoxClick = { boxId -> openScreen("BoxDetailScreen/$boxId") }
                )
            }
        }
    }
}

@Composable
private fun ModernHeader() {
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
                text = "My Boxes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

        }
    }
}

@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isUnusedFilterActive: Boolean,
    onToggleUnusedFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
    ) {
        // Modern Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search by name or description...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isUnusedFilterActive,
                onClick = onToggleUnusedFilter,
                shape = CircleShape,
                label = {
                    Text(
                        text = "Unused",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isUnusedFilterActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isUnusedFilterActive) Icons.Default.CheckCircle else Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                enabled = true,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isUnusedFilterActive,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    borderWidth = 1.dp
                )
            )
        }
    }
}

@Composable
private fun BoxesList(
    boxes: List<Box>,
    currentUserId: String,
    ownerNames: Map<String, String>,
    onBoxClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to top when boxes change
    LaunchedEffect(boxes.size) {
        if (boxes.isNotEmpty()) {
             listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(boxes, key = { it.boxId }) { box ->
            val ownerName = ownerNames[box.ownerId]

            ModernBoxCard(
                box = box,
                currentUserId = currentUserId,
                ownerName = ownerName,
                onClick = { onBoxClick(box.boxId) }
            )
        }

        // Bottom spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun getConsistentPastelColor(id: String): Color {
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )

    val index = (id.hashCode() % colors.size).let { if (it < 0) -it else it }
    return colors[index]
}

@Composable
private fun ModernBoxCard(
    box: Box,
    currentUserId: String,
    ownerName: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val isSharedWithMe = box.ownerId != currentUserId


    // Scale animation on press
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Log.d("BoxItem", "Box: ${box.title}, Fragile: ${box.isFragile}, Fill: ${box.fillStatus}")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .scale(scale)
            .clickable(
                onClick = {
                    if (box.isFragile) {
                        scope.launch {
                            // Vibrate for fragile items
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            if (vibrator.hasVibrator()) {
                                if (Build.VERSION.SDK_INT >= 26) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(300)
                                }
                            }

                            // Shake animation
                            val shakeStrength = 15f
                            val speed = spring<Float>(stiffness = Spring.StiffnessHigh)

                            offsetX.animateTo(shakeStrength, animationSpec = speed)
                            offsetX.animateTo(-shakeStrength, animationSpec = speed)
                            offsetX.animateTo(shakeStrength / 2, animationSpec = speed)
                            offsetX.animateTo(0f, animationSpec = speed)

                            onClick()
                        }
                    } else {
                        onClick()
                    }
                },
                onClickLabel = "Open box details"
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Internal padding for the card
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon section with colored background (on the left)
            BoxIconSection(boxId = box.humanId)

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Content section (Title, fragile, ID, owner)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title and fragile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title
                    Text(
                        text = box.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Fragile indicator

                    // Human ID badge
                    if (box.humanId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "#${box.humanId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }


                }
                Spacer(modifier = Modifier.height(4.dp))


                // SHARED OWNER BADGE
                if (isSharedWithMe) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(0.7f)
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        // Nome
                        Text(
                            text = ownerName ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }


            // Fill status (empty, full, half) and human ID
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                if (box.isFragile) {

                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Fragile",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(18.dp)
                )

                }

                // Fill status indicator
                FillStatusIndicator(fillStatus = box.fillStatus)



            }
        }
    }
}


@Composable
private fun BoxIconSection(boxId: String) {
    val backgroundColor = getConsistentPastelColor(boxId)

    val iconColor = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Inventory2,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun FillStatusIndicator(fillStatus: String) {
    val (text, color) = when (fillStatus.uppercase()) {
        "RED" -> "Full" to Color(0xFFE53935)
        "YELLOW" -> "Half" to Color(0xFFFFC107)
        "GREEN" -> "Empty" to Color(0xFF43A047)
        else -> "N/A" to Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        shape = CircleShape
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,

            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyState(hasActiveFilters: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        Icon(
            imageVector = if (hasActiveFilters) Icons.Outlined.SearchOff else Icons.Outlined.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (hasActiveFilters) "No boxes found" else "No boxes yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasActiveFilters)
                "Try adjusting your search"
            else
                "Create your first box to get started!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun FloatingActionButtons(
    onScanClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Scan QR FAB
        SmallFloatingActionButton(
            onClick = onScanClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = "Scan QR Code",
                modifier = Modifier.size(24.dp)
            )
        }

        // Add Box FAB
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add Box",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
