package com.example.sensy.ui.main

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.res.painterResource
import com.example.sensy.data.ActiveStatus
import com.example.sensy.data.PresetStatus

class MainScreenViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainScreenViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: MainScreenViewModel = viewModel(
        factory = MainScreenViewModelFactory(application)
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var customStatusText by remember { mutableStateOf("") }
    var customDurationText by remember { mutableStateOf("15") }
    var showPresetsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.SEND_SMS,
                "android.permission.POST_NOTIFICATIONS"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sensy: Missed Call Manager",
            style = MaterialTheme.typography.titleLarge
        )

        Divider()

        if (state.activeStatus != null) {
            ActiveStatusCard(
                status = state.activeStatus!!,
                onClearClick = { viewModel.clearStatus() }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set Your Status", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showPresetsDialog = true }) {
                    Icon(painterResource(android.R.drawable.ic_menu_edit), contentDescription = "Edit Presets")
                }
            }

            // Presets
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.presets.forEach { preset ->
                    Button(onClick = { customStatusText = preset.content }) { Text(preset.heading) }
                }
            }

            // Recent Statuses
            if (state.recentStatuses.isNotEmpty()) {
                Text("Recent Custom Statuses", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recentStatuses.size) { index ->
                        val recent = state.recentStatuses[index]
                        val durationStr = if (recent.durationMinutes == -1) "No Limit" else "${recent.durationMinutes}m"
                        
                        InputChip(
                            selected = false,
                            onClick = { 
                                customStatusText = recent.text
                                customDurationText = recent.durationMinutes.toString()
                            },
                            label = { Text("${recent.text} ($durationStr)") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.removeRecentStatus(recent) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
            }

            // Custom Text
            OutlinedTextField(
                value = customStatusText,
                onValueChange = { customStatusText = it },
                label = { Text("Custom Status") },
                modifier = Modifier.fillMaxWidth()
            )

            // Duration Selection
            OutlinedTextField(
                value = customDurationText,
                onValueChange = { customDurationText = it.filter { char -> char.isDigit() } },
                label = { Text("Duration in Minutes (leave blank for no time limit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = {
                    val duration = customDurationText.toIntOrNull() ?: -1
                    if (customStatusText.isNotBlank()) {
                        viewModel.saveStatus(customStatusText, duration)
                        customStatusText = ""
                        customDurationText = "" // reset
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = customStatusText.isNotBlank()
            ) {
                Text("Set Status")
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }) {
                Text("Disable Battery Optimization (Recommended)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showPresetsDialog) {
        PresetsDialog(
            presets = state.presets,
            onDismiss = { showPresetsDialog = false },
            onAdd = { heading, content -> viewModel.addPreset(PresetStatus(heading, content)) },
            onRemove = { viewModel.removePreset(it) }
        )
    }
}

@Composable
fun PresetsDialog(
    presets: List<PresetStatus>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (PresetStatus) -> Unit
) {
    var newHeading by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Presets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newHeading,
                    onValueChange = { newHeading = it },
                    label = { Text("Button Name (e.g., Gym)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text("Status (e.g., working out)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = {
                            if (newHeading.isNotBlank() && newContent.isNotBlank()) {
                                onAdd(newHeading.trim(), newContent.trim())
                                newHeading = ""
                                newContent = ""
                            }
                        }) {
                            Text("Add")
                        }
                    }
                )
                
                Divider()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    presets.forEach { preset ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(preset.heading, style = MaterialTheme.typography.titleSmall)
                                Text(preset.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onRemove(preset) }) {
                                Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ActiveStatusCard(status: ActiveStatus, onClearClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Active Status", style = MaterialTheme.typography.titleMedium)
            Text(status.statusText, style = MaterialTheme.typography.bodyLarge)
            val durationText = if (status.durationMinutes == -1) "No Limit" else "${status.durationMinutes} mins"
            Text("Duration: $durationText", style = MaterialTheme.typography.bodyMedium)

            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Auto-Reply Preview:", style = MaterialTheme.typography.labelMedium)
            val previewMessage = com.example.sensy.MessageBuilder.buildSmsMessage(
                status.statusText,
                status.startTimeMillis,
                status.durationMinutes
            )
            Text(
                text = previewMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Button(
                onClick = onClearClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Clear Status")
            }
        }
    }
}
