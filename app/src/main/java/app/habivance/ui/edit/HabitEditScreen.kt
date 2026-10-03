package app.habivance.ui.edit

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.habivance.domain.model.HabitFrequency
import app.habivance.domain.model.HabitPriority
import app.habivance.ui.components.TimePickerDialog

private val EMOJI_CHOICES = listOf(
    "✅", "💧", "📚", "🧠", "🏃", "🧘", "🍎", "💤", "💪", "✍️",
    "🎯", "🌱", "☀️", "🌙", "🎵", "🧹", "💰", "📵"
)

private val COLOR_CHOICES = listOf(
    "#3B82F6", "#8B5CF6", "#10B981", "#F59E0B",
    "#EF4444", "#EC4899", "#06B6D4", "#84CC16"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditScreen(
    habitId: Long = 0L,
    onBack: () -> Unit = {},
    viewModel: HabitEditViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* result ignored */ }
    )

    LaunchedEffect(habitId) {
        if (habitId != 0L) viewModel.loadHabit(habitId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit habit" else "New habit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete habit"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Text("Emoji", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EMOJI_CHOICES) { emoji ->
                    EmojiChip(
                        emoji = emoji,
                        selected = emoji == state.emoji,
                        onClick = { viewModel.onEmojiChange(emoji) }
                    )
                }
            }

            Text("Color", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(COLOR_CHOICES) { hex ->
                    ColorChip(
                        hex = hex,
                        selected = hex == state.colorHex,
                        onClick = { viewModel.onColorChange(hex) }
                    )
                }
            }

            Text("Frequency", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = freq == state.frequency,
                        onClick = { viewModel.onFrequencyChange(freq) },
                        label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitPriority.entries.forEach { p ->
                    FilterChip(
                        selected = p == state.priority,
                        onClick = { viewModel.onPriorityChange(p) },
                        label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // --- Reminder row ---
            Text("Reminder", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.reminderHour?.let { h ->
                        "%02d:%02d".format(h, state.reminderMinute ?: 0)
                    } ?: "No reminder",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.reminderHour == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Row {
                    if (state.reminderHour != null) {
                        TextButton(onClick = {
                            viewModel.onReminderChange(null, null)
                        }) {
                            Text("Clear")
                        }
                    }
                    TextButton(onClick = {
                        if (viewModel.onReminderSetRequested()) {
                            showTimePicker = true
                        }
                    }) {
                        Text(if (state.reminderHour == null) "Set time" else "Change")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(onSaved = onBack) },
                enabled = state.name.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isEditing) "Save changes" else "Create habit")
            }
        }
    }

    if (state.showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissPermissionDialog()
                showTimePicker = true
            },
            title = { Text("Enable reliable reminders") },
            text = { Text(
                "Habivance needs two permissions so reminders fire exactly on time, " +
                "even when the phone is idle or the app is closed.\n\n" +
                "1. Allow background activity (battery)\n" +
                "2. Allow exact alarms\n\n" +
                "You can skip this, but reminders may be late or miss."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissPermissionDialog()
                    val ctx = viewModel.getApplication<Application>()

                    try {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        ).apply {
                            data = Uri.parse("package:${ctx.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        ctx.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val fallback = Intent(
                                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                            ).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            ctx.startActivity(fallback)
                        } catch (_: Exception) { }
                    }

                    showTimePicker = true
                }) { Text("Grant battery") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissPermissionDialog()
                    val ctx = viewModel.getApplication<Application>()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            ).apply {
                                data = Uri.parse("package:${ctx.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            ctx.startActivity(intent)
                        } catch (_: Exception) { }
                    }

                    showTimePicker = true
                }) { Text("Grant exact alarms") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete habit?") },
            text = { Text("\"${state.name}\" and all its history will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteHabit {
                        onBack()
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTimePicker) {
        val initialHour = state.reminderHour ?: 9
        val initialMinute = state.reminderMinute ?: 0

        TimePickerDialog(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                viewModel.onReminderChange(h, m)
                showTimePicker = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }
}

@Composable
private fun EmojiChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ColorChip(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(hex))
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
    }
}
