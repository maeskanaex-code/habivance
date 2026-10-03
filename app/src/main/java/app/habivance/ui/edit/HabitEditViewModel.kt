package app.habivance.ui.edit

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.habivance.HabitApp
import app.habivance.data.repository.HabitRepository
import app.habivance.domain.model.Habit
import app.habivance.domain.model.HabitFrequency
import app.habivance.domain.model.HabitPriority
import app.habivance.notification.ReminderAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HabitEditState(
    val id: Long = 0L,
    val name: String = "",
    val emoji: String = "✅",
    val colorHex: String = "#3B82F6",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val priority: HabitPriority = HabitPriority.NORMAL
)

class HabitEditViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: HabitRepository =
        (app as HabitApp).container.habitRepository

    private val _state = MutableStateFlow(HabitEditState())
    val state: StateFlow<HabitEditState> = _state.asStateFlow()

    fun loadHabit(habitId: Long) {
        viewModelScope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            _state.value = HabitEditState(
                id = habit.id,
                name = habit.name,
                emoji = habit.emoji,
                colorHex = habit.colorHex,
                frequency = habit.frequency,
                reminderHour = habit.reminderHour,
                reminderMinute = habit.reminderMinute,
                isEditing = true,
                priority = habit.priority
            )
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onEmojiChange(value: String) { _state.value = _state.value.copy(emoji = value) }
    fun onColorChange(value: String) { _state.value = _state.value.copy(colorHex = value) }
    fun onFrequencyChange(value: HabitFrequency) { _state.value = _state.value.copy(frequency = value) }
    fun onPriorityChange(value: HabitPriority) { _state.value = _state.value.copy(priority = value) }
    fun onReminderChange(hour: Int?, minute: Int?) {
        _state.value = _state.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (current.name.isBlank()) return
        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val existing = if (current.id != 0L) repository.getHabit(current.id) else null
            val habit = Habit(
                id = current.id,
                name = current.name.trim(),
                emoji = current.emoji,
                colorHex = current.colorHex,
                frequency = current.frequency,
                reminderHour = current.reminderHour,
                reminderMinute = current.reminderMinute,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                isArchived = false,
                sortOrder = existing?.sortOrder ?: 0L,
                priority = current.priority
            )
            val id = repository.upsertHabit(habit)
            val saved = habit.copy(id = if (id == 0L) habit.id else id)
            if (saved.reminderHour != null) {
                ReminderAlarmScheduler.schedule(getApplication(), saved)
            } else {
                ReminderAlarmScheduler.cancel(getApplication(), saved.id)
            }
            _state.value = _state.value.copy(isSaving = false)
            onSaved()
        }
    }

    fun deleteHabit(onDeleted: () -> Unit) {
        val current = _state.value
        if (current.id == 0L) return
        viewModelScope.launch {
            val habit = repository.getHabit(current.id) ?: return@launch
            ReminderAlarmScheduler.cancel(getApplication(), current.id)
            repository.deleteHabit(habit)
            onDeleted()
        }
    }

    fun onReminderSetRequested(): Boolean {
        val app = getApplication<Application>()
        val hasBatteryExemption = try {
            val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(app.packageName) == true
        } catch (_: Exception) { false }

        val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                am?.canScheduleExactAlarms() == true
            } catch (_: Exception) { false }
        } else true

        val missing = !hasBatteryExemption || !hasExactAlarm
        if (missing) {
            _state.value = _state.value.copy(showPermissionDialog = true)
        }
        return !missing
    }

    fun dismissPermissionDialog() {
        _state.value = _state.value.copy(showPermissionDialog = false)
    }
}
