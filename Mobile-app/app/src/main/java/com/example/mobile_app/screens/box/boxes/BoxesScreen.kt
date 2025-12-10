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
import com.example.mobile_app.EDIT_BOX_SCREEN
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

@Composable
fun BoxesScreen(
    openScreen: (String) -> Unit,
    viewModel: BoxesViewModel = hiltViewModel()
) {
    val boxes by viewModel.boxes.collectAsState(emptyList())

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
                    onClick = { openScreen(EDIT_BOX_SCREEN) },
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
                                // Vibra per 500 millisecondi
                                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                // Vecchio metodo per Android < 8.0
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