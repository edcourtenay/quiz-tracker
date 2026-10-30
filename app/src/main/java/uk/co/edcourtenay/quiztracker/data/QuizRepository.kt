package uk.co.edcourtenay.quiztracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single entry point for reading and writing quiz data.
 * Keeps the ViewModel free of Room details.
 */
class QuizRepository(private val dao: QuizDao) {

    /** Teams in entry order. */
    val teams: Flow<List<TeamEntity>> = dao.observeTeams()

    /** Configured rounds, in play order. */
    val rounds: Flow<List<RoundEntity>> = dao.observeRounds()

    /** Every entered score. */
    val scores: Flow<List<ScoreEntity>> = dao.observeScores()

    /** Configured prizes, in award order. */
    val prizes: Flow<List<PrizeEntity>> = dao.observePrizes()

    /** Tie-break questions and correct answers, keyed by contested final position. */
    val tieBreaks: Flow<List<TieBreakEntity>> = dao.observeTieBreaks()

    /** Tie-break guesses, one per tied team. */
    val tieBreakAnswers: Flow<List<TieBreakAnswerEntity>> = dao.observeTieBreakAnswers()

    /** Config, falling back to defaults until the user changes something. */
    val config: Flow<ConfigEntity> = dao.observeConfig().map { it ?: ConfigEntity.Default }

    // ---------------------------------------------------------------------
    // Teams
    // ---------------------------------------------------------------------

    suspend fun addTeam(name: String = "", players: Int? = null): Long =
        dao.insertTeam(TeamEntity(name = name, players = players))

    suspend fun updateTeam(team: TeamEntity) = dao.updateTeam(team)

    suspend fun deleteTeam(team: TeamEntity) = dao.deleteTeam(team)

    /** Removes every team and their scores. Rounds are left alone. */
    suspend fun deleteAllTeams() = dao.deleteAllTeams()

    // ---------------------------------------------------------------------
    // Rounds
    // ---------------------------------------------------------------------

    /** Appends a new round at the end of the running order. */
    suspend fun addRound(name: String): Long {
        val position = dao.getRounds().size
        return dao.insertRound(RoundEntity(name = name, position = position))
    }

    suspend fun renameRound(round: RoundEntity, name: String) =
        dao.updateRound(round.copy(name = name))

    /**
     * Moves [round] one place earlier or later, rewriting the positions of the
     * affected rounds so the running order stays 0..n-1.
     */
    suspend fun moveRound(round: RoundEntity, offset: Int) {
        val ordered = dao.getRounds()
        val from = ordered.indexOfFirst { it.id == round.id }
        val to = from + offset
        if (from < 0 || to !in ordered.indices) return

        val reordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
        reordered.forEachIndexed { position, item ->
            if (item.position != position) dao.updateRoundPosition(item.id, position)
        }
    }

    suspend fun deleteRound(round: RoundEntity) {
        dao.deleteRound(round)
        // Close the gap left in the running order.
        dao.getRounds().forEachIndexed { position, item ->
            if (item.position != position) dao.updateRoundPosition(item.id, position)
        }
    }

    /** Number of teams that have a score recorded for a round. */
    suspend fun countScoresForRound(roundId: Long): Int = dao.countScoresForRound(roundId)

    // ---------------------------------------------------------------------
    // Scores
    // ---------------------------------------------------------------------

    /** Records a score, or clears it when [value] is null ("not entered"). */
    suspend fun setScore(teamId: Long, roundId: Long, value: Int?) {
        if (value == null) {
            dao.deleteScore(teamId, roundId)
        } else {
            dao.upsertScore(ScoreEntity(teamId = teamId, roundId = roundId, value = value))
        }
        // The ties — and therefore the tie-breaks — may have moved.
        clearTieBreaks()
    }

    /** Clears every entered score. Teams and rounds are left alone. */
    suspend fun clearAllScores() {
        dao.clearAllScores()
        clearTieBreaks()
    }

    // ---------------------------------------------------------------------
    // Prizes
    // ---------------------------------------------------------------------

    /** Appends a new prize after the last one. */
    suspend fun addPrize(name: String): Long {
        val position = dao.getPrizes().size
        return dao.insertPrize(PrizeEntity(name = name, position = position))
    }

    suspend fun updatePrize(prize: PrizeEntity) = dao.updatePrize(prize)

    /**
     * Moves [prize] one place earlier or later, rewriting the positions of the
     * affected prizes so the award order stays 0..n-1.
     */
    suspend fun movePrize(prize: PrizeEntity, offset: Int) {
        val ordered = dao.getPrizes()
        val from = ordered.indexOfFirst { it.id == prize.id }
        val to = from + offset
        if (from < 0 || to !in ordered.indices) return

        val reordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
        reordered.forEachIndexed { position, item ->
            if (item.position != position) dao.updatePrizePosition(item.id, position)
        }
    }

    suspend fun deletePrize(prize: PrizeEntity) {
        dao.deletePrize(prize)
        dao.getPrizes().forEachIndexed { position, item ->
            if (item.position != position) dao.updatePrizePosition(item.id, position)
        }
    }

    // ---------------------------------------------------------------------
    // Tie-breaks
    // ---------------------------------------------------------------------

    suspend fun setTieBreakQuestion(rank: Int, question: String) {
        val existing = dao.getTieBreak(rank)
        dao.upsertTieBreak(TieBreakEntity(rank = rank, question = question, correctAnswer = existing?.correctAnswer))
    }

    suspend fun setTieBreakAnswer(rank: Int, teamId: Long, answer: Double?) {
        if (answer == null) {
            dao.deleteTieBreakAnswer(rank, teamId)
        } else {
            dao.upsertTieBreakAnswer(TieBreakAnswerEntity(rank, teamId, answer))
        }
    }

    suspend fun setTieBreakCorrectAnswer(rank: Int, correctAnswer: Double?) {
        val existing = dao.getTieBreak(rank)
        dao.upsertTieBreak(
            TieBreakEntity(rank = rank, question = existing?.question.orEmpty(), correctAnswer = correctAnswer),
        )
    }

    /** Drops every tie-break; called whenever a score changes. */
    suspend fun clearTieBreaks() {
        dao.clearTieBreakAnswers()
        dao.clearTieBreaks()
    }

    // ---------------------------------------------------------------------
    // Config
    // ---------------------------------------------------------------------

    suspend fun saveConfig(config: ConfigEntity) = dao.upsertConfig(config)
}
