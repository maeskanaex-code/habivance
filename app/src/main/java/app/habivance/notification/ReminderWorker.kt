package app.habivance.notification

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val habitName = inputData.getString(KEY_HABIT_NAME) ?: return Result.success()
        val habitEmoji = inputData.getString(KEY_HABIT_EMOJI) ?: "✅"
        val habitId = inputData.getLong(KEY_HABIT_ID, 0L)
        val hour = inputData.getInt(KEY_HOUR, -1)
        val minute = inputData.getInt(KEY_MINUTE, 0)

        ensureChannel(applicationContext)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("$habitEmoji  $habitName")
            .setContentText("Time to check in.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(habitId.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission not granted — silently no-op
        }

        // Reschedule for tomorrow at the same time
        if (hour >= 0) {
            ReminderScheduler.scheduleNext(
                context = applicationContext,
                habitId = habitId,
                habitName = habitName,
                habitEmoji = habitEmoji,
                hour = hour,
                minute = minute
            )
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_NAME = "habit_name"
        const val KEY_HABIT_EMOJI = "habit_emoji"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for your habits"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
