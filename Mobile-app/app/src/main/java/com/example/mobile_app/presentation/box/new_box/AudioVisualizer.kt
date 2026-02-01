package com.example.mobile_app.presentation.box.new_box

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.pow
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

    // 3. DYNAMIC RANGE
    var maxObservedVolume by remember { mutableFloatStateOf(15.0f) }

    // 4. OPTIMIZED UPDATE LOOP (20 FPS)
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(50)

            val rawInput = currentRawLoudness

            // Auto calibration
            if (rawInput > maxObservedVolume) {
                maxObservedVolume = rawInput * 1.2f // Leave margine
            } else {
                // Low decay to keep stability
                maxObservedVolume -= 0.15f
                if (maxObservedVolume < 10.0f) maxObservedVolume = 10.0f
            }

            // Normalization
            val noiseFloor = -1.0f
            val adjustedInput = (rawInput - noiseFloor).coerceAtLeast(0f)

            var targetLevel = (adjustedInput / maxObservedVolume).coerceIn(0f, 1f)

            // Curve exponential
            targetLevel = targetLevel.pow(1.8f)

            // Soglia minima
            if (targetLevel < 0.03f) targetLevel = 0.03f

            // No smoothing
            amplitudes.add(targetLevel)
            if (amplitudes.size > maxBars) {
                amplitudes.removeAt(0)
            }
        }
    }

    // All spikes same color
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f

        if (amplitudes.isEmpty()) return@Canvas

        val totalGapWidth = 10f // Space from spike
        val barWidth = 7f // Width of spike


        // Draw from Right to Left
        val startIndex = (maxBars - amplitudes.size).coerceAtLeast(0)

        amplitudes.forEachIndexed { index, amplitude ->
            val visualIndex = index + startIndex
            val x = visualIndex * (barWidth + totalGapWidth) + (barWidth / 2)

            // Calculate the height of the bar based on the amplitude
            val barHeight = (amplitude * canvasHeight * 1.1f).coerceAtMost(canvasHeight * 0.98f)

            drawLine(
                color = barColor,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}