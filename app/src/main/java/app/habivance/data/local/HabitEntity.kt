package app.habivance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val frequency: String,          // "DAILY" or "WEEKLY"
    val reminderHour: Int? = null,  // null = no reminder
    val reminderMinute: Int? = null,
    val createdAt: Long,
    val isArchived: Boolean = false
)
