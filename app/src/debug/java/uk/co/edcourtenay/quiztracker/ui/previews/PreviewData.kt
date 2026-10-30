package uk.co.edcourtenay.quiztracker.ui.previews

import uk.co.edcourtenay.quiztracker.data.ConfigEntity
import uk.co.edcourtenay.quiztracker.data.PrizeEntity
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.ScoreEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import uk.co.edcourtenay.quiztracker.domain.Prizes
import uk.co.edcourtenay.quiztracker.domain.Scoring
import uk.co.edcourtenay.quiztracker.domain.TieBreakState
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
        RoundEntity(id = 3, name = "Wipeout", position = 2),
    )

    val prizes = listOf(
        PrizeEntity(id = 1, name = "1st", value = 50.0, position = 0),
        PrizeEntity(id = 2, name = "2nd", value = 30.0, position = 1),
        PrizeEntity(id = 3, name = "3rd", value = 10.0, position = 2),
    )

    private fun team(id: Long, name: String, players: Int?) =
        TeamEntity(id = id, name = name, players = players, createdAt = id)

    private fun score(teamId: Long, roundId: Long, value: Int) =
        ScoreEntity(teamId = teamId, roundId = roundId, value = value)

    // ---------------------------------------------------------------------
    // The default sample night
    // ---------------------------------------------------------------------

    val teams = listOf(
        team(1, "Kinder Suprise", 4),
        team(2, "Quiz Hemsworth", 3),
        team(3, "Hip Hip", 5),
        team(4, "Groovy Gang", 4),
        team(5, "Edward Quizzerhands", 2),
    )

    val scores = listOf(
        score(1, 1, 20), score(1, 2, 14), score(1, 3, 3),
        score(2, 1, 21), score(2, 2, 12), score(2, 3, 5),
        score(3, 1, 17), score(3, 2, 13), score(3, 3, 0),
        score(4, 1, 16), score(4, 2, 9), score(4, 3, 8),
        score(5, 1, 24), // rounds 5-6 and Wipeout not entered yet
    )

    fun state(
        teams: List<TeamEntity> = this.teams,
        rounds: List<RoundEntity> = this.rounds,
        scores: List<ScoreEntity> = this.scores,
        prizes: List<PrizeEntity> = emptyList(),
        tieBreaks: Map<Int, TieBreakState> = emptyMap(),
    ): QuizUiState {
        val scored = Scoring.scoreTeams(teams, rounds, scores, config.feePerPlayer)
        return QuizUiState(
            teams = teams,
            rounds = rounds,
            scores = scored,
            prizes = prizes,
            standings = Prizes.standings(scored, prizes, tieBreaks),
            tieBreaks = tieBreaks,
            config = config,
        )
    }

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
            score(1, 1, 20), score(1, 2, 10), score(1, 3, 2),
            score(2, 1, 15), score(2, 2, 10), score(2, 3, 2),
            score(3, 1, 15), score(3, 2, 10), score(3, 3, 2),
            score(4, 1, 10), score(4, 2, 10), score(4, 3, 2),
            score(5, 1, 10), score(5, 2, 10), score(5, 3, 2),
        )
        state(teams = tiedTeams, scores = tiedScores)
    }

    // ---------------------------------------------------------------------
    // Prize scenarios
    // ---------------------------------------------------------------------

    /** A finished night with no ties: every prize is awarded outright. */
    val prizesAwardedState: QuizUiState = run {
        val awarded = listOf(
            team(1, "Groovy Gang", 4),
            team(2, "Quiz Hemsworth", 3),
            team(3, "Kinder Suprise", 4),
            team(4, "Hip Hip", 5),
        )
        val awardedScores = listOf(
            score(1, 1, 20), score(1, 2, 10), score(1, 3, 10), // 40
            score(2, 1, 21), score(2, 2, 12), score(2, 3, 2),  // 35
            score(3, 1, 20), score(3, 2, 14), score(3, 3, 0),  // 34
            score(4, 1, 17), score(4, 2, 13), score(4, 3, 0),  // 30
        )
        state(teams = awarded, scores = awardedScores, prizes = prizes)
    }

    private val tiedForPrizesTeams = listOf(
        team(1, "Groovy Gang", 4),
        team(2, "Quiz Hemsworth", 3),
        team(3, "Kinder Suprise", 4),
    )

    private val tiedForPrizesScores = listOf(
        score(1, 1, 20), score(1, 2, 10), score(1, 3, 10), // 40
        score(2, 1, 21), score(2, 2, 12), score(2, 3, 5),  // 38
        score(3, 1, 20), score(3, 2, 14), score(3, 3, 4),  // 38
    )

    /** Two teams tied on the last two prizes, awaiting a tie-break. */
    val tiedForPrizesState: QuizUiState =
        state(teams = tiedForPrizesTeams, scores = tiedForPrizesScores, prizes = prizes)

    /** The same night with the tie-break answered and settled. */
    val tieBreakResolvedState: QuizUiState = run {
        val tieBreaks = mapOf(
            2 to TieBreakState(
                question = "In which year did the Hundred Years War end?",
                correctAnswer = 1453.0,
                answers = mapOf(
                    tiedForPrizesTeams[1].id to 1415.0,
                    tiedForPrizesTeams[2].id to 1485.0,
                ),
            ),
        )
        state(
            teams = tiedForPrizesTeams,
            scores = tiedForPrizesScores,
            prizes = prizes,
            tieBreaks = tieBreaks,
        )
    }
}
