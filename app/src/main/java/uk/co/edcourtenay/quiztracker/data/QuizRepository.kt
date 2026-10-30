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

    suspend fun setRoundHighlightZero(round: RoundEntity, highlightZero: Boolean) =
        dao.updateRound(round.copy(highlightZero = highlightZero))

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
    }

    /** Clears every entered score. Teams and rounds are left alone. */
    suspend fun clearAllScores() = dao.clearAllScores()

    // ---------------------------------------------------------------------
    // Config
    // ---------------------------------------------------------------------

    suspend fun saveConfig(config: ConfigEntity) = dao.upsertConfig(config)
}
