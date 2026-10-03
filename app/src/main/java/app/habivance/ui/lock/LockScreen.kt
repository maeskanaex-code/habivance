package app.habivance.ui.lock

import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Wire the unlock callback
    LaunchedEffect(Unit) {
        viewModel.onUnlocked = onUnlocked
    }

    // Auto-prompt biometric on first display, if available
    LaunchedEffect(Unit) {
        val manager = BiometricManager.from(context)
        val canUseBiometric = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

        if (canUseBiometric && context is FragmentActivity) {
            viewModel.showBiometricPrompt(context)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Unlock Habivance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = state.errorMessage ?: "Enter your PIN or use fingerprint",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.errorMessage != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(24.dp)
            ) {
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < state.pinInput.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Numeric keypad
            PinKeypad(
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
                onBiometric = {
                    if (context is FragmentActivity) {
                        viewModel.showBiometricPrompt(context)
                    }
                }
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: () -> Unit
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(-1, 0, -2) // -1 = biometric, -2 = backspace
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    when (key) {
                        -1 -> BiometricKey(onClick = onBiometric)
                        -2 -> BackspaceKey(onClick = onBackspace)
                        else -> PinDigitKey(digit = key, onClick = { onDigit(key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDigitKey(digit: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BackspaceKey(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BiometricKey(onClick: () -> Unit) {
    val context = LocalContext.current
    val manager = BiometricManager.from(context)
    val available = manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
            or BiometricManager.Authenticators.BIOMETRIC_WEAK
    ) == BiometricManager.BIOMETRIC_SUCCESS

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(enabled = available, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Fingerprint,
            contentDescription = "Fingerprint",
            tint = if (available) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
