package app.habivance

import android.app.Application
import app.habivance.notification.ReminderWorker

class HabitApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderWorker.ensureChannel(this)
    }
}
