package uk.co.edcourtenay.quiztracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One team's answer to a tie-break question, keyed by the contested [rank] and
 * the team. Cascades from [TeamEntity], so deleting a team cleans up its answer.
 */
@Entity(
    tableName = "tiebreak_answers",
    primaryKeys = ["rank", "teamId"],
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("teamId")],
)
data class TieBreakAnswerEntity(
    val rank: Int,
    val teamId: Long,
    val answer: Double,
)
