package com.coopers.quiztracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A team taking part in the quiz night.
 *
 * Mirrors a row of the "Scores" sheet in Coopers Quiz.xlsx.
 * Nullable score fields represent "not entered yet" (a blank cell in the sheet)
 * which is different from an entered score of 0.
 */
@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    /** Number of players in the team. Drives the entry fee. */
    val players: Int? = null,
    /** Combined score for rounds 1-4 (sheet column D). */
    val rounds1to4: Int? = null,
    /** Combined score for rounds 5-6 (sheet column F). */
    val rounds5to6: Int? = null,
    /** Score for the final "Wipeout" round (sheet column I). */
    val wipeout: Int? = null,
    /** Stable ordering of the rows as entered. */
    val createdAt: Long = System.currentTimeMillis(),
)
