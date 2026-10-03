package app.habivance.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

class SnoozeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
        val habitEmoji = intent.getStringExtra(EXTRA_HABIT_EMOJI) ?: "✅"
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val action = intent.getStringExtra(EXTRA_SNOOZE_ACTION) ?: return

        if (habitId <= 0L) return

        // Dismiss current notification + cancel repeat chain
        ReminderAlarmScheduler.cancelRepeat(context, habitId)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(habitId.toInt())

        val delayMs: Long = when (action) {
            ACTION_ONE_HOUR -> 60L * 60L * 1000L
            ACTION_TWO_HOURS -> 2L * 60L * 60L * 1000L
            ACTION_TOMORROW -> {
                // Compute delay until tomorrow at the same configured time
                val now = LocalDateTime.now()
                var target = now.withHour(hour.coerceAtLeast(0))
                    .withMinute(minute).withSecond(0).withNano(0)
                if (!target.isAfter(now)) target = target.plusDays(1)
                val targetMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                targetMillis - System.currentTimeMillis()
            }
            else -> return
        }

        if (delayMs <= 0) return

        SnoozeScheduler.scheduleSnooze(
            context = context,
            habitId = habitId,
            habitName = habitName,
            habitEmoji = habitEmoji,
            hour = hour,
            minute = minute,
            delayMillis = delayMs
        )
    }

    companion object {
        const val ACTION_ONE_HOUR = "snooze_1h"
        const val ACTION_TWO_HOURS = "snooze_2h"
        const val ACTION_TOMORROW = "snooze_tomorrow"

        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_EMOJI = "habit_emoji"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_SNOOZE_ACTION = "snooze_action"
    }
}
