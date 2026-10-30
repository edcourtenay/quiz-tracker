package com.coopers.quiztracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TeamEntity::class, ConfigEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao

    companion object {
        const val DB_NAME = "quiz-tracker.db"

        @Volatile
        private var instance: QuizDatabase? = null

        fun get(context: Context): QuizDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): QuizDatabase =
            Room.databaseBuilder(context, QuizDatabase::class.java, DB_NAME).build()
    }
}
