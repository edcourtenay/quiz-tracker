package uk.co.edcourtenay.quiztracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A scored stage of the quiz night, previously hard-coded as "Rounds 1-4",
 * "Rounds 5-6" and "Wipeout".
 *
 * Rounds are configured by the user (add / rename / reorder / remove), so scores
 * are keyed by [id] rather than by column — renaming a round never detaches its
 * scores.
 */
@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    /** Order in which rounds are played; the last round is excluded from the subtotal. */
    val position: Int = 0,
)
