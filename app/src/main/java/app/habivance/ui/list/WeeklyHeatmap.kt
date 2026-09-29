package app.habivance.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun WeeklyHeatmap(
    // completed counts per day, index 0=Monday, 6=Sunday. Value range 0..habitsTotal
    dailyCompletions: List<Int>,
    habitsTotal: Int,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DAY_LABELS.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until 7) {
                val count = dailyCompletions.getOrNull(i) ?: 0
                val fraction = if (habitsTotal == 0) 0f
                    else count.toFloat() / habitsTotal.toFloat()
                val alpha = (0.15f + fraction * 0.85f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (count > 0) accent.copy(alpha = alpha) else empty)
                )
            }
        }
    }
}
