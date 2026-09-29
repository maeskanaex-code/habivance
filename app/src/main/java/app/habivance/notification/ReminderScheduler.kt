package app.habivance.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.habivance.domain.model.Habit
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private fun workName(habitId: Long) = "habit_reminder_$habitId"

    fun schedule(context: Context, habit: Habit) {
        val hour = habit.reminderHour ?: return cancel(context, habit.id)
        val minute = habit.reminderMinute ?: 0
        scheduleNext(context, habit.id, habit.name, habit.emoji, hour, minute)
    }

    /**
     * Schedules the next firing for a habit reminder.
     * If the target time is in the past today, schedules for tomorrow.
     */
    fun scheduleNext(
        context: Context,
        habitId: Long,
        habitName: String,
        habitEmoji: String,
        hour: Int,
        minute: Int
    ) {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes().coerceAtLeast(1)

        val data = Data.Builder()
            .putLong(ReminderWorker.KEY_HABIT_ID, habitId)
            .putString(ReminderWorker.KEY_HABIT_NAME, habitName)
            .putString(ReminderWorker.KEY_HABIT_EMOJI, habitEmoji)
            .putInt(ReminderWorker.KEY_HOUR, hour)
            .putInt(ReminderWorker.KEY_MINUTE, minute)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .addTag(workName(habitId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(habitId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, habitId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(habitId))
    }
}
