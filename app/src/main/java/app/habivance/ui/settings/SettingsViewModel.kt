package app.habivance.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.habivance.HabitApp
import app.habivance.data.backup.BackupManager
import app.habivance.data.repository.HabitRepository
import app.habivance.data.settings.SettingsRepository
import app.habivance.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val backupManager = BackupManager(app)

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val _backupStatus = MutableStateFlow(BackupStatus())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
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
