package uk.co.edcourtenay.quiztracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The tie-break asked when two or more qualifying teams share a final position.
 *
 * Keyed by the contested [rank] (the final position the teams are tied on). The
 * tie-break is always a "closest number" question: the host records the question,
 * each tied team's guess, and finally the correct value.
 */
@Entity(tableName = "tiebreaks")
data class TieBreakEntity(
    @PrimaryKey val rank: Int,
    val question: String = "",
    val correctAnswer: Double? = null,
)
