package uk.co.edcourtenay.quiztracker.export

import uk.co.edcourtenay.quiztracker.data.ConfigEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import uk.co.edcourtenay.quiztracker.domain.Scoring
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultsExporterTest {

    private fun stateOf(vararg teams: TeamEntity): QuizUiState {
        val list = teams.toList()
        return QuizUiState(
            teams = list,
            scores = Scoring.scoreTeams(list, feePerPlayer = 2.0),
            config = ConfigEntity(quizName = "Test Quiz", feePerPlayer = 2.0),
        )
    }

    @Test
    fun `csv has the sheet columns and is ordered by final position`() {
        val state = stateOf(
            TeamEntity(id = 1, name = "Alpha", players = 4, rounds1to4 = 17, rounds5to6 = 13, wipeout = 0),
            TeamEntity(id = 2, name = "Bravo", players = 3, rounds1to4 = 24, rounds5to6 = 5, wipeout = 10),
            TeamEntity(id = 3, name = "Charlie", players = 2, rounds1to4 = 20, rounds5to6 = 14, wipeout = 3),
        )

        val csv = ResultsExporter.buildCsv(state)
        val lines = csv.trim().split("\n")

        assertEquals(
            "Team Name,Players,Amount,Rounds 1-4,Rounds 1-4 Rank,Rounds 5-6,Subtotal,Rounds 5-6 Rank,Wipeout,Total,Final Position",
            lines[3],
        )
        assertTrue("first data row is the winner", lines[4].startsWith("Bravo,3,6.00,24,1,5,29,3,10,39,1"))
        assertTrue(lines[5].startsWith("Charlie,2,4.00,20,2,14,34,1,3,37,2"))
        assertTrue(lines[6].startsWith("Alpha,4,8.00,17,3,13,30,2,0,30,3"))
        assertTrue(csv.contains("Total Players,9"))
        assertTrue(csv.contains("Total Funds,18.00"))
    }

    @Test
    fun `team names containing commas are quoted`() {
        val state = stateOf(
            TeamEntity(id = 1, name = "Smith, Jones & Co", players = 1, rounds1to4 = 5),
        )

        val csv = ResultsExporter.buildCsv(state)

        assertTrue(csv.contains("\"Smith, Jones & Co\""))
    }
}
