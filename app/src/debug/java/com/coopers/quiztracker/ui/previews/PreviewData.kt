package com.coopers.quiztracker.ui.previews

import com.coopers.quiztracker.data.ConfigEntity
import com.coopers.quiztracker.data.TeamEntity
import com.coopers.quiztracker.domain.Scoring
import com.coopers.quiztracker.ui.QuizUiState

/**
 * Sample data for Compose previews, shaped like a real quiz night
 * (and the original workbook's placeholders).
 */
internal object PreviewData {

    val config = ConfigEntity(quizName = "Coopers Quiz", feePerPlayer = 2.0)

    private fun team(
        id: Long,
        name: String,
        players: Int?,
        rounds1to4: Int?,
        rounds5to6: Int?,
        wipeout: Int?,
    ) = TeamEntity(
        id = id,
        name = name,
        players = players,
        rounds1to4 = rounds1to4,
        rounds5to6 = rounds5to6,
        wipeout = wipeout,
        createdAt = id,
    )

    val teams = listOf(
        team(1, "Kinder Suprise", 4, 20, 14, 3),
        team(2, "Quiz Hemsworth", 3, 21, 12, 5),
        team(3, "Hip Hip", 5, 17, 13, 0),
        team(4, "Groovy Gang", 4, 16, 9, 8),
        team(5, "Edward Quizzerhands", 2, 24, null, null),
    )

    fun state(teams: List<TeamEntity> = this.teams): QuizUiState = QuizUiState(
        teams = teams,
        scores = Scoring.scoreTeams(teams, config.feePerPlayer),
        config = config,
    )

    val emptyState: QuizUiState = state(teams = emptyList())

    /**
     * A night with shared ranks: two teams tie at 2nd and two tie at 4th, so the
     * board badges should read "=2" and "=4".
     */
    val tiedState: QuizUiState = state(
        teams = listOf(
            team(1, "Alpha", 3, 20, 10, 2),   // total 32
            team(2, "Bravo", 4, 15, 10, 2),   // total 27, tied 2nd
            team(3, "Charlie", 2, 15, 10, 2), // total 27, tied 2nd
            team(4, "Delta", 3, 10, 10, 2),   // total 22, tied 4th
            team(5, "Echo", 3, 10, 10, 2),    // total 22, tied 4th
        ),
    )
}
