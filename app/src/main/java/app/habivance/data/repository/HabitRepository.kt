package app.habivance.data.repository

import app.habivance.data.local.HabitCompletionEntity
import app.habivance.data.local.HabitDao
import app.habivance.data.mapper.toDomain
import app.habivance.data.mapper.toEntity
import app.habivance.domain.model.Habit
import app.habivance.domain.model.HabitCompletion
import app.habivance.domain.model.HabitPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    suspend fun getHabit(id: Long): Habit?
    suspend fun upsertHabit(habit: Habit): Long
    suspend fun deleteHabit(habit: Habit)
    suspend fun deleteAllData()
    suspend fun reorderAll(orderedHabits: List<Habit>)

    fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>>
    suspend fun getAllCompletions(habitId: Long): List<HabitCompletion>
    suspend fun isCompleted(habitId: Long, epochDay: Long): Boolean
    suspend fun toggleCompletion(habitId: Long, epochDay: Long, completedAt: Long)
    suspend fun isCompletedToday(habitId: Long): Boolean
    suspend fun getCompletionEpochDays(habitId: Long): Set<Long>
}

class HabitRepositoryImpl(private val dao: HabitDao) : HabitRepository {

    override fun observeActiveHabits(): Flow<List<Habit>> =
        dao.observeActiveHabits().map { list -> list.map { it.toDomain() } }

    override suspend fun getHabit(id: Long): Habit? = dao.getHabitById(id)?.toDomain()

    override suspend fun upsertHabit(habit: Habit): Long {
        val entity = habit.toEntity()
        return if (entity.id == 0L) {
            val newOrder = (dao.getMaxSortOrder() ?: 0L) + 1L
            dao.insertHabit(entity.copy(sortOrder = newOrder))
        } else {
            dao.updateHabit(entity)
            entity.id
        }
    }

    override suspend fun deleteHabit(habit: Habit) = dao.deleteHabit(habit.toEntity())

    override suspend fun deleteAllData() {
        dao.deleteAllCompletions()
        dao.deleteAllHabits()
    }

    override suspend fun reorderAll(orderedHabits: List<Habit>) {
        dao.reorderAllInTransaction(orderedHabits.mapIndexed { i, h -> h.id to (i + 1).toLong() })
    }

    override fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>> =
        dao.observeCompletionsForHabit(habitId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllCompletions(habitId: Long): List<HabitCompletion> =
        dao.getAllCompletionsForHabit(habitId).map { it.toDomain() }

    override suspend fun isCompleted(habitId: Long, epochDay: Long): Boolean =
        dao.getCompletion(habitId, epochDay) != null

    override suspend fun toggleCompletion(habitId: Long, epochDay: Long, completedAt: Long) {
        val existing = dao.getCompletion(habitId, epochDay)
        if (existing == null) {
            dao.insertCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    dateEpochDay = epochDay,
                    completedAt = System.currentTimeMillis()
                )
            )
        } else {
            dao.deleteCompletion(habitId, epochDay)
        }
    }

    override suspend fun isCompletedToday(habitId: Long): Boolean {
        val today = LocalDate.now().toEpochDay()
        return dao.getCompletion(habitId, today) != null
    }

    override suspend fun getCompletionEpochDays(habitId: Long): Set<Long> =
        dao.getAllCompletionsForHabit(habitId).map { it.dateEpochDay }.toSet()
}
