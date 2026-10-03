package app.habivance

import android.content.Context
import app.habivance.data.local.HabivanceDatabase
import app.habivance.data.repository.HabitRepository
import app.habivance.data.repository.HabitRepositoryImpl
import app.habivance.data.settings.LockRepository
import app.habivance.data.settings.SettingsRepository

class AppContainer(context: Context) {
    private val database: HabivanceDatabase = HabivanceDatabase.getInstance(context)
    val habitRepository: HabitRepository = HabitRepositoryImpl(database.habitDao())
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val lockRepository: LockRepository = LockRepository(context)
}
