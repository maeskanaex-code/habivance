package app.habivance.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AMBER = Color(0xFFF59E0B)
private val SPLASH_BG = Color(0xFF0B0C0E)

@Composable
fun BrandedSplashScreen(
    onFinished: () -> Unit
) {
    // Bars start already visible (they carry over from the native splash).
    // Only the wordmark and tagline animate in.
    val barsAlpha = remember { Animatable(1f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Bars are already visible. Wait a moment so the native splash 
        // transition completes smoothly.
        delay(300)
        // Wordmark fades in
        wordmarkAlpha.animateTo(1f, tween(400))
        // Tagline fades in 200ms after wordmark
        delay(200)
        taglineAlpha.animateTo(1f, tween(400))
        // Hold for 800ms after animation completes
        delay(800)
        // Notify caller
        onFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SPLASH_BG
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ThreeBarMark(
                modifier = Modifier
                    .width(120.dp)
                    .height(100.dp)
                    .alpha(barsAlpha.value),
                color = AMBER
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Habivance",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(wordmarkAlpha.value)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Private by default. Offline-first habit tracking.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF8A8A8E),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}

@Composable
private fun ThreeBarMark(
    modifier: Modifier = Modifier,
    color: Color = AMBER
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Bars: widths and gaps sized for optical center.
        // Bars occupy ~72% of canvas width, centered with slight rightward bias.
        val barWidth = canvasWidth * 0.19f
        val gap = canvasWidth * 0.095f
        val totalWidth = (barWidth * 3) + (gap * 2)
        // Shift right by 2.5% of canvas width for optical center
        val startX = (canvasWidth - totalWidth) / 2f + (canvasWidth * 0.025f)

        // Heights: relative to canvasHeight. Left=60%, Middle=78%, Right=98%
        val leftHeight = canvasHeight * 0.60f
        val middleHeight = canvasHeight * 0.78f
        val rightHeight = canvasHeight * 0.98f

        val cornerRadius = barWidth / 2f

        // Left bar
        drawRoundRect(
            color = color,
            topLeft = Offset(startX, canvasHeight - leftHeight),
            size = Size(barWidth, leftHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Middle bar
        drawRoundRect(
            color = color,
            topLeft = Offset(startX + barWidth + gap, canvasHeight - middleHeight),
            size = Size(barWidth, middleHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Right bar
        drawRoundRect(
            color = color,
            topLeft = Offset(startX + (barWidth + gap) * 2, canvasHeight - rightHeight),
            size = Size(barWidth, rightHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
    }
}
