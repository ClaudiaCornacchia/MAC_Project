package com.example.mobile_app.screens.box.boxes

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

@Composable
fun BoxesScreen(
    openScreen: (String) -> Unit,
    viewModel: BoxesViewModel = hiltViewModel()
) {
    val boxes by viewModel.boxes.collectAsState(emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openScreen(EDIT_BOX_SCREEN) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, "Add Box")
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
    Card(
        modifier = Modifier.padding(8.dp, 4.dp).fillMaxWidth(),
        onClick = onBoxClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = box.title, style = MaterialTheme.typography.titleMedium)
                Text(text = box.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            // Visual indicator for Fragile
            if (box.isFragile) {
                Icon(Icons.Filled.Warning, "Fragile", tint = Color.Red)
            }
        }
    }
}