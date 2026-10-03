package app.habivance

import android.app.Application
import app.habivance.notification.ReminderReceiver

class HabitApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderReceiver.ensureChannel(this)
    }
}
