package app.habivance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * PIN setup dialog. Two phases:
 * 1. Enter PIN (4 digits)
 * 2. Confirm PIN
 * Calls [onPinSet] when both match.
 */
@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(Phase.ENTER) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (phase) {
                    Phase.ENTER -> "Set a PIN"
                    Phase.CONFIRM -> "Confirm your PIN"
                },
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error ?: when (phase) {
                        Phase.ENTER -> "Choose a 4-digit PIN to unlock Habivance"
                        Phase.CONFIRM -> "Enter the same PIN again"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (error != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // PIN dots
                val currentInput = if (phase == Phase.ENTER) firstPin else confirmPin
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
                                    if (i < currentInput.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Simple keypad
                val rows = listOf(
                    listOf(1, 2, 3),
                    listOf(4, 5, 6),
                    listOf(7, 8, 9),
                    listOf(-2, 0, -1)  // -2 = empty, 0 = digit, -1 = backspace
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            when (key) {
                                -1 -> {
                                    // Backspace
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (phase == Phase.ENTER && firstPin.isNotEmpty()) {
                                                    firstPin = firstPin.dropLast(1)
                                                } else if (phase == Phase.CONFIRM && confirmPin.isNotEmpty()) {
                                                    confirmPin = confirmPin.dropLast(1)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                -2 -> {
                                    // Empty slot
                                    Box(modifier = Modifier.size(48.dp))
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                error = null
                                                if (phase == Phase.ENTER) {
                                                    if (firstPin.length < 4) {
                                                        firstPin += key.toString()
                                                        if (firstPin.length == 4) {
                                                            // Move to confirm phase
                                                            phase = Phase.CONFIRM
                                                        }
                                                    }
                                                } else {
                                                    if (confirmPin.length < 4) {
                                                        confirmPin += key.toString()
                                                        if (confirmPin.length == 4) {
                                                            if (confirmPin == firstPin) {
                                                                onPinSet(firstPin)
                                                            } else {
                                                                error = "PINs don't match. Try again."
                                                                firstPin = ""
                                                                confirmPin = ""
                                                                phase = Phase.ENTER
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private enum class Phase { ENTER, CONFIRM }
