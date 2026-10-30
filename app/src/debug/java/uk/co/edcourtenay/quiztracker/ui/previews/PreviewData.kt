package uk.co.edcourtenay.quiztracker.ui.previews

import uk.co.edcourtenay.quiztracker.data.ConfigEntity
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.ScoreEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import uk.co.edcourtenay.quiztracker.domain.Scoring
import uk.co.edcourtenay.quiztracker.ui.QuizUiState

/**
 * Sample data for Compose previews, shaped like a real quiz night
 * (and the original workbook's placeholders).
 */
internal object PreviewData {

    val config = ConfigEntity(quizName = "Coopers Quiz", feePerPlayer = 2.0)

    /** The three rounds the app used to hard-code. */
    val rounds = listOf(
        RoundEntity(id = 1, name = "Rounds 1-4", position = 0),
        RoundEntity(id = 2, name = "Rounds 5-6", position = 1),
        RoundEntity(id = 3, name = "Wipeout", position = 2, highlightZero = true),
    )

    private fun team(id: Long, name: String, players: Int?) = TeamEntity(
        id = id,
        name = name,
        players = players,
        createdAt = id,
    )

    /** teamId -> (round 1, round 2, round 3); null means "not entered". */
    private val entered = listOf(
        Triple(team(1, "Kinder Suprise", 4), 20, 14) to 3,
        Triple(team(2, "Quiz Hemsworth", 3), 21, 12) to 5,
        Triple(team(3, "Hip Hip", 5), 17, 13) to 0,
        Triple(team(4, "Groovy Gang", 4), 16, 9) to 8,
        Triple(team(5, "Edward Quizzerhands", 2), 24, null) to null,
    )

    private val teams: List<TeamEntity> = entered.map { it.first.first }

    private val scores: List<ScoreEntity> = entered.flatMap { (row, wipeout) ->
        val (team, round1, round2) = row
        listOfNotNull(
            round1?.let { ScoreEntity(team.id, 1, it) },
            round2?.let { ScoreEntity(team.id, 2, it) },
            wipeout?.let { ScoreEntity(team.id, 3, it) },
        )
    }

    fun state(
        teams: List<TeamEntity> = this.teams,
        rounds: List<RoundEntity> = this.rounds,
        scores: List<ScoreEntity> = this.scores,
    ): QuizUiState = QuizUiState(
        teams = teams,
        rounds = rounds,
        scores = Scoring.scoreTeams(teams, rounds, scores, config.feePerPlayer),
        config = config,
    )

    val emptyState: QuizUiState = state(teams = emptyList(), scores = emptyList())

    /**
     * A night with shared ranks: two teams tie at 2nd and two tie at 4th, so the
     * board badges should read "=2" and "=4".
     */
    val tiedState: QuizUiState = run {
        val tiedTeams = listOf(
            team(1, "Alpha", 3),
            team(2, "Bravo", 4),
            team(3, "Charlie", 2),
            team(4, "Delta", 3),
            team(5, "Echo", 3),
        )
        val tiedScores = listOf(
            ScoreEntity(1, 1, 20), ScoreEntity(1, 2, 10), ScoreEntity(1, 3, 2),
            ScoreEntity(2, 1, 15), ScoreEntity(2, 2, 10), ScoreEntity(2, 3, 2),
            ScoreEntity(3, 1, 15), ScoreEntity(3, 2, 10), ScoreEntity(3, 3, 2),
            ScoreEntity(4, 1, 10), ScoreEntity(4, 2, 10), ScoreEntity(4, 3, 2),
            ScoreEntity(5, 1, 10), ScoreEntity(5, 2, 10), ScoreEntity(5, 3, 2),
        )
        state(teams = tiedTeams, scores = tiedScores)
    }
}
