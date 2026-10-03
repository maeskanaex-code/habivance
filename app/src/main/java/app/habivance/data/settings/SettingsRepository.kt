package app.habivance.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.habivance.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val soundKey = stringPreferencesKey("notification_sound_uri")
    private val hasSeenWelcomeKey = booleanPreferencesKey("has_seen_welcome")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[themeModeKey]
        try {
            if (raw != null) ThemeMode.valueOf(raw) else ThemeMode.SYSTEM
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val notificationSoundUri: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[soundKey]
    }

    val hasSeenWelcome: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[hasSeenWelcomeKey] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setNotificationSoundUri(uri: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri == null) prefs.remove(soundKey) else prefs[soundKey] = uri
        }
    }

    suspend fun setHasSeenWelcome(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[hasSeenWelcomeKey] = value
        }
    }
}
