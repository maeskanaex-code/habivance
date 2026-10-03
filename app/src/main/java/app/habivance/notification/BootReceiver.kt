package app.habivance.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.habivance.data.local.HabivanceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = HabivanceDatabase.getInstance(context).habitDao()
                val entities = dao.observeActiveHabits().first()
                entities.forEach { entity ->
                    if (entity.reminderHour != null) {
                        ReminderAlarmScheduler.schedule(
                            context = context,
                            habitId = entity.id,
                            habitName = entity.name,
                            habitEmoji = entity.emoji,
                            hour = entity.reminderHour,
                            minute = entity.reminderMinute ?: 0
                        )
                    }
                }
            } catch (_: Exception) {
                // Silent fail
            } finally {
                pendingResult.finish()
            }
        }
    }
}
