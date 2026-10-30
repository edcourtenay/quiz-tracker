package uk.co.edcourtenay.quiztracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uk.co.edcourtenay.quiztracker.QuizTrackerApp
import uk.co.edcourtenay.quiztracker.data.ConfigEntity
import uk.co.edcourtenay.quiztracker.data.QuizRepository
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import uk.co.edcourtenay.quiztracker.domain.Scoring
import uk.co.edcourtenay.quiztracker.domain.TeamScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuizUiState(
    val teams: List<TeamEntity> = emptyList(),
    val rounds: List<RoundEntity> = emptyList(),
    val scores: List<TeamScore> = emptyList(),
    val config: ConfigEntity = ConfigEntity.Default,
) {
    val teamCount: Int get() = teams.size
    val totalPlayers: Int get() = teams.sumOf { it.players ?: 0 }
    val totalFunds: Double get() = scores.sumOf { it.amount ?: 0.0 }
}

/** A one-shot request to reveal a team on the Scores screen. */
data class RevealRequest(
    val teamId: Long,
    /** Which round's field to focus, or null when the board has no editable value. */
    val roundId: Long?,
)

class QuizViewModel(private val repository: QuizRepository) : ViewModel() {

    val uiState: StateFlow<QuizUiState> =
        combine(
            repository.teams,
            repository.rounds,
            repository.scores,
            repository.config,
        ) { teams, rounds, scoreRows, config ->
            QuizUiState(
                teams = teams,
                rounds = rounds,
                scores = Scoring.scoreTeams(teams, rounds, scoreRows, config.feePerPlayer),
                config = config,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QuizUiState(),
        )

    // ---------------------------------------------------------------------
    // Teams
    // ---------------------------------------------------------------------

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

    /** Removes every team and their scores. The configured rounds are kept. */
    fun deleteAllTeams() = viewModelScope.launch {
        repository.deleteAllTeams()
    }

    // ---------------------------------------------------------------------
    // Reveal (from a leaderboard row)
    // ---------------------------------------------------------------------

    private val _revealRequest = MutableStateFlow<RevealRequest?>(null)

    /**
     * A pending request to show a team on the Scores screen, e.g. after the user
     * tapped that team on a leaderboard. Null when there is nothing pending.
     */
    val revealRequest: StateFlow<RevealRequest?> = _revealRequest.asStateFlow()

    /**
     * Ask the Scores screen to bring [teamId] into view, focusing [roundId]'s
     * field for input when the originating board has an editable score.
     */
    fun revealTeam(teamId: Long, roundId: Long? = null) {
        _revealRequest.value = RevealRequest(teamId = teamId, roundId = roundId)
    }

    /** Called by the UI once the team has been revealed. */
    fun onRevealHandled() {
        _revealRequest.value = null
    }

    // ---------------------------------------------------------------------
    // Rounds
    // ---------------------------------------------------------------------

    fun addRound() = viewModelScope.launch {
        val position = uiState.value.rounds.size + 1
        repository.addRound(name = "Round $position")
    }

    fun renameRound(round: RoundEntity, name: String) = viewModelScope.launch {
        repository.renameRound(round, name)
    }

    fun setRoundHighlightZero(round: RoundEntity, highlightZero: Boolean) = viewModelScope.launch {
        repository.setRoundHighlightZero(round, highlightZero)
    }

    /** Moves a round one place earlier (offset -1) or later (offset +1). */
    fun moveRound(round: RoundEntity, offset: Int) = viewModelScope.launch {
        repository.moveRound(round, offset)
    }

    fun deleteRound(round: RoundEntity) = viewModelScope.launch {
        repository.deleteRound(round)
    }

    /** How many teams have a score recorded for a round. */
    suspend fun countScoresForRound(roundId: Long): Int = repository.countScoresForRound(roundId)

    // ---------------------------------------------------------------------
    // Scores
    // ---------------------------------------------------------------------

    fun setScore(teamId: Long, roundId: Long, value: Int?) = viewModelScope.launch {
        repository.setScore(teamId, roundId, value)
    }

    /** Clears every entered score. Teams and rounds are kept. */
    fun clearAllScores() = viewModelScope.launch {
        repository.clearAllScores()
    }

    // ---------------------------------------------------------------------
    // Config
    // ---------------------------------------------------------------------

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
