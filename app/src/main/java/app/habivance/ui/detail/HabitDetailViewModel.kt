package app.habivance.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.habivance.HabitApp
import app.habivance.data.repository.HabitRepository
import app.habivance.domain.model.Habit
import app.habivance.domain.util.StreakCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitDetailState(
    val habit: Habit? = null,
    val completionEpochDays: Set<Long> = emptySet(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val completionRateLast30: Float = 0f,
    val isCompletedToday: Boolean = false,
    val isLoading: Boolean = true
)

class HabitDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: HabitRepository =
        (app as HabitApp).container.habitRepository

    private val _state = MutableStateFlow(HabitDetailState())
    val state: StateFlow<HabitDetailState> = _state.asStateFlow()

    fun load(habitId: Long) {
        viewModelScope.launch {
            val habit = repository.getHabit(habitId) ?: run {
                _state.value = HabitDetailState(isLoading = false)
                return@launch
            }
            val epochs = repository.getCompletionEpochDays(habitId)
            val today = LocalDate.now()
            val last30 = (0L until 30L).map { today.minusDays(it).toEpochDay() }
            val completed30 = last30.count { epochs.contains(it) }
            _state.value = HabitDetailState(
                habit = habit,
                completionEpochDays = epochs,
                currentStreak = StreakCalculator.currentStreak(epochs, today),
                bestStreak = StreakCalculator.bestStreak(epochs),
                totalCompletions = epochs.size,
                completionRateLast30 = completed30 / 30f,
                isCompletedToday = epochs.contains(today.toEpochDay()),
                isLoading = false
            )
        }
    }

    fun toggleToday() {
        val habit = _state.value.habit ?: return
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            repository.toggleCompletion(habit.id, today, System.currentTimeMillis())
            load(habit.id)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val habit = _state.value.habit ?: return
        viewModelScope.launch {
            repository.deleteHabit(habit)
            onDeleted()
        }
    }
}
