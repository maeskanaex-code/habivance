package app.habivance.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.habivance.domain.model.Habit
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderAlarmScheduler {

    private const val REQUEST_CODE_OFFSET = 500000
    private const val REPEAT_REQUEST_OFFSET = 700000

    fun schedule(context: Context, habit: Habit) {
        val hour = habit.reminderHour ?: return cancel(context, habit.id)
        val minute = habit.reminderMinute ?: 0
        schedule(context, habit.id, habit.name, habit.emoji, hour, minute)
    }

    fun schedule(
        context: Context,
        habitId: Long,
        habitName: String,
        habitEmoji: String,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(ReminderReceiver.EXTRA_HABIT_EMOJI, habitEmoji)
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE, minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId + REQUEST_CODE_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(alarmManager, triggerMillis, pendingIntent)
    }

    fun scheduleRepeat(
        context: Context,
        habitId: Long,
        habitName: String,
        habitEmoji: String,
        hour: Int,
        minute: Int,
        repeatIndex: Int,
        delayMs: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = System.currentTimeMillis() + delayMs

        val intent = Intent(context, ReminderRepeaterReceiver::class.java).apply {
            putExtra(ReminderRepeaterReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderRepeaterReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(ReminderRepeaterReceiver.EXTRA_HABIT_EMOJI, habitEmoji)
            putExtra(ReminderRepeaterReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderRepeaterReceiver.EXTRA_MINUTE, minute)
            putExtra(ReminderRepeaterReceiver.EXTRA_REPEAT_INDEX, repeatIndex)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId + REPEAT_REQUEST_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(alarmManager, triggerMillis, pendingIntent)
    }

    fun cancelRepeat(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderRepeaterReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId + REPEAT_REQUEST_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
            )
        }
    }

    fun cancel(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId + REQUEST_CODE_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        cancelRepeat(context, habitId)
    }
}
