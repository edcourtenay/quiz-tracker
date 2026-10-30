package uk.co.edcourtenay.quiztracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TeamEntity::class,
        RoundEntity::class,
        ScoreEntity::class,
        PrizeEntity::class,
        TieBreakEntity::class,
        TieBreakAnswerEntity::class,
        ConfigEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao

    companion object {
        const val DB_NAME = "quiz-tracker.db"

        /** The rounds that used to be hard-coded, seeded so behaviour is unchanged. */
        private val SEEDED_ROUNDS = listOf("Rounds 1-4", "Rounds 5-6", "Wipeout")

        /** A starting set of prizes; the host sets the values. */
        private val SEEDED_PRIZES = listOf("1st", "2nd", "3rd")

        private const val CREATE_PRIZES =
            "CREATE TABLE IF NOT EXISTS `prizes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `value` REAL, `position` INTEGER NOT NULL)"

        private const val CREATE_TIEBREAKS =
            "CREATE TABLE IF NOT EXISTS `tiebreaks` (" +
                "`rank` INTEGER NOT NULL, `question` TEXT NOT NULL, " +
                "`correctAnswer` REAL, PRIMARY KEY(`rank`))"

        private const val CREATE_TIEBREAK_ANSWERS =
            "CREATE TABLE IF NOT EXISTS `tiebreak_answers` (" +
                "`rank` INTEGER NOT NULL, `teamId` INTEGER NOT NULL, `answer` REAL NOT NULL, " +
                "PRIMARY KEY(`rank`, `teamId`), " +
                "FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"

        private const val CREATE_TIEBREAK_ANSWERS_INDEX =
            "CREATE INDEX IF NOT EXISTS `index_tiebreak_answers_teamId` ON `tiebreak_answers` (`teamId`)"

        private const val CREATE_SCORES =
            "CREATE TABLE IF NOT EXISTS `scores` (" +
                "`teamId` INTEGER NOT NULL, `roundId` INTEGER NOT NULL, `value` INTEGER NOT NULL, " +
                "PRIMARY KEY(`teamId`, `roundId`), " +
                "FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`roundId`) REFERENCES `rounds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"

        private const val CREATE_SCORES_ROUND_INDEX =
            "CREATE INDEX IF NOT EXISTS `index_scores_roundId` ON `scores` (`roundId`)"

        private fun seedPrizes(db: SupportSQLiteDatabase) {
            SEEDED_PRIZES.forEachIndexed { position, name ->
                db.execSQL(
                    "INSERT INTO `prizes` (name, value, position) VALUES (?, NULL, ?)",
                    arrayOf<Any>(name, position),
                )
            }
        }

        /** Seeds the current `rounds` shape: (id, name, position). */
        private fun seedRounds(db: SupportSQLiteDatabase) {
            SEEDED_ROUNDS.forEachIndexed { position, name ->
                db.execSQL(
                    "INSERT INTO `rounds` (id, name, position) VALUES (?, ?, ?)",
                    arrayOf<Any>(position + 1, name, position),
                )
            }
        }

        /** Seeds the v2/v3 shape, which still had the `highlightZero` column. */
        private fun seedRoundsWithFlag(db: SupportSQLiteDatabase) {
            SEEDED_ROUNDS.forEachIndexed { position, name ->
                db.execSQL(
                    "INSERT INTO `rounds` (id, name, position, highlightZero) VALUES (?, ?, ?, ?)",
                    arrayOf<Any>(position + 1, name, position, if (name == "Wipeout") 1 else 0),
                )
            }
        }

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
                seedRoundsWithFlag(db)

                db.execSQL(CREATE_SCORES)
                db.execSQL(CREATE_SCORES_ROUND_INDEX)
                db.execSQL("INSERT INTO `scores` (teamId, roundId, value) SELECT teamId, roundId, value FROM `_scores_backup`")
                db.execSQL("DROP TABLE `_scores_backup`")
            }
        }

        /**
         * v2 -> v3: prizes and tie-breaks.
         *
         * Both are new, so there is nothing to copy across — but the default prize
         * set is seeded here to match what a fresh install gets.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_PRIZES)
                db.execSQL(CREATE_TIEBREAKS)
                db.execSQL(CREATE_TIEBREAK_ANSWERS)
                db.execSQL(CREATE_TIEBREAK_ANSWERS_INDEX)
                seedPrizes(db)

                // Defensive: v2 had no create-callback, so a *fresh* v2 install could
                // end up with an empty rounds table. Seed it if so.
                db.query("SELECT COUNT(*) FROM rounds").use { cursor ->
                    val rounds = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    if (rounds == 0) seedRoundsWithFlag(db)
                }
            }
        }

        /**
         * v3 -> v4: drop the per-round `highlightZero` flag. A score of 0 is now
         * always shown in red, so the flag has nothing left to control.
         *
         * `scores` references `rounds`, and SQLite rewrites foreign keys when a table
         * is renamed — so the scores are stashed while both tables are rebuilt, and
         * dropped before `rounds` so the implicit delete cannot cascade into them.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `_scores_backup` " +
                        "(`teamId` INTEGER NOT NULL, `roundId` INTEGER NOT NULL, `value` INTEGER NOT NULL)",
                )
                db.execSQL("INSERT INTO `_scores_backup` (teamId, roundId, value) SELECT teamId, roundId, value FROM `scores`")
                db.execSQL("DROP TABLE `scores`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rounds_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `position` INTEGER NOT NULL)",
                )
                db.execSQL("INSERT INTO `rounds_new` (id, name, position) SELECT id, name, position FROM `rounds`")
                db.execSQL("DROP TABLE `rounds`")
                db.execSQL("ALTER TABLE `rounds_new` RENAME TO `rounds`")

                db.execSQL(CREATE_SCORES)
                db.execSQL(CREATE_SCORES_ROUND_INDEX)
                db.execSQL("INSERT INTO `scores` (teamId, roundId, value) SELECT teamId, roundId, value FROM `_scores_backup`")
                db.execSQL("DROP TABLE `_scores_backup`")
            }
        }

        /**
         * A fresh database gets the same starting rounds and prizes as an upgraded
         * one. Without this, a clean install has an empty rounds table (no score
         * fields at all), since the rounds are otherwise only seeded by the
         * v1 -> v2 migration.
         */
        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedRounds(db)
                seedPrizes(db)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(SEED_CALLBACK)
                .build()
    }
}
