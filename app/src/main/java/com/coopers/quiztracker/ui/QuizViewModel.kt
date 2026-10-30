package com.coopers.quiztracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.coopers.quiztracker.QuizTrackerApp
import com.coopers.quiztracker.data.ConfigEntity
import com.coopers.quiztracker.data.QuizRepository
import com.coopers.quiztracker.data.TeamEntity
import com.coopers.quiztracker.domain.Scoring
import com.coopers.quiztracker.domain.TeamScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuizUiState(
    val teams: List<TeamEntity> = emptyList(),
    val scores: List<TeamScore> = emptyList(),
    val config: ConfigEntity = ConfigEntity.Default,
) {
    val teamCount: Int get() = teams.size
    val totalPlayers: Int get() = teams.sumOf { it.players ?: 0 }
    val totalFunds: Double get() = scores.sumOf { it.amount ?: 0.0 }
}

class QuizViewModel(private val repository: QuizRepository) : ViewModel() {

    val uiState: StateFlow<QuizUiState> =
        combine(repository.teams, repository.config) { teams, config ->
            QuizUiState(
                teams = teams,
                scores = Scoring.scoreTeams(teams, config.feePerPlayer),
                config = config,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QuizUiState(),
        )

    private val _focusTeamId = MutableStateFlow<Long?>(null)

    /**
     * Id of a team whose name field should be focused and selected for editing.
     * Null when there is nothing pending.
     */
    val focusTeamId: StateFlow<Long?> = _focusTeamId.asStateFlow()

    fun addTeam(name: String = "", players: Int? = null) = viewModelScope.launch {
        // Remember the new row so the UI can focus its name field for typing.
        _focusTeamId.value = repository.addTeam(name = name, players = players)
    }

    /** Called by the UI once the pending team has been focused. */
    fun onFocusHandled() {
        _focusTeamId.value = null
    }

    fun updateTeam(team: TeamEntity) = viewModelScope.launch {
        repository.updateTeam(team)
    }

    fun deleteTeam(team: TeamEntity) = viewModelScope.launch {
        repository.deleteTeam(team)
    }

    fun deleteAllTeams() = viewModelScope.launch {
        repository.deleteAllTeams()
    }

    fun clearAllScores() = viewModelScope.launch {
        repository.clearAllScores()
    }

    fun setFeePerPlayer(fee: Double) = viewModelScope.launch {
        repository.saveConfig(uiState.value.config.copy(feePerPlayer = fee))
    }

    fun setQuizName(name: String) = viewModelScope.launch {
        repository.saveConfig(uiState.value.config.copy(quizName = name))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as QuizTrackerApp
                QuizViewModel(app.repository)
            }
        }
    }
}
