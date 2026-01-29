package com.example.mobile_app.screens.box.boxes

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobile_app.NEW_BOX_SCREEN
import com.example.mobile_app.model.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.remember
import com.example.mobile_app.SCAN_QR_SCREEN
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import android.os.VibrationEffect
import android.os.Build
import android.os.Vibrator
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*


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

    Scaffold(
        floatingActionButton = {
            // We use a Column to stack buttons vertically
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // 1. SCAN BUTTON (Secondary)
                FloatingActionButton(
                    onClick = { openScreen(SCAN_QR_SCREEN) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.QrCodeScanner, "Scan QR")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. ADD BUTTON (Primary)
                FloatingActionButton(
                    onClick = { openScreen(NEW_BOX_SCREEN) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, "Add Box")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // SEARCH & FILTER SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // A. Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search boxes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        // Show Clear ('X') button only if there is text
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // B. Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isUnusedFilterActive,
                        onClick = { viewModel.toggleUnusedFilter() },
                        label = { Text("Unused > 1 Year") },
                        leadingIcon = if (isUnusedFilterActive) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }

            // LIST SECTION
            if (boxes.isEmpty()) {
                Text(
                    text = "No boxes yet. Add one!",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(boxes, key = { it.boxId }) { box ->
                        BoxItem(
                            box = box,
                            onBoxClick = { openScreen("BoxDetailScreen/${box.boxId}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoxItem(box: Box, onBoxClick: () -> Unit) {

    // Log the info of boxes
    Log.d("BoxItem", "Box title: ${box.title}")
    Log.d("BoxItem", "Box description: ${box.description}")
    Log.d("BoxItem", "Box fragile status: ${box.isFragile}")
    Log.d("BoxItem", "Box fill status: ${box.fillStatus}")




    // 1. ANIMATION STATE: Tracks the X offset (horizontal movement)
    val offsetX = remember { Animatable(0f) }

    // 2. COROUTINE SCOPE: To launch the animation
    val scope = rememberCoroutineScope()

    // 3.
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(8.dp, 4.dp)
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .clickable {
                if (box.isFragile) {
                    scope.launch {
                        // A. VIBRATE (LongPress simulates a heavy/warning vibration)
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                        if (vibrator.hasVibrator()) {
                            if (Build.VERSION.SDK_INT >= 26) {
                                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                vibrator.vibrate(500)
                            }
                        }
                        // B. ANIMATE (Shake Effect)
                        // Move right -> left -> right -> center rapidly
                        val shakeStrength = 20f // Pixels to move
                        val speed = spring<Float>(stiffness = Spring.StiffnessHigh) // Fast speed

                        offsetX.animateTo(shakeStrength, animationSpec = speed)
                        offsetX.animateTo(-shakeStrength, animationSpec = speed)
                        offsetX.animateTo(shakeStrength / 2, animationSpec = speed)
                        offsetX.animateTo(0f, animationSpec = speed)

                        // C. NAVIGATE after a tiny delay to let user see the shake
                        onBoxClick()
                    }
                } else {
                    // Not fragile? Just navigate immediately
                    onBoxClick()
                }
            }

    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = box.title, style = MaterialTheme.typography.titleMedium)

                // Show Human ID if available
                if (box.humanId.isNotBlank()) {
                    Text(text = "#${box.humanId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                Text(text = box.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }

            // Visual indicator for Fragile
            if (box.isFragile) {
                // Log
                Log.d("BoxItem", "Fragile box detected: ${box.title}")

                // You can also add a color tint to the warning icon
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Fragile",
                    tint = Color.Red,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}