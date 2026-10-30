package uk.co.edcourtenay.quiztracker.domain

import uk.co.edcourtenay.quiztracker.data.TeamEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoringTest {

    private var nextId = 1L

    private fun team(
        name: String,
        players: Int? = null,
        rounds1to4: Int? = null,
        rounds5to6: Int? = null,
        wipeout: Int? = null,
    ) = TeamEntity(
        id = nextId++,
        name = name,
        players = players,
        rounds1to4 = rounds1to4,
        rounds5to6 = rounds5to6,
        wipeout = wipeout,
    )

    @Test
    fun `reproduces the Rounds 1-4 and Rounds 5-6 ranks from the workbook`() {
        val teams = listOf(
            team("Hip Hip", rounds1to4 = 17, rounds5to6 = 13),
            team("And In First Place...", rounds1to4 = 19, rounds5to6 = 8),
            team("Groovy Gang", rounds1to4 = 16, rounds5to6 = 9),
            team("Kinder Suprise", rounds1to4 = 20, rounds5to6 = 14),
            team("Quiz Hemsworth", rounds1to4 = 21, rounds5to6 = 12),
            team("We Thought This Was Bingo", rounds1to4 = 19, rounds5to6 = 13),
            team("Edward Quizzerhands", rounds1to4 = 24),
            team("T's & C's", rounds1to4 = 18),
            team("Don't Quiz Your Day Job", rounds1to4 = 16),
            team("Universally Challenged", rounds1to4 = 13),
        )

        val byName = Scoring.scoreTeams(teams, feePerPlayer = 2.0).associateBy { it.name }

        // Sheet column E (Rounds 1-4 Rank)
        assertEquals(1, byName.getValue("Edward Quizzerhands").rankRounds1to4)
        assertEquals(2, byName.getValue("Quiz Hemsworth").rankRounds1to4)
        assertEquals(3, byName.getValue("Kinder Suprise").rankRounds1to4)
        assertEquals(4, byName.getValue("And In First Place...").rankRounds1to4)
        assertEquals(4, byName.getValue("We Thought This Was Bingo").rankRounds1to4)
        assertEquals(6, byName.getValue("T's & C's").rankRounds1to4)
        assertEquals(7, byName.getValue("Hip Hip").rankRounds1to4)
        assertEquals(8, byName.getValue("Groovy Gang").rankRounds1to4)
        assertEquals(8, byName.getValue("Don't Quiz Your Day Job").rankRounds1to4)
        assertEquals(10, byName.getValue("Universally Challenged").rankRounds1to4)

        // Sheet column H (Rounds 5-6 Rank, ranked on the Subtotal)
        assertEquals(1, byName.getValue("Kinder Suprise").rankRounds5to6)
        assertEquals(2, byName.getValue("Quiz Hemsworth").rankRounds5to6)
        assertEquals(3, byName.getValue("We Thought This Was Bingo").rankRounds5to6)
        assertEquals(4, byName.getValue("Hip Hip").rankRounds5to6)
        assertEquals(5, byName.getValue("And In First Place...").rankRounds5to6)
        assertEquals(6, byName.getValue("Groovy Gang").rankRounds5to6)
        assertNull(byName.getValue("Edward Quizzerhands").rankRounds5to6)
    }

    @Test
    fun `subtotal and total stay blank until their round is entered`() {
        val teams = listOf(
            team("Only rounds 1-4", rounds1to4 = 20),
            team("Plus rounds 5-6", rounds1to4 = 20, rounds5to6 = 10),
            team("Plus wipeout", rounds1to4 = 20, rounds5to6 = 10, wipeout = 5),
        )

        val byName = Scoring.scoreTeams(teams, feePerPlayer = 2.0).associateBy { it.name }

        assertNull(byName.getValue("Only rounds 1-4").subtotal)
        assertNull(byName.getValue("Only rounds 1-4").total)

        assertEquals(30, byName.getValue("Plus rounds 5-6").subtotal)
        assertNull(byName.getValue("Plus rounds 5-6").total)

        assertEquals(30, byName.getValue("Plus wipeout").subtotal)
        assertEquals(35, byName.getValue("Plus wipeout").total)
    }

    @Test
    fun `a wipeout of zero is kept as an entered score`() {
        val teams = listOf(
            team("Zero wipeout", rounds1to4 = 20, rounds5to6 = 10, wipeout = 0),
        )

        val score = Scoring.scoreTeams(teams, feePerPlayer = 2.0).single()

        assertEquals(30, score.subtotal)
        assertEquals(30, score.total)
        assertEquals(0, score.team.wipeout)
    }

    @Test
    fun `amount is the per-player fee multiplied by the headcount`() {
        val teams = listOf(
            team("Four players", players = 4),
            team("No players recorded"),
        )

        val byName = Scoring.scoreTeams(teams, feePerPlayer = 2.5).associateBy { it.name }

        assertEquals(10.0, byName.getValue("Four players").amount!!, 0.0001)
        assertNull(byName.getValue("No players recorded").amount)
    }

    @Test
    fun `final position ranks on the total`() {
        val teams = listOf(
            team("First", rounds1to4 = 20, rounds5to6 = 10, wipeout = 10), // 40
            team("Second", rounds1to4 = 20, rounds5to6 = 10, wipeout = 5), // 35
            team("Third", rounds1to4 = 20, rounds5to6 = 10, wipeout = 0), // 30
        )

        val byName = Scoring.scoreTeams(teams, feePerPlayer = 2.0).associateBy { it.name }

        assertEquals(1, byName.getValue("First").rankFinal)
        assertEquals(2, byName.getValue("Second").rankFinal)
        assertEquals(3, byName.getValue("Third").rankFinal)
    }
}
