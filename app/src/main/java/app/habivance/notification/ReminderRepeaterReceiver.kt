package app.habivance.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderRepeaterReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
        val habitEmoji = intent.getStringExtra(EXTRA_HABIT_EMOJI) ?: "✅"
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val repeatIndex = intent.getIntExtra(EXTRA_REPEAT_INDEX, 0)

        if (habitId <= 0L) return
        if (repeatIndex >= REPEAT_DELAYS_MS.size) return  // done nagging

        // Re-fire the notification
        ReminderReceiver.postReminderNotification(
            context = context,
            habitId = habitId,
            habitName = habitName,
            habitEmoji = habitEmoji,
            hour = hour,
            minute = minute,
            isRepeat = true,
            isSnooze = false
        )

        // Schedule next repeat
        val nextIndex = repeatIndex + 1
        if (nextIndex < REPEAT_DELAYS_MS.size) {
            ReminderAlarmScheduler.scheduleRepeat(
                context = context,
                habitId = habitId,
                habitName = habitName,
                habitEmoji = habitEmoji,
                hour = hour,
                minute = minute,
                repeatIndex = nextIndex,
                delayMs = REPEAT_DELAYS_MS[nextIndex]
            )
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_EMOJI = "habit_emoji"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_REPEAT_INDEX = "repeat_index"

        // Delay BEFORE each repeat fires. Index 0 = first repeat (30s after original)
        val REPEAT_DELAYS_MS = longArrayOf(
            30_000L,      // 30 seconds
            60_000L,      // 60 seconds
            120_000L,     // 2 minutes
            300_000L      // 5 minutes
        )
    }
}
