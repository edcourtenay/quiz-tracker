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
}
