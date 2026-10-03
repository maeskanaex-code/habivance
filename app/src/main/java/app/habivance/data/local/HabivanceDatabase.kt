package app.habivance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HabivanceDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile private var INSTANCE: HabivanceDatabase? = null

        fun getInstance(context: Context): HabivanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabivanceDatabase::class.java,
                    "habivance.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
