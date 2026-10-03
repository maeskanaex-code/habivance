package app.habivance.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object SnoozeScheduler {

    private const val SNOOZE_REQUEST_OFFSET = 900000

    /**
     * Schedule a one-time snooze. This does NOT affect the daily reminder.
     * The notification will re-fire at [delayMillis] from now.
     * Also optionally schedules a follow-up snooze at a different delay 
     * (used for "Tomorrow" = fire at habit's configured time tomorrow).
     */
    fun scheduleSnooze(
        context: Context,
        habitId: Long,
        habitName: String,
        habitEmoji: String,
        hour: Int,
        minute: Int,
        delayMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = System.currentTimeMillis() + delayMillis

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(ReminderReceiver.EXTRA_HABIT_EMOJI, habitEmoji)
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE, minute)
            putExtra(ReminderReceiver.EXTRA_IS_SNOOZE, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId + SNOOZE_REQUEST_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelSnooze(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId + SNOOZE_REQUEST_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
