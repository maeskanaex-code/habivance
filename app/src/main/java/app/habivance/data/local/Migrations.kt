package app.habivance.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE habits ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
        // Preserve current order: sortOrder = createdAt for existing habits
        db.execSQL("UPDATE habits SET sortOrder = createdAt WHERE sortOrder = 0")
    }
}
