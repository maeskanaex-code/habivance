package app.habivance.ui.welcome

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Three-bar mark
            Canvas(
                modifier = Modifier
                    .width(120.dp)
                    .height(100.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = canvasWidth * 0.20f
                val gap = canvasWidth * 0.10f
                val totalWidth = (barWidth * 3) + (gap * 2)
                val startX = (canvasWidth - totalWidth) / 2f
                val cornerRadius = barWidth / 2f
                val amber = Color(0xFFF59E0B)

                // Left bar
                drawRoundRect(
                    color = amber,
                    topLeft = Offset(startX, canvasHeight * 0.45f),
                    size = Size(barWidth, canvasHeight * 0.55f),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
                // Middle bar
                drawRoundRect(
                    color = amber,
                    topLeft = androidx.compose.ui.geometry.Offset(startX + barWidth + gap, canvasHeight * 0.25f),
                    size = androidx.compose.ui.geometry.Size(barWidth, canvasHeight * 0.75f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
                // Right bar
                drawRoundRect(
                    color = amber,
                    topLeft = androidx.compose.ui.geometry.Offset(startX + (barWidth + gap) * 2, canvasHeight * 0.0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, canvasHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Build better habits",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "one day at a time.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "No account needed.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your data stays on your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Get started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            PrivacyPolicyLink(
                url = "https://maeskanaex-code.github.io/habivance-privacy/"
            )
        }
    }
}

@Composable
private fun PrivacyPolicyLink(url: String) {
    val context = LocalContext.current

    val annotated = buildAnnotatedString {
        append("By tapping Get started, you agree to our ")
        withStyle(
            SpanStyle(
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Privacy Policy")
        }
        append(".")
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.clickable {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    )
}
