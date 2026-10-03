package app.habivance.data.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import app.habivance.data.settings.LockRepository
import kotlinx.coroutines.flow.first

object LockManager {

    const val GRACE_PERIOD_MS = 30_000L

    /**
     * Returns true if the device has any usable authentication method
     * (biometric strong/weak or device credential as fallback).
     */
    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Returns the display message for what's available on this device.
     */
    fun getBiometricStatus(context: Context): String {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric temporarily unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprint enrolled"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Unsupported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unknown"
            else -> "Unavailable"
        }
    }

    /**
     * Determine whether the app should currently be locked based on
     * lockEnabled + lastBackgroundedAt + grace period.
     */
    suspend fun shouldLock(repository: LockRepository, now: Long): Boolean {
        val enabled = repository.lockEnabled.first()
        if (!enabled) return false

        val lastBg = repository.lastBackgroundedAt.first() ?: return false
        val elapsed = now - lastBg

        return elapsed >= GRACE_PERIOD_MS
    }
}
