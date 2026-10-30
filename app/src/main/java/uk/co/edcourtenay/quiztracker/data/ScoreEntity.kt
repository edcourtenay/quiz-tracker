package uk.co.edcourtenay.quiztracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One team's score for one round.
 *
 * The number of rounds is user-configurable, so scores live in their own table
 * keyed by (team, round). **An absent row means "not entered"** — which is
 * deliberately different from an entered score of 0 (the red-zero highlight
 * depends on that distinction).
 *
 * Both foreign keys cascade, and in opposite directions: deleting a team removes
 * only its scores, and deleting a round removes only that round's scores. Neither
 * touches the other's parent table, so deleting every team leaves the configured
 * rounds intact.
 */
@Entity(
    tableName = "scores",
    primaryKeys = ["teamId", "roundId"],
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("roundId")],
)
data class ScoreEntity(
    val teamId: Long,
    val roundId: Long,
    val value: Int,
)
