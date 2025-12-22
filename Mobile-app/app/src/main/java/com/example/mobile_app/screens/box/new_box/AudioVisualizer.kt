package com.example.mobile_app.screens.box.new_box

//graphic component for audio spike


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    loudness: Float // Current raw dB from SpeechService
) {
    // 1. CAPTURE LATEST VALUE SAFELY
    val currentRawLoudness by rememberUpdatedState(loudness)

    // 2. HISTORY BUFFER (Scrolling Waveform)
    val amplitudes = remember { mutableStateListOf<Float>() }
    val maxBars = 45

    // 3. DYNAMIC STATE FOR AUTO-CALIBRATION
    // We track the highest volume seen recently to auto-scale the graph.
    // Default to 5.0 to avoid division by zero.
    var maxObservedVolume by remember { mutableFloatStateOf(5.0f) }

    // Smoothing variable for fluid movement
    var currentSmoothedLevel by remember { mutableFloatStateOf(0f) }

    // 4. OPTIMIZED UPDATE LOOP (20 FPS)
    LaunchedEffect(Unit) {
        while (isActive) {
            // Wait 50ms to prevent CPU Lag (approx 20 updates/sec)
            delay(50)

            val rawInput = currentRawLoudness

            // --- AUTO-CALIBRATION LOGIC ---
            // If current sound is louder than max, update max.
            if (rawInput > maxObservedVolume) {
                maxObservedVolume = rawInput
            } else {
                // Decay: Slowly lower max volume so bars don't stay tiny forever
                maxObservedVolume -= 0.1f
                if (maxObservedVolume < 3.0f) maxObservedVolume = 3.0f
            }

            // --- NORMALIZATION ---
            val noiseFloor = -1.5f // Ignore background noise
            var targetLevel = (rawInput - noiseFloor) / (maxObservedVolume - noiseFloor)

            // Clamp results
            if (targetLevel < 0.05f) targetLevel = 0.05f // Minimum dot size
            if (targetLevel > 1.0f) targetLevel = 1.0f

            // --- SMOOTHING (Interpolation) ---
            // Move 25% towards the target each frame for a fluid look
            currentSmoothedLevel += (targetLevel - currentSmoothedLevel) * 0.25f

            // Update List
            amplitudes.add(currentSmoothedLevel)
            if (amplitudes.size > maxBars) {
                amplitudes.removeAt(0)
            }
        }
    }

    // Colors
    val barColor = MaterialTheme.colorScheme.primary
    val highVolumeColor = Color.Red

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f

        if (amplitudes.isEmpty()) return@Canvas

        val totalGapWidth = 6f
        val availableWidth = canvasWidth - (totalGapWidth * (maxBars - 1))
        val barWidth = (availableWidth / maxBars).coerceAtLeast(2f)

        // Draw from Right to Left
        val startIndex = (maxBars - amplitudes.size).coerceAtLeast(0)

        amplitudes.forEachIndexed { index, amplitude ->
            val visualIndex = index + startIndex
            val x = visualIndex * (barWidth + totalGapWidth) + (barWidth / 2)

            val barHeight = amplitude * canvasHeight * 0.9f
            val currentColor = if (amplitude > 0.75f) highVolumeColor else barColor

            drawLine(
                color = currentColor,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}