package uk.co.edcourtenay.quiztracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TeamEntity::class, RoundEntity::class, ScoreEntity::class, ConfigEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao

    companion object {
        const val DB_NAME = "quiz-tracker.db"

        /** The rounds that used to be hard-coded, seeded so behaviour is unchanged. */
        private val SEEDED_ROUNDS = listOf(
            Triple("Rounds 1-4", 0, 0),
            Triple("Rounds 5-6", 1, 0),
            Triple("Wipeout", 2, 1),
        )

        /**
         * v1 -> v2: rounds become configurable.
         *
         * The three fixed score columns on `teams` move into a `scores` table keyed
         * by (team, round), and the three rounds are seeded with the ids 1..3 that
         * the copied scores reference.
         *
         * The old values are stashed in a temporary table *before* `teams` is
         * rebuilt, because recreating a table that other tables reference would
         * rewrite their foreign keys.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `_scores_backup` " +
                        "(`teamId` INTEGER NOT NULL, `roundId` INTEGER NOT NULL, `value` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `_scores_backup` (teamId, roundId, value) " +
                        "SELECT id, 1, rounds1to4 FROM teams WHERE rounds1to4 IS NOT NULL",
                )
                db.execSQL(
                    "INSERT INTO `_scores_backup` (teamId, roundId, value) " +
                        "SELECT id, 2, rounds5to6 FROM teams WHERE rounds5to6 IS NOT NULL",
                )
                db.execSQL(
                    "INSERT INTO `_scores_backup` (teamId, roundId, value) " +
                        "SELECT id, 3, wipeout FROM teams WHERE wipeout IS NOT NULL",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `teams_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `players` INTEGER, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `teams_new` (id, name, players, createdAt) " +
                        "SELECT id, name, players, createdAt FROM teams",
                )
                db.execSQL("DROP TABLE `teams`")
                db.execSQL("ALTER TABLE `teams_new` RENAME TO `teams`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rounds` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                        "`highlightZero` INTEGER NOT NULL)",
                )
                SEEDED_ROUNDS.forEachIndexed { index, (name, position, highlightZero) ->
                    db.execSQL(
                        "INSERT INTO `rounds` (id, name, position, highlightZero) VALUES (?, ?, ?, ?)",
                        arrayOf<Any>(index + 1, name, position, highlightZero),
                    )
                }

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scores` (" +
                        "`teamId` INTEGER NOT NULL, `roundId` INTEGER NOT NULL, `value` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`teamId`, `roundId`), " +
                        "FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`roundId`) REFERENCES `rounds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_scores_roundId` ON `scores` (`roundId`)")
                db.execSQL("INSERT INTO `scores` (teamId, roundId, value) SELECT teamId, roundId, value FROM `_scores_backup`")
                db.execSQL("DROP TABLE `_scores_backup`")
            }
        }

        @Volatile
        private var instance: QuizDatabase? = null

        fun get(context: Context): QuizDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): QuizDatabase =
            Room.databaseBuilder(context, QuizDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
