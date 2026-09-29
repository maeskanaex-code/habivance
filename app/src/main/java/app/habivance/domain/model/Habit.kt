package app.habivance.domain.model

enum class HabitFrequency { DAILY, WEEKLY }

data class Habit(
    val id: Long = 0L,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val frequency: HabitFrequency,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val createdAt: Long,
    val isArchived: Boolean = false
)

data class HabitCompletion(
    val habitId: Long,
    val dateEpochDay: Long,
    val completedAt: Long
)
