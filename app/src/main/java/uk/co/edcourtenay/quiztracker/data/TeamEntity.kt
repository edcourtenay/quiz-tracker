package uk.co.edcourtenay.quiztracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A team taking part in the quiz night.
 *
 * Mirrors a row of the "Scores" sheet in Coopers Quiz.xlsx. Scores are no longer
 * columns here — the rounds are user-configurable, so they live in [ScoreEntity].
 */
@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    /** Number of players in the team. Drives the entry fee. */
    val players: Int? = null,
    /** Stable ordering of the rows as entered. */
    val createdAt: Long = System.currentTimeMillis(),
)
