package app.habivance.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.habivance.data.local.HabivanceDatabase
import app.habivance.data.repository.HabitRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class MarkDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return

        // Cancel the repeat chain and any pending snooze
        ReminderAlarmScheduler.cancelRepeat(context, habitId)
        SnoozeScheduler.cancelSnooze(context, habitId)

        // Dismiss the notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(habitId.toInt())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = HabivanceDatabase.getInstance(context).habitDao()
                val repository = HabitRepositoryImpl(dao)
                val today = LocalDate.now().toEpochDay()
                val isCompleted = repository.isCompleted(habitId, today)
                if (!isCompleted) {
                    repository.toggleCompletion(habitId, today, System.currentTimeMillis())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val ACTION_MARK_DONE = "app.habivance.ACTION_MARK_DONE"
    }
}
