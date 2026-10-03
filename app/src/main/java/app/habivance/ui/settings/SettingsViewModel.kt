package app.habivance.ui.settings

import android.app.Application
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.habivance.HabitApp
import app.habivance.data.backup.BackupManager
import app.habivance.data.repository.HabitRepository
import app.habivance.data.settings.LockRepository
import app.habivance.data.lock.LockManager
import app.habivance.data.settings.SettingsRepository
import app.habivance.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupStatus(
    val message: String? = null,
    val isError: Boolean = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: SettingsRepository =
        (app as HabitApp).container.settingsRepository

    private val habitRepository: HabitRepository =
        (app as HabitApp).container.habitRepository

    private val lockRepository: LockRepository =
        (app as HabitApp).container.lockRepository

    private val backupManager = BackupManager(app)

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val notificationSoundUri: StateFlow<String?> = repository.notificationSoundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val hasSeenWelcome: StateFlow<Boolean?> = repository.hasSeenWelcome
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lockEnabled: StateFlow<Boolean> = lockRepository.lockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun canUseBiometric(): Boolean = LockManager.canAuthenticate(getApplication())
    fun getBiometricStatus(): String = LockManager.getBiometricStatus(getApplication())

    fun enableLock(pin: String) {
        viewModelScope.launch {
            lockRepository.setPin(pin)
            lockRepository.setLockEnabled(true)
            // Clear last background time so user isn't immediately locked
            lockRepository.setLastBackgroundedAt(0L)
        }
    }

    fun disableLock(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = lockRepository.verifyPin(pin)
            if (ok) {
                lockRepository.clearPin()
                lockRepository.setLockEnabled(false)
            }
            onResult(ok)
        }
    }

    private val _permissionRefreshTrigger = MutableStateFlow(0L)

    private val _batteryExempt = MutableStateFlow(false)
    val batteryExempt: StateFlow<Boolean> = _batteryExempt.asStateFlow()

    private val _exactAlarmGranted = MutableStateFlow(false)
    val exactAlarmGranted: StateFlow<Boolean> = _exactAlarmGranted.asStateFlow()

    init {
        // Now that _batteryExempt and _exactAlarmGranted are initialized,
        // compute permission status immediately to avoid a UI flash.
        refreshPermissionStatus()
    }

    fun refreshPermissionStatus() {
        val app = getApplication<Application>()

        // Battery exemption
        _batteryExempt.value = try {
            val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(app.packageName) == true
        } catch (_: Exception) { false }

        // Exact alarm (Android 12+)
        _exactAlarmGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                am?.canScheduleExactAlarms() == true
            } catch (_: Exception) { false }
        } else true
    }

    private val _backupStatus = MutableStateFlow(BackupStatus())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun markWelcomeSeen() {
        viewModelScope.launch {
            repository.setHasSeenWelcome(true)
        }
    }

    fun setNotificationSoundUri(uri: String?) {
        viewModelScope.launch {
            repository.setNotificationSoundUri(uri)
        }
    }

    fun openNotificationSettings() {
        val app = getApplication<Application>()
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            app.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${app.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                app.startActivity(fallback)
            } catch (_: Exception) { }
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val app = getApplication<Application>()
        val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun openBatteryOptimizationSettings(): Boolean {
        val app = getApplication<Application>()
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${app.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            app.startActivity(intent)
            return true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                app.startActivity(fallback)
                return true
            } catch (_: Exception) { }
        }
        return false
    }

    fun openExactAlarmSettings(): Boolean {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${app.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            app.startActivity(intent)
            return true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${app.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                app.startActivity(fallback)
                return true
            } catch (_: Exception) { }
        }
        return false
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                habitRepository.deleteAllData()
                _backupStatus.value = BackupStatus(
                    message = "All data deleted.",
                    isError = false
                )
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus(
                    message = "Delete failed: ${e.message ?: "unknown error"}",
                    isError = true
                )
            }
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = backupManager.exportToUri(uri)
                _backupStatus.value = BackupStatus(
                    message = "Exported $count habit${if (count == 1) "" else "s"} successfully.",
                    isError = false
                )
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus(
                    message = "Export failed: ${e.message ?: "unknown error"}",
                    isError = true
                )
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                val result = backupManager.importFromUri(uri)
                _backupStatus.value = BackupStatus(
                    message = "Imported ${result.habitsAdded} new, merged ${result.habitsMerged}, added ${result.completionsAdded} completions.",
                    isError = false
                )
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus(
                    message = "Import failed: ${e.message ?: "unknown error"}",
                    isError = true
                )
            }
        }
    }

    fun clearStatus() {
        _backupStatus.value = BackupStatus()
    }
}
