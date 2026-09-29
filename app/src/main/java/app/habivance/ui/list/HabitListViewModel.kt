package app.habivance.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.habivance.HabitApp
import app.habivance.data.repository.HabitRepository
import app.habivance.domain.model.Habit
import app.habivance.domain.model.HabitWithStatus
import app.habivance.domain.util.StreakCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class HabitListViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: HabitRepository =
        (app as HabitApp).container.habitRepository

    private val refreshTrigger = MutableStateFlow(0L)

    val habitsWithStatus: StateFlow<List<HabitWithStatus>> =
        combine(repository.observeActiveHabits(), refreshTrigger) { habits, _ ->
            val today = LocalDate.now()
            habits.map { habit ->
                val epochs = repository.getCompletionEpochDays(habit.id)
                HabitWithStatus(
                    habit = habit,
                    isCompletedToday = epochs.contains(today.toEpochDay()),
                    currentStreak = StreakCalculator.currentStreak(epochs, today),
                    bestStreak = StreakCalculator.bestStreak(epochs)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayCompleted: StateFlow<Int> = combine(habitsWithStatus, refreshTrigger) { list, _ ->
        list.count { it.isCompletedToday }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val todayTotal: StateFlow<Int> = habitsWithStatus
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val weekCompletions: StateFlow<List<Int>> =
        combine(repository.observeActiveHabits(), refreshTrigger) { habits, _ ->
            val today = LocalDate.now()
            // Monday = index 0, Sunday = index 6
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val weekEpochs = (0L until 7L).map { monday.plusDays(it).toEpochDay() }
            weekEpochs.map { epochDay ->
                habits.count { habit ->
                    repository.getCompletionEpochDays(habit.id).contains(epochDay)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(7) { 0 })

    fun toggleToday(habit: Habit) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            repository.toggleCompletion(habit.id, today, System.currentTimeMillis())
            refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }
}
