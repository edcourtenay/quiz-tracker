package uk.co.edcourtenay.quiztracker.export

import uk.co.edcourtenay.quiztracker.data.ConfigEntity
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.ScoreEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import uk.co.edcourtenay.quiztracker.domain.Scoring
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultsExporterTest {

    private val rounds = listOf(
        RoundEntity(id = 1, name = "Rounds 1-4", position = 0),
        RoundEntity(id = 2, name = "Rounds 5-6", position = 1),
        RoundEntity(id = 3, name = "Wipeout", position = 2, highlightZero = true),
    )

    private fun team(id: Long, name: String, players: Int?) =
        TeamEntity(id = id, name = name, players = players, createdAt = id)

    private fun stateOf(rounds: List<RoundEntity>, teams: List<TeamEntity>, scores: List<ScoreEntity>) =
        QuizUiState(
            teams = teams,
            rounds = rounds,
            scores = Scoring.scoreTeams(teams, rounds, scores, feePerPlayer = 2.0),
            config = ConfigEntity(quizName = "Test Quiz", feePerPlayer = 2.0),
        )

    @Test
    fun `csv has a column per round and is ordered by final position`() {
        val teams = listOf(
            team(1, "Alpha", 4),
            team(2, "Bravo", 3),
            team(3, "Charlie", 2),
        )
        val scores = listOf(
            ScoreEntity(1, 1, 17), ScoreEntity(1, 2, 13), ScoreEntity(1, 3, 0),
            ScoreEntity(2, 1, 24), ScoreEntity(2, 2, 5), ScoreEntity(2, 3, 10),
            ScoreEntity(3, 1, 20), ScoreEntity(3, 2, 14), ScoreEntity(3, 3, 3),
        )

        val csv = ResultsExporter.buildCsv(stateOf(rounds, teams, scores))
        val lines = csv.trim().split("\n")

        assertEquals(
            "Team Name,Players,Amount,Rounds 1-4,Rounds 1-4 Rank,Rounds 5-6,Rounds 5-6 Rank," +
                "Wipeout,Wipeout Rank,Subtotal,Total,Final Position",
            lines[3],
        )
        // Ranks are by the running total through each round, matching the boards.
        assertEquals("Bravo,3,6.00,24,1,5,3,10,1,29,39,1", lines[4])
        assertEquals("Charlie,2,4.00,20,2,14,1,3,2,34,37,2", lines[5])
        assertEquals("Alpha,4,8.00,17,3,13,2,0,3,30,30,3", lines[6])
        assertTrue(csv.contains("Total Players,9"))
        assertTrue(csv.contains("Total Funds,18.00"))
    }

    @Test
    fun `csv follows a custom set of rounds`() {
        val custom = listOf(
            RoundEntity(id = 10, name = "Opening", position = 0),
            RoundEntity(id = 11, name = "Sprint", position = 1),
        )
        val teams = listOf(team(1, "Alpha", 2))
        val scores = listOf(ScoreEntity(1, 10, 5), ScoreEntity(1, 11, 7))

        val csv = ResultsExporter.buildCsv(stateOf(custom, teams, scores))
        val lines = csv.trim().split("\n")

        assertEquals("Team Name,Players,Amount,Opening,Opening Rank,Sprint,Sprint Rank,Subtotal,Total,Final Position", lines[3])
        assertEquals("Alpha,2,4.00,5,1,7,1,5,12,1", lines[4])
    }

    @Test
    fun `a single round omits the subtotal column`() {
        val single = listOf(RoundEntity(id = 1, name = "Final", position = 0))
        val teams = listOf(team(1, "Alpha", 2))
        val scores = listOf(ScoreEntity(1, 1, 9))

        val csv = ResultsExporter.buildCsv(stateOf(single, teams, scores))
        val lines = csv.trim().split("\n")

        assertEquals("Team Name,Players,Amount,Final,Final Rank,Total,Final Position", lines[3])
        assertEquals("Alpha,2,4.00,9,1,9,1", lines[4])
    }

    @Test
    fun `team names containing commas are quoted`() {
        val teams = listOf(team(1, "Smith, Jones & Co", 1))
        val scores = listOf(ScoreEntity(1, 1, 5))

        val csv = ResultsExporter.buildCsv(stateOf(rounds, teams, scores))

        assertTrue(csv.contains("\"Smith, Jones & Co\""))
    }
}
