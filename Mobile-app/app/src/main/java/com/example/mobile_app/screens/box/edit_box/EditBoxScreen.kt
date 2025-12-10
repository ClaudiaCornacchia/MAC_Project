package com.example.mobile_app.screens.box.edit_box


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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EditBoxScreen(
    popUpScreen: () -> Unit,
    viewModel: EditBoxViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New Box", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Title
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.onTitleChange(it) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        // Description
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.onDescriptionChange(it) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        // Secret Note (Optional)
        OutlinedTextField(
            value = uiState.secretNote,
            onValueChange = { viewModel.onSecretNoteChange(it) },
            label = { Text("Secret Note (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Fragile Switch
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Is Fragile?")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = uiState.isFragile,
                onCheckedChange = { viewModel.onFragileChange(it) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Fill Status Selection
        Text("Fill Status", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(
                selected = uiState.fillStatus == "GREEN",
                onClick = { viewModel.onFillStatusChange("GREEN") },
                label = { Text("Empty") }
            )
            FilterChip(
                selected = uiState.fillStatus == "YELLOW",
                onClick = { viewModel.onFillStatusChange("YELLOW") },
                label = { Text("Half") }
            )
            FilterChip(
                selected = uiState.fillStatus == "RED",
                onClick = { viewModel.onFillStatusChange("RED") },
                label = { Text("Full") }
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { viewModel.onDoneClick(popUpScreen) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.title.isNotBlank() && uiState.description.isNotBlank()
        ) {
            Text("Save Box")
        }
    }
}