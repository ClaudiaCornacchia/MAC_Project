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
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BoxDetailScreen(
    popUpScreen: () -> Unit,
    viewModel: BoxDetailViewModel = hiltViewModel()
) {
    val box = viewModel.box

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Toolbar with Back button
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = popUpScreen) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(text = "Box Details", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Title:", style = MaterialTheme.typography.labelMedium)
                Text(box.title, style = MaterialTheme.typography.headlineMedium)

                Spacer(Modifier.height(8.dp))

                Text("Description:", style = MaterialTheme.typography.labelMedium)
                Text(box.description, style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Status Info
                Row {
                    if (box.isFragile) {
                        SuggestionChip(onClick = {}, label = { Text("Fragile ⚠️") })
                        Spacer(Modifier.width(8.dp))
                    }

                    val statusLabel = when(box.fillStatus) {
                        "GREEN" -> "Empty"
                        "YELLOW" -> "Half Full"
                        "RED" -> "Full"
                        else -> "Unknown"
                    }
                    SuggestionChip(onClick = {}, label = { Text("Status: $statusLabel") })
                }

                if (box.secretNote.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Secret Note:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(box.secretNote, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}