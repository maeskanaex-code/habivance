package app.habivance.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.habivance.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val selectedMode by viewModel.themeMode.collectAsState()
    val status by viewModel.backupStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showDisableLockDialog by remember { mutableStateOf(false) }
    var disableError by remember { mutableStateOf<String?>(null) }

    // Refresh permission status whenever the screen resumes.
    // This handles the case where the user goes to Android settings, grants 
    // a permission, and returns to this screen.
    val settingsLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(settingsLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatus()
            }
        }
        settingsLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { settingsLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> if (uri != null) viewModel.exportData(uri) }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) viewModel.importData(uri) }
    )

    LaunchedEffect(status.message) {
        status.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
            )

            ThemeMode.entries.forEach { mode ->
                ThemeModeRow(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { viewModel.setThemeMode(mode) }
                )
            }

            Spacer(Modifier.size(20.dp))

            Text(
                text = "Data",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
            )

            ActionRow(
                title = "Export data",
                subtitle = "Save all habits + history to a JSON file",
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    exportLauncher.launch("habivance_$stamp.json")
                }
            )

            ActionRow(
                title = "Import data",
                subtitle = "Restore from a JSON backup",
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )

            ActionRow(
                title = "Delete all data",
                subtitle = "Permanently remove all habits and history",
                onClick = { showDeleteAllDialog = true }
            )

            Spacer(Modifier.size(20.dp))

            Text(
                text = "Reminders",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
            )

            val batteryExempt by viewModel.batteryExempt.collectAsState()
            val exactAlarmGranted by viewModel.exactAlarmGranted.collectAsState()
            val allGranted = batteryExempt && exactAlarmGranted

            if (!batteryExempt) {
                WarningActionRow(
                    title = "Allow background reminders",
                    subtitle = "Tap to grant — required for reminders when app is closed",
                    onClick = {
                        viewModel.openBatteryOptimizationSettings()
                    }
                )
            }

            if (!exactAlarmGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WarningActionRow(
                    title = "Allow exact alarms",
                    subtitle = "Tap to grant — required on Android 12+ for exact timing",
                    onClick = {
                        viewModel.openExactAlarmSettings()
                    }
                )
            }

            if (allGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(
                                text = "Reminders are set up",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.size(2.dp))
                            Text(
                                text = "You will receive reminders at the exact time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(20.dp))

            Text(
                text = "Privacy & Security",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
            )

            val lockEnabledState by viewModel.lockEnabled.collectAsState()
            ActionRow(
                title = "App lock",
                subtitle = if (lockEnabledState) "On - PIN required to open"
                           else "Off - tap to enable",
                onClick = {
                    if (lockEnabledState) {
                        showDisableLockDialog = true
                    } else {
                        showPinSetup = true
                    }
                }
            )

            Spacer(Modifier.size(20.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Habivance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete all data?") },
            text = { Text("This will permanently remove all habits, completions, and history. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllData()
                    showDeleteAllDialog = false
                }) {
                    Text("Delete everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onPinSet = { pin ->
                viewModel.enableLock(pin)
                showPinSetup = false
            }
        )
    }

    if (showDisableLockDialog) {
        PinEntryDialog(
            title = "Disable app lock",
            message = disableError ?: "Enter your current PIN to disable app lock",
            onDismiss = {
                showDisableLockDialog = false
                disableError = null
            },
            onPinEntered = { pin ->
                viewModel.disableLock(pin) { ok ->
                    if (ok) {
                        showDisableLockDialog = false
                        disableError = null
                    } else {
                        disableError = "Incorrect PIN. Try again."
                    }
                }
            }
        )
    }
}

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WarningActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
