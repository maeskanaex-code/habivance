package app.habivance.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

private const val WEEKS = 26
private const val CELL_SIZE = 11
private const val CELL_GAP = 3

@Composable
fun HabitHeatmap(
    completionEpochDays: Set<Long>,
    habitColor: Color,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()
) {
    val startDate = remember(today) {
        // Go back to the most recent Sunday, then back 25 more weeks
        val daysSinceSunday = today.dayOfWeek.value % 7 // Sunday=0
        today.minusDays(daysSinceSunday.toLong()).minusWeeks((WEEKS - 1).toLong())
    }

    Row(modifier = modifier) {
        DayLabels()
        Spacer(Modifier.width(6.dp))
        Column {
            MonthLabels(startDate = startDate, today = today)
            Spacer(Modifier.size(4.dp))
            WeekGrid(
                startDate = startDate,
                completionEpochDays = completionEpochDays,
                habitColor = habitColor,
                today = today
            )
        }
    }
}

@Composable
private fun DayLabels() {
    Column(
        verticalArrangement = Arrangement.spacedBy((CELL_GAP).dp),
        modifier = Modifier.padding(top = 18.dp) // offset for month labels row
    ) {
        listOf("M", "", "W", "", "F", "", "S").forEach { label ->
            Box(
                modifier = Modifier.size(CELL_SIZE.dp),
                contentAlignment = Alignment.Center
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthLabels(startDate: LocalDate, today: LocalDate) {
    // Approximate: place a label at every ~4 weeks if the month changed
    val labels = remember(startDate) {
        val result = mutableListOf<Pair<Int, String>>()
        var lastMonth = -1
        var weekIndex = 0
        var cursor = startDate
        while (!cursor.isAfter(today)) {
            if (cursor.monthValue != lastMonth) {
                lastMonth = cursor.monthValue
                result.add(weekIndex to cursor.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() })
            }
            cursor = cursor.plusWeeks(1)
            weekIndex++
        }
        result
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { (weekIdx, name) ->
            val offset = (weekIdx * (CELL_SIZE + CELL_GAP)).dp
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = offset)
            )
        }
    }
}

@Composable
private fun WeekGrid(
    startDate: LocalDate,
    completionEpochDays: Set<Long>,
    habitColor: Color,
    today: LocalDate
) {
    Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP.dp)) {
        for (week in 0 until WEEKS) {
            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP.dp)) {
                for (day in 0 until 7) {
                    val date = startDate.plusDays((week * 7 + day).toLong())
                    val inFuture = date.isAfter(today)
                    val completed = completionEpochDays.contains(date.toEpochDay())
                    HeatmapCell(
                        completed = completed,
                        inFuture = inFuture,
                        habitColor = habitColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    completed: Boolean,
    inFuture: Boolean,
    habitColor: Color
) {
    val bg = when {
        inFuture -> Color.Transparent
        completed -> habitColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(CELL_SIZE.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(bg)
    )
}
