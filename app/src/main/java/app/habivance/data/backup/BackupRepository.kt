package app.habivance.data.backup

import android.content.Context
import app.habivance.data.local.HabitCompletionEntity
import app.habivance.data.local.HabitEntity
import app.habivance.data.local.HabivanceDatabase
import app.habivance.domain.model.HabitFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class BackupRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(outputStream: OutputStream): Int = withContext(Dispatchers.IO) {
        val db = HabivanceDatabase.getInstance(context)

        val allHabitEntities = db.query("SELECT * FROM habits ORDER BY createdAt DESC", null)
            .use { cursor ->
                val list = mutableListOf<HabitEntity>()
                while (cursor.moveToNext()) {
                    list.add(
                        HabitEntity(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                            emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji")),
                            colorHex = cursor.getString(cursor.getColumnIndexOrThrow("colorHex")),
                            frequency = cursor.getString(cursor.getColumnIndexOrThrow("frequency")),
                            reminderHour = cursor.getInt(cursor.getColumnIndexOrThrow("reminderHour"))
                                .takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("reminderHour")) },
                            reminderMinute = cursor.getInt(cursor.getColumnIndexOrThrow("reminderMinute"))
                                .takeIf { !cursor.isNull(cursor.getColumnIndexOrThrow("reminderMinute")) },
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                            isArchived = cursor.getInt(cursor.getColumnIndexOrThrow("isArchived")) == 1
                        )
                    )
                }
                list
            }

        val backupHabits = allHabitEntities.map { habit ->
            val completions = db.query(
                "SELECT dateEpochDay FROM habit_completions WHERE habitId = ?",
                arrayOf(habit.id)
            ).use { c ->
                val list = mutableListOf<Long>()
                while (c.moveToNext()) list.add(c.getLong(0))
                list.sorted()
            }
            BackupHabit(
                name = habit.name,
                emoji = habit.emoji,
                colorHex = habit.colorHex,
                frequency = habit.frequency,
                reminderHour = habit.reminderHour,
                reminderMinute = habit.reminderMinute,
                createdAt = habit.createdAt,
                isArchived = habit.isArchived,
                completionEpochDays = completions
            )
        }

        val file = BackupFile(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            habits = backupHabits
        )

        val text = json.encodeToString(BackupFile.serializer(), file)
        outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        backupHabits.size
    }

    suspend fun import(inputStream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        val text = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        val parsed = json.decodeFromString(BackupFile.serializer(), text)

        val db = HabivanceDatabase.getInstance(context)
        val habitDao = db.habitDao()

        var added = 0
        var merged = 0
        var completionsAdded = 0

        for (bHabit in parsed.habits) {
            val existingCursor = db.query(
                "SELECT id FROM habits WHERE name = ? LIMIT 1",
                arrayOf(bHabit.name)
            )
            val existingId = existingCursor.use { c ->
                if (c.moveToFirst()) c.getLong(0) else null
            }

            val habitId: Long = if (existingId != null) {
                merged++
                existingId
            } else {
                added++
                habitDao.insertHabit(
                    HabitEntity(
                        name = bHabit.name,
                        emoji = bHabit.emoji,
                        colorHex = bHabit.colorHex,
                        frequency = try {
                            HabitFrequency.valueOf(bHabit.frequency).name
                        } catch (_: Exception) { "DAILY" },
                        reminderHour = bHabit.reminderHour,
                        reminderMinute = bHabit.reminderMinute,
                        createdAt = bHabit.createdAt,
                        isArchived = bHabit.isArchived,
                        sortOrder = habitDao.getMaxSortOrder()?.plus(1L) ?: 1L,
                        priority = "NORMAL"
                    )
                )
            }

            for (epochDay in bHabit.completionEpochDays) {
                val result = habitDao.insertCompletion(
                    HabitCompletionEntity(
                        habitId = habitId,
                        dateEpochDay = epochDay,
                        completedAt = System.currentTimeMillis()
                    )
                )
                if (result != -1L) completionsAdded++
            }
        }

        ImportResult(
            habitsAdded = added,
            habitsMerged = merged,
            completionsAdded = completionsAdded
        )
    }
}

data class ImportResult(
    val habitsAdded: Int,
    val habitsMerged: Int,
    val completionsAdded: Int
)
