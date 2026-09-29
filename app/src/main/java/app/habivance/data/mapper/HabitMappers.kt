package app.habivance.data.mapper

import app.habivance.data.local.HabitCompletionEntity
import app.habivance.data.local.HabitEntity
import app.habivance.domain.model.Habit
import app.habivance.domain.model.HabitCompletion
import app.habivance.domain.model.HabitFrequency

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    emoji = emoji,
    colorHex = colorHex,
    frequency = try { HabitFrequency.valueOf(frequency) } catch (_: Exception) { HabitFrequency.DAILY },
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    createdAt = createdAt,
    isArchived = isArchived
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    emoji = emoji,
    colorHex = colorHex,
    frequency = frequency.name,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    createdAt = createdAt,
    isArchived = isArchived
)

fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
    habitId = habitId,
    dateEpochDay = dateEpochDay,
    completedAt = completedAt
)

fun HabitCompletion.toEntity(): HabitCompletionEntity = HabitCompletionEntity(
    habitId = habitId,
    dateEpochDay = dateEpochDay,
    completedAt = completedAt
)
