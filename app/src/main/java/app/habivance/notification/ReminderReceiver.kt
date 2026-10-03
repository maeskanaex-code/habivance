package app.habivance.notification

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.habivance.MainActivity
import app.habivance.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
        val habitEmoji = intent.getStringExtra(EXTRA_HABIT_EMOJI) ?: "✅"
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        if (habitId <= 0L) return

        ensureChannel(context)
        postReminderNotification(context, habitId, habitName, habitEmoji, hour, minute, isRepeat = false, isSnooze = isSnooze)

        // Only schedule the repeat chain for the ORIGINAL reminder, not snoozes
        if (!isSnooze && ReminderRepeaterReceiver.REPEAT_DELAYS_MS.isNotEmpty()) {
            ReminderAlarmScheduler.scheduleRepeat(
                context = context,
                habitId = habitId,
                habitName = habitName,
                habitEmoji = habitEmoji,
                hour = hour,
                minute = minute,
                repeatIndex = 0,
                delayMs = ReminderRepeaterReceiver.REPEAT_DELAYS_MS[0]
            )
        }

        // Reschedule tomorrow's regular reminder — but ONLY if this wasn't a snooze.
        // Snoozes should not touch the daily schedule.
        if (!isSnooze && hour >= 0) {
            ReminderAlarmScheduler.schedule(
                context = context,
                habitId = habitId,
                habitName = habitName,
                habitEmoji = habitEmoji,
                hour = hour,
                minute = minute
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders_v4"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_EMOJI = "habit_emoji"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_IS_SNOOZE = "is_snooze"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for your habits"
                enableVibration(true)
                setSound(soundUri, audioAttrs)
            }
            manager.createNotificationChannel(channel)
        }

        fun postReminderNotification(
            context: Context,
            habitId: Long,
            habitName: String,
            habitEmoji: String,
            hour: Int,
            minute: Int,
            isRepeat: Boolean,
            isSnooze: Boolean
        ) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("habit_id", habitId)
            }
            val openPending = PendingIntent.getActivity(
                context,
                habitId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val markDoneIntent = Intent(context, MarkDoneReceiver::class.java).apply {
                action = MarkDoneReceiver.ACTION_MARK_DONE
                putExtra(MarkDoneReceiver.EXTRA_HABIT_ID, habitId)
            }
            val markDonePending = PendingIntent.getBroadcast(
                context,
                (habitId + 100000).toInt(),
                markDoneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Snooze +1h action
            val snooze1hIntent = Intent(context, SnoozeActionReceiver::class.java).apply {
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_ID, habitId)
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_NAME, habitName)
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_EMOJI, habitEmoji)
                putExtra(SnoozeActionReceiver.EXTRA_HOUR, hour)
                putExtra(SnoozeActionReceiver.EXTRA_MINUTE, minute)
                putExtra(SnoozeActionReceiver.EXTRA_SNOOZE_ACTION, SnoozeActionReceiver.ACTION_ONE_HOUR)
            }
            val snooze1hPending = PendingIntent.getBroadcast(
                context,
                (habitId + 200000).toInt(),
                snooze1hIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Snooze +2h action (shown in expanded view)
            val snooze2hIntent = Intent(context, SnoozeActionReceiver::class.java).apply {
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_ID, habitId)
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_NAME, habitName)
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_EMOJI, habitEmoji)
                putExtra(SnoozeActionReceiver.EXTRA_HOUR, hour)
                putExtra(SnoozeActionReceiver.EXTRA_MINUTE, minute)
                putExtra(SnoozeActionReceiver.EXTRA_SNOOZE_ACTION, SnoozeActionReceiver.ACTION_TWO_HOURS)
            }
            val snooze2hPending = PendingIntent.getBroadcast(
                context,
                (habitId + 300000).toInt(),
                snooze2hIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Snooze Tomorrow action
            val snoozeTomorrowIntent = Intent(context, SnoozeActionReceiver::class.java).apply {
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_ID, habitId)
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_NAME, habitName)
                putExtra(SnoozeActionReceiver.EXTRA_HABIT_EMOJI, habitEmoji)
                putExtra(SnoozeActionReceiver.EXTRA_HOUR, hour)
                putExtra(SnoozeActionReceiver.EXTRA_MINUTE, minute)
                putExtra(SnoozeActionReceiver.EXTRA_SNOOZE_ACTION, SnoozeActionReceiver.ACTION_TOMORROW)
            }
            val snoozeTomorrowPending = PendingIntent.getBroadcast(
                context,
                (habitId + 400000).toInt(),
                snoozeTomorrowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val settingsRepository = SettingsRepository(context)
            val soundUriString = runBlocking {
                try { settingsRepository.notificationSoundUri.first() } catch (_: Exception) { null }
            }

            val vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 700)

            val contentText = when {
                isSnooze -> "Snoozed reminder."
                isRepeat -> "Still time to check in."
                else -> "Time to check in."
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dialog_info)
                .setContentTitle("$habitEmoji  $habitName")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVibrate(vibrationPattern)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(openPending)
                // Primary actions (collapsed view)
                .addAction(0, "Mark done", markDonePending)
                .addAction(0, "+1h", snooze1hPending)
                // Extended actions (expanded view)
                .addAction(0, "+2h", snooze2hPending)
                .addAction(0, "Tomorrow", snoozeTomorrowPending)

            if (!soundUriString.isNullOrBlank()) {
                try {
                    builder.setSound(Uri.parse(soundUriString))
                } catch (_: Exception) { }
            }

            try {
                NotificationManagerCompat.from(context)
                    .notify(habitId.toInt(), builder.build())
            } catch (_: SecurityException) { }
        }
    }
}
