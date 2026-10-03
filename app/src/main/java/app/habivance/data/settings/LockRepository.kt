package app.habivance.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.lockDataStore: DataStore<Preferences> by preferencesDataStore(name = "lock")

class LockRepository(private val context: Context) {

    private val lockEnabledKey = booleanPreferencesKey("lock_enabled")
    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val lastBackgroundedKey = longPreferencesKey("last_backgrounded_at")

    val lockEnabled: Flow<Boolean> = context.lockDataStore.data.map { prefs ->
        prefs[lockEnabledKey] ?: false
    }

    val hasPinSet: Flow<Boolean> = context.lockDataStore.data.map { prefs ->
        !prefs[pinHashKey].isNullOrEmpty()
    }

    val lastBackgroundedAt: Flow<Long?> = context.lockDataStore.data.map { prefs ->
        prefs[lastBackgroundedKey]
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.lockDataStore.edit { prefs ->
            prefs[lockEnabledKey] = enabled
        }
    }

    suspend fun setPin(pin: String) {
        context.lockDataStore.edit { prefs ->
            prefs[pinHashKey] = hashPin(pin)
        }
    }

    suspend fun clearPin() {
        context.lockDataStore.edit { prefs ->
            prefs.remove(pinHashKey)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        var stored: String? = null
        context.lockDataStore.edit { prefs ->
            stored = prefs[pinHashKey]
        }
        return stored != null && stored == hashPin(pin)
    }

    suspend fun setLastBackgroundedAt(timestamp: Long) {
        context.lockDataStore.edit { prefs ->
            prefs[lastBackgroundedKey] = timestamp
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(("habivance_pin_salt::$pin").toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
