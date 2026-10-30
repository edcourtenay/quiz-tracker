package uk.co.edcourtenay.quiztracker.domain

import uk.co.edcourtenay.quiztracker.data.PrizeEntity
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.ScoreEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrizesTest {

    private var nextTeamId = 1L

    private val rounds = listOf(RoundEntity(id = 1, name = "Only round", position = 0))

    private val prizes = listOf(
        PrizeEntity(id = 1, name = "1st", value = 50.0, position = 0),
        PrizeEntity(id = 2, name = "2nd", value = 30.0, position = 1),
        PrizeEntity(id = 3, name = "3rd", value = 10.0, position = 2),
    )

    private fun team(name: String) = TeamEntity(id = nextTeamId++, name = name, createdAt = nextTeamId)

    /** Scores every team in a single round, so the score *is* the total. */
    private fun standings(teams: List<TeamEntity>, totals: List<Int>, tieBreaks: Map<Int, TieBreakState> = emptyMap()) =
        Prizes.standings(
            scores = Scoring.scoreTeams(
                teams,
                rounds,
                teams.mapIndexed { index, team -> ScoreEntity(team.id, 1, totals[index]) },
                feePerPlayer = 2.0,
            ),
            prizes = prizes,
            tieBreaks = tieBreaks,
        )

    @Test
    fun `awards prizes to the top scoring teams`() {
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"), team("Delta"))

        val result = standings(teams, listOf(40, 35, 30, 25))

        assertTrue(result.complete)
        assertEquals(
            listOf("Alpha", "Bravo", "Charlie"),
            result.awards.filterIsInstance<PrizeAward.Awarded>().map { it.team.name },
        )
        assertEquals("1st", result.prizeByTeam.getValue(teams[0].id).name)
        assertTrue(result.contested.isEmpty())
        assertEquals("Delta", result.order.last().name)
    }

    @Test
    fun `leaves prizes unclaimed when there are fewer teams than prizes`() {
        val teams = listOf(team("Alpha"), team("Bravo"))

        val result = standings(teams, listOf(40, 35))

        assertEquals(2, result.awards.filterIsInstance<PrizeAward.Awarded>().size)
        assertEquals(1, result.awards.filterIsInstance<PrizeAward.Unclaimed>().size)
    }

    @Test
    fun `flags the tied position as contested while the tie-break is unresolved`() {
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"))

        // 40, 38, 38 -> positions 2 and 3 are tied.
        val result = standings(teams, listOf(40, 38, 38))

        assertTrue(result.complete)
        assertEquals(listOf(2), result.contested.map { it.rank })
        assertEquals(listOf("Bravo", "Charlie"), result.contested.single().teams.map { it.name })

        // First prize is settled; the other two are waiting on the tie-break.
        assertEquals(1, result.awards.filterIsInstance<PrizeAward.Awarded>().size)
        assertEquals(2, result.awards.filterIsInstance<PrizeAward.Contested>().size)
        assertTrue(result.prizeByTeam.keys.none { it == teams[1].id || it == teams[2].id })
    }

    @Test
    fun `a resolved tie-break decides who gets which prize`() {
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"))

        // Bravo guesses 1050, Charlie 1100; the answer is 1066, so Bravo is closer.
        val tieBreak = mapOf(
            2 to TieBreakState(
                question = "In which year did the Hundred Years War end?",
                correctAnswer = 1066.0,
                answers = mapOf(teams[1].id to 1050.0, teams[2].id to 1100.0),
            ),
        )

        val result = standings(teams, listOf(40, 38, 38), tieBreak)

        assertTrue(result.contested.isNotEmpty())
        assertEquals("2nd", result.prizeByTeam.getValue(teams[1].id).name)
        assertEquals("3rd", result.prizeByTeam.getValue(teams[2].id).name)
        assertTrue(result.awards.all { it !is PrizeAward.Contested })
        assertEquals(listOf("Alpha", "Bravo", "Charlie"), result.order.map { it.name })
    }

    @Test
    fun `an exact match beats a nearer-but-wrong guess only when it is closest`() {
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"))
        val tieBreak = mapOf(
            2 to TieBreakState(
                correctAnswer = 100.0,
                answers = mapOf(teams[1].id to 100.0, teams[2].id to 110.0),
            ),
        )

        val result = standings(teams, listOf(40, 38, 38), tieBreak)

        assertEquals("2nd", result.prizeByTeam.getValue(teams[1].id).name)
    }

    @Test
    fun `equidistant guesses are decided in favour of the lower one`() {
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"))
        val tieBreak = mapOf(
            2 to TieBreakState(
                correctAnswer = 100.0,
                answers = mapOf(teams[1].id to 90.0, teams[2].id to 110.0),
            ),
        )

        val result = standings(teams, listOf(40, 38, 38), tieBreak)

        assertEquals("2nd", result.prizeByTeam.getValue(teams[1].id).name)
        assertEquals("3rd", result.prizeByTeam.getValue(teams[2].id).name)
    }

    @Test
    fun `identical guesses cannot be separated`() {
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"))
        val tieBreak = mapOf(
            2 to TieBreakState(
                correctAnswer = 100.0,
                answers = mapOf(teams[1].id to 42.0, teams[2].id to 42.0),
            ),
        )

        val result = standings(teams, listOf(40, 38, 38), tieBreak)

        assertEquals(2, result.awards.filterIsInstance<PrizeAward.Contested>().size)
    }

    @Test
    fun `a tie-break ranks the whole group when it is larger than the slots left`() {
        // 40, 38, 38, 38 with three prizes: three teams contest positions 2 and 3,
        // so the furthest guess misses out entirely.
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"), team("Delta"))
        val tieBreak = mapOf(
            2 to TieBreakState(
                correctAnswer = 100.0,
                answers = mapOf(teams[1].id to 90.0, teams[2].id to 105.0, teams[3].id to 130.0),
            ),
        )

        val result = standings(teams, listOf(40, 38, 38, 38), tieBreak)

        // Distances: Charlie 5, Bravo 10, Delta 30.
        assertEquals("2nd", result.prizeByTeam.getValue(teams[2].id).name) // Charlie
        assertEquals("3rd", result.prizeByTeam.getValue(teams[1].id).name) // Bravo
        assertNull(result.prizeByTeam[teams[3].id])                        // Delta misses out
    }

    @Test
    fun `a tie outside the prize positions is not contested`() {
        val teams = listOf(team("Alpha"), team("Bravo"), team("Charlie"), team("Delta"), team("Echo"))

        // 4th and 5th are tied, but there are only three prizes.
        val result = standings(teams, listOf(40, 35, 30, 20, 20))

        assertTrue(result.contested.isEmpty())
        assertEquals(3, result.awards.filterIsInstance<PrizeAward.Awarded>().size)
    }

    @Test
    fun `standings are incomplete until every team has a total`() {
        val scored = team("Scored")
        val partial = team("Partial")
        val scores = Scoring.scoreTeams(
            listOf(scored, partial),
            rounds,
            listOf(ScoreEntity(scored.id, 1, 10)),
            feePerPlayer = 2.0,
        )

        val result = Prizes.standings(scores, prizes, emptyMap())

        assertFalse(result.complete)
        assertEquals(listOf("Scored"), result.awards.filterIsInstance<PrizeAward.Awarded>().map { it.team.name })
        assertEquals(2, result.awards.filterIsInstance<PrizeAward.Unclaimed>().size)
    }
}
