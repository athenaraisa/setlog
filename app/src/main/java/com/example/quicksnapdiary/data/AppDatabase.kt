package com.example.quicksnapdiary.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE clips ADD COLUMN videoRotation INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [Clip::class, Vlog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
    abstract fun vlogDao(): VlogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "diary.db"
            )
                .addMigrations(MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}
