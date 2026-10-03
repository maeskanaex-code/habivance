package app.habivance.ui.lock

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.habivance.HabitApp
import app.habivance.data.settings.LockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LockUiState(
    val pinInput: String = "",
    val errorMessage: String? = null,
    val isVerifying: Boolean = false
)

class LockViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: LockRepository =
        (app as HabitApp).container.lockRepository

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    fun onDigit(digit: Int) {
        val current = _state.value
        if (current.pinInput.length >= 6) return
        val newInput = current.pinInput + digit.toString()
        _state.value = current.copy(pinInput = newInput, errorMessage = null)

        // Auto-verify when 4 digits are entered
        if (newInput.length == 4) {
            verifyPin(newInput)
        }
    }

    fun onBackspace() {
        val current = _state.value
        if (current.pinInput.isEmpty()) return
        _state.value = current.copy(pinInput = current.pinInput.dropLast(1), errorMessage = null)
    }

    fun clearInput() {
        _state.value = _state.value.copy(pinInput = "", errorMessage = null)
    }

    private fun verifyPin(pin: String) {
        _state.value = _state.value.copy(isVerifying = true)
        viewModelScope.launch {
            val ok = repository.verifyPin(pin)
            if (ok) {
                _state.value = _state.value.copy(
                    isVerifying = false,
                    errorMessage = null
                )
                onUnlocked?.invoke()
            } else {
                _state.value = _state.value.copy(
                    pinInput = "",
                    isVerifying = false,
                    errorMessage = "Incorrect PIN. Try again."
                )
            }
        }
    }

    var onUnlocked: (() -> Unit)? = null

    /**
     * Called after biometric succeeds. Just triggers the unlock callback.
     */
    fun onBiometricSuccess() {
        onUnlocked?.invoke()
    }

    fun showBiometricPrompt(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricSuccess()
                }

                override fun onAuthenticationFailed() {
                    _state.value = _state.value.copy(
                        errorMessage = "Fingerprint not recognized. Try again or use PIN."
                    )
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled or biometric unavailable — do nothing, PIN remains
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Habivance")
            .setSubtitle("Use your fingerprint")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                    or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
