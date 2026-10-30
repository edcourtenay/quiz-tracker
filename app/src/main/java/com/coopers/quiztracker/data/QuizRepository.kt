package com.coopers.quiztracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single entry point for reading and writing quiz data.
 * Keeps the ViewModel free of Room details.
 */
class QuizRepository(private val dao: QuizDao) {

    /** Teams in entry order. */
    val teams: Flow<List<TeamEntity>> = dao.observeTeams()

    /** Config, falling back to defaults until the user changes something. */
    val config: Flow<ConfigEntity> = dao.observeConfig().map { it ?: ConfigEntity.Default }

    suspend fun addTeam(name: String = "", players: Int? = null): Long =
        dao.insertTeam(TeamEntity(name = name, players = players))

    suspend fun updateTeam(team: TeamEntity) = dao.updateTeam(team)

    suspend fun deleteTeam(team: TeamEntity) = dao.deleteTeam(team)

    suspend fun deleteAllTeams() = dao.deleteAllTeams()

    suspend fun clearAllScores() = dao.clearAllScores()

    suspend fun saveConfig(config: ConfigEntity) = dao.upsertConfig(config)
}
