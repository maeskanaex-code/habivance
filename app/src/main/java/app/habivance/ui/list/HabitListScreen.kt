package app.habivance.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.habivance.domain.model.Habit
import app.habivance.domain.model.HabitPriority
import app.habivance.domain.model.HabitWithStatus
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    modifier: Modifier = Modifier,
    onAddHabit: () -> Unit = {},
    onEditHabit: (Long) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: HabitListViewModel = viewModel()
) {
    val habits by viewModel.habitsWithStatus.collectAsState()
    val todayCompleted by viewModel.todayCompleted.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val weekCompletions by viewModel.weekCompletions.collectAsState()
    var habitPendingDelete by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Habivance", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            DateHeader()
            Spacer(Modifier.height(16.dp))
            ProgressHeader(completed = todayCompleted, total = todayTotal)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "This week",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            WeeklyHeatmap(
                dailyCompletions = weekCompletions,
                habitsTotal = todayTotal
            )
            Spacer(Modifier.height(24.dp))

            if (habits.isEmpty()) {
                EmptyHabitsMessage()
            } else {
                HabitReorderableList(
                    habits = habits,
                    viewModel = viewModel,
                    onEditHabit = onEditHabit,
                    onDeleteRequested = { habitPendingDelete = it }
                )
            }
        }
    }

    habitPendingDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitPendingDelete = null },
            title = { Text("Delete habit?") },
            text = { Text("\"${habit.name}\" and all its history will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHabit(habit)
                    habitPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { habitPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HabitReorderableList(
    habits: List<HabitWithStatus>,
    viewModel: HabitListViewModel,
    onEditHabit: (Long) -> Unit,
    onDeleteRequested: (Habit) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val localItems = remember { mutableStateListOf<HabitWithStatus>() }

    // Sync from DB, but only when IDs actually differ.
    // If only non-order fields changed (e.g., isCompletedToday), update in-place 
    // without clearing the list — avoids full recomposition.
    LaunchedEffect(habits) {
        val dbIds = habits.map { it.habit.id }
        val localIds = localItems.map { it.habit.id }
        if (dbIds != localIds) {
            // Order changed — full replace
            localItems.clear()
            localItems.addAll(habits)
        } else {
            // Same order — update in place if individual items changed
            habits.forEachIndexed { index, newItem ->
                if (index < localItems.size && localItems[index] != newItem) {
                    localItems[index] = newItem
                }
            }
        }
    }

    val reorderState = rememberReorderableLazyListState(
        listState = lazyListState,
        onMove = { from, to ->
            val fromIdx = from.index
            val toIdx = to.index
            if (fromIdx in localItems.indices && toIdx in localItems.indices) {
                localItems.add(toIdx, localItems.removeAt(fromIdx))
            }
        }
    )

    // Persist on drag release
    LaunchedEffect(reorderState) {
        snapshotFlow { reorderState.draggingItemKey }
            .collect { draggingKey ->
                if (draggingKey == null && localItems.isNotEmpty()) {
                    val ids = localItems.map { it.habit.id }
                    val dbIds = habits.map { it.habit.id }
                    if (ids != dbIds) {
                        viewModel.reorderHabits(localItems.map { it.habit })
                    }
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState),
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = localItems,
            key = { it.habit.id }
        ) { item ->
            ReorderableItem(
                state = reorderState,
                key = item.habit.id
            ) { isDragging ->
                Box(
                    modifier = Modifier
                        .detectReorderAfterLongPress(reorderState)
                ) {
                    HabitRow(
                        item = item,
                        isDragging = isDragging,
                        onToggle = { viewModel.toggleToday(item.habit) },
                        onClick = { onEditHabit(item.habit.id) },
                        onDelete = { onDeleteRequested(item.habit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateHeader() {
    val today = remember { LocalDate.now() }
    val weekday = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
    }
    val monthDay = remember(today) {
        today.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH))
    }

    Column {
        Text(
            text = weekday,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = monthDay,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressHeader(completed: Int, total: Int) {
    val subtitle = when {
        total == 0 -> "No habits yet."
        completed == 0 -> "Let's get started."
        completed == total -> "All done for today."
        else -> "$completed done · ${total - completed} to go."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressRing(completed = completed, total = total)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HabitRow(
    item: HabitWithStatus,
    isDragging: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = item.habit
    val habitColor = remember(habit.colorHex) {
        try { Color(android.graphics.Color.parseColor(habit.colorHex)) }
        catch (_: Exception) { Color(0xFF3B82F6) }
    }

    val borderColor = when (habit.priority) {
        HabitPriority.HIGH -> MaterialTheme.colorScheme.primary
        HabitPriority.LOW -> MaterialTheme.colorScheme.outline
        HabitPriority.NORMAL -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = when (habit.priority) {
        HabitPriority.HIGH -> 2.dp
        HabitPriority.LOW -> 1.dp
        HabitPriority.NORMAL -> 1.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(habit.id) {
                detectTapGestures(onTap = { onClick() })
            }
            .then(modifier),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckCircle(
                isChecked = item.isCompletedToday,
                color = habitColor,
                onClick = onToggle
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${habit.emoji}  ${habit.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.currentStreak > 0) {
                StreakBadge(streak = item.currentStreak)
                Spacer(Modifier.width(8.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckCircle(
    isChecked: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isChecked) color else Color.Transparent
    val borderColor = if (isChecked) color else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "🔥 $streak",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun EmptyHabitsMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No habits yet.\nTap + to create your first one.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
