package app.habivance.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long,
    val habits: List<BackupHabit>
)

@Serializable
data class BackupHabit(
    val name: String,
    val emoji: String,
    val colorHex: String,
    val frequency: String,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val createdAt: Long,
    val isArchived: Boolean = false,
    val completionEpochDays: List<Long> = emptyList()
)
