package app.habivance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val frequency: String,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val createdAt: Long,
    val isArchived: Boolean = false,
    val sortOrder: Long = 0L,
    val priority: String = "NORMAL"
)
