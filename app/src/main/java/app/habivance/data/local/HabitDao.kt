package app.habivance.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // ----- Habits -----
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("SELECT MAX(sortOrder) FROM habits")
    suspend fun getMaxSortOrder(): Long?

    @Query("UPDATE habits SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, newOrder: Long)

    @Query("UPDATE habits SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: String)

    @Transaction
    suspend fun reorderAllInTransaction(pairs: List<Pair<Long, Long>>) {
        pairs.forEach { (id, order) ->
            updateSortOrder(id, order)
        }
    }

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND sortOrder < :currentSort ORDER BY sortOrder DESC LIMIT 1")
    suspend fun getHabitAbove(currentSort: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND sortOrder > :currentSort ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getHabitBelow(currentSort: Long): HabitEntity?

    // ----- Completions -----
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateEpochDay DESC")
    fun observeCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun getCompletion(habitId: Long, epochDay: Long): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay")
    suspend fun deleteCompletion(habitId: Long, epochDay: Long)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    suspend fun getAllCompletionsForHabit(habitId: Long): List<HabitCompletionEntity>

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAllCompletions()
}
