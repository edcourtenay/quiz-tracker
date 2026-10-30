package uk.co.edcourtenay.quiztracker.domain

import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.ScoreEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoringTest {

    private var nextTeamId = 1L

    /** The three rounds the app used to hard-code. */
    private val rounds = listOf(
        RoundEntity(id = 1, name = "Rounds 1-4", position = 0),
        RoundEntity(id = 2, name = "Rounds 5-6", position = 1),
        RoundEntity(id = 3, name = "Wipeout", position = 2),
    )

    private fun team(name: String, players: Int? = null) = TeamEntity(
        id = nextTeamId++,
        name = name,
        players = players,
        createdAt = nextTeamId,
    )

    /** Builds score rows for one team, positional against [rounds]; null = not entered. */
    private fun scores(teamId: Long, vararg values: Int?): List<ScoreEntity> =
        values.mapIndexedNotNull { index, value ->
            value?.let { ScoreEntity(teamId = teamId, roundId = rounds[index].id, value = it) }
        }

    private fun scoreOf(teams: List<TeamEntity>, scores: List<ScoreEntity>) =
        Scoring.scoreTeams(teams, rounds, scores, feePerPlayer = 2.0).associateBy { it.name }

    @Test
    fun `reproduces the Rounds 1-4 and Rounds 5-6 ranks from the workbook`() {
        val hipHip = team("Hip Hip")
        val firstPlace = team("And In First Place...")
        val groovy = team("Groovy Gang")
        val kinder = team("Kinder Suprise")
        val hemsworth = team("Quiz Hemsworth")
        val bingo = team("We Thought This Was Bingo")
        val edward = team("Edward Quizzerhands")
        val tsAndCs = team("T's & C's")
        val dayJob = team("Don't Quiz Your Day Job")
        val universally = team("Universally Challenged")

        val teams = listOf(hipHip, firstPlace, groovy, kinder, hemsworth, bingo, edward, tsAndCs, dayJob, universally)
        val scores = listOf(hipHip, firstPlace, groovy, kinder, hemsworth, bingo, edward, tsAndCs, dayJob, universally)
            .flatMap { team ->
                when (team.name) {
                    "Hip Hip" -> scores(team.id, 17, 13)
                    "And In First Place..." -> scores(team.id, 19, 8)
                    "Groovy Gang" -> scores(team.id, 16, 9)
                    "Kinder Suprise" -> scores(team.id, 20, 14)
                    "Quiz Hemsworth" -> scores(team.id, 21, 12)
                    "We Thought This Was Bingo" -> scores(team.id, 19, 13)
                    "Edward Quizzerhands" -> scores(team.id, 24)
                    "T's & C's" -> scores(team.id, 18)
                    "Don't Quiz Your Day Job" -> scores(team.id, 16)
                    else -> scores(team.id, 13)
                }
            }

        val byName = scoreOf(teams, scores)

        // Sheet column E — rank by the Rounds 1-4 score.
        assertEquals(1, byName.getValue("Edward Quizzerhands").rank(1))
        assertEquals(2, byName.getValue("Quiz Hemsworth").rank(1))
        assertEquals(3, byName.getValue("Kinder Suprise").rank(1))
        assertEquals(4, byName.getValue("And In First Place...").rank(1))
        assertEquals(4, byName.getValue("We Thought This Was Bingo").rank(1))
        assertEquals(6, byName.getValue("T's & C's").rank(1))
        assertEquals(7, byName.getValue("Hip Hip").rank(1))
        assertEquals(8, byName.getValue("Groovy Gang").rank(1))
        assertEquals(8, byName.getValue("Don't Quiz Your Day Job").rank(1))
        assertEquals(10, byName.getValue("Universally Challenged").rank(1))

        // Sheet column H — rank by the running total after Rounds 5-6.
        assertEquals(1, byName.getValue("Kinder Suprise").runningRank(2))
        assertEquals(2, byName.getValue("Quiz Hemsworth").runningRank(2))
        assertEquals(3, byName.getValue("We Thought This Was Bingo").runningRank(2))
        assertEquals(4, byName.getValue("Hip Hip").runningRank(2))
        assertEquals(5, byName.getValue("And In First Place...").runningRank(2))
        assertEquals(6, byName.getValue("Groovy Gang").runningRank(2))
        assertNull(byName.getValue("Edward Quizzerhands").runningRank(2))

        // Nobody recorded a Wipeout, so the final position is unranked — as in the sheet.
        assertNull(byName.getValue("Hip Hip").rankFinal)
    }

    @Test
    fun `subtotal is the running total after the penultimate round`() {
        val onlyFirst = team("Only rounds 1-4")
        val both = team("Plus rounds 5-6")
        val complete = team("Plus wipeout")

        val teams = listOf(onlyFirst, both, complete)
        val scores = scores(onlyFirst.id, 20) +
            scores(both.id, 20, 10) +
            scores(complete.id, 20, 10, 5)

        val byName = scoreOf(teams, scores)

        assertNull(byName.getValue("Only rounds 1-4").subtotal)
        assertNull(byName.getValue("Only rounds 1-4").total)

        assertEquals(30, byName.getValue("Plus rounds 5-6").subtotal)
        assertNull(byName.getValue("Plus rounds 5-6").total)

        assertEquals(30, byName.getValue("Plus wipeout").subtotal)
        assertEquals(35, byName.getValue("Plus wipeout").total)
    }

    @Test
    fun `a wipeout of zero is kept as an entered score`() {
        val zeroWipeout = team("Zero wipeout")
        val scores = scores(zeroWipeout.id, 20, 10, 0)

        val score = Scoring.scoreTeams(listOf(zeroWipeout), rounds, scores, 2.0).single()

        assertEquals(30, score.subtotal)
        assertEquals(30, score.total)
        assertEquals(0, score.value(3))
    }

    @Test
    fun `amount is the per-player fee multiplied by the headcount`() {
        val fourPlayers = team("Four players", players = 4)
        val noPlayers = team("No players recorded")

        val byName = Scoring
            .scoreTeams(listOf(fourPlayers, noPlayers), rounds, emptyList(), feePerPlayer = 2.5)
            .associateBy { it.name }

        assertEquals(10.0, byName.getValue("Four players").amount!!, 0.0001)
        assertNull(byName.getValue("No players recorded").amount)
    }

    @Test
    fun `final position ranks on the total`() {
        val first = team("First")
        val second = team("Second")
        val third = team("Third")

        val teams = listOf(first, second, third)
        val scores = scores(first.id, 20, 10, 10) +
            scores(second.id, 20, 10, 5) +
            scores(third.id, 20, 10, 0)

        val byName = scoreOf(teams, scores)

        assertEquals(1, byName.getValue("First").rankFinal)
        assertEquals(2, byName.getValue("Second").rankFinal)
        assertEquals(3, byName.getValue("Third").rankFinal)
    }

    @Test
    fun `a running total stays blank until every round up to that point is entered`() {
        val gap = team("Missed round one")
        val scores = scores(gap.id, null, 10, 5)

        val score = Scoring.scoreTeams(listOf(gap), rounds, scores, 2.0).single()

        assertNull(score.runningTotal(2))
        assertNull(score.runningTotal(3))
        assertNull(score.total)
        // The round that *was* played is still recorded and ranked.
        assertEquals(10, score.value(2))
        assertEquals(1, score.rank(2))
    }

    @Test
    fun `handles a single configured round`() {
        val oneRound = listOf(RoundEntity(id = 7, name = "Final", position = 0))
        val teamA = team("A")
        val teamB = team("B")
        val scores = listOf(
            ScoreEntity(teamId = teamA.id, roundId = 7, value = 12),
            ScoreEntity(teamId = teamB.id, roundId = 7, value = 15),
        )

        val byName = Scoring.scoreTeams(listOf(teamA, teamB), oneRound, scores, 2.0).associateBy { it.name }

        // No subtotal with a single round, but the total is that round.
        assertNull(byName.getValue("A").subtotal)
        assertEquals(12, byName.getValue("A").total)
        assertEquals(2, byName.getValue("A").rankFinal)
        assertEquals(1, byName.getValue("B").rankFinal)
    }

    @Test
    fun `handles an arbitrary number of rounds`() {
        val many = (1L..5L).map { RoundEntity(id = it, name = "Round $it", position = (it - 1).toInt()) }
        val teamA = team("A")
        val scores = listOf(1L, 2L, 3L, 4L, 5L).mapIndexed { index, roundId ->
            ScoreEntity(teamId = teamA.id, roundId = roundId, value = index + 1)
        }

        val score = Scoring.scoreTeams(listOf(teamA), many, scores, 2.0).single()

        assertEquals(1 + 2 + 3 + 4, score.subtotal)
        assertEquals(1 + 2 + 3 + 4 + 5, score.total)
        assertEquals(1 + 2 + 3, score.runningTotal(3))
    }
}
