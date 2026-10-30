package uk.co.edcourtenay.quiztracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uk.co.edcourtenay.quiztracker.QuizTrackerApp
import uk.co.edcourtenay.quiztracker.data.ConfigEntity
import uk.co.edcourtenay.quiztracker.data.PrizeEntity
import uk.co.edcourtenay.quiztracker.data.QuizRepository
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.ScoreEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import uk.co.edcourtenay.quiztracker.data.TieBreakAnswerEntity
import uk.co.edcourtenay.quiztracker.data.TieBreakEntity
import uk.co.edcourtenay.quiztracker.domain.Prizes
import uk.co.edcourtenay.quiztracker.domain.Scoring
import uk.co.edcourtenay.quiztracker.domain.Standings
import uk.co.edcourtenay.quiztracker.domain.TeamScore
import uk.co.edcourtenay.quiztracker.domain.TieBreakState
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
    val prizes: List<PrizeEntity> = emptyList(),
    val standings: Standings = Standings(
        complete = false,
        awards = emptyList(),
        order = emptyList(),
        prizeByTeam = emptyMap(),
        contested = emptyList(),
    ),
    /** Tie-break question and answers per contested final position. */
    val tieBreaks: Map<Int, TieBreakState> = emptyMap(),
    val config: ConfigEntity = ConfigEntity.Default,
) {
    val teamCount: Int get() = teams.size
    val totalPlayers: Int get() = teams.sumOf { it.players ?: 0 }
    val totalFunds: Double get() = scores.sumOf { it.amount ?: 0.0 }
    val totalPrizeValue: Double get() = prizes.sumOf { it.value ?: 0.0 }
}

/** A one-shot request to reveal a team on the Scores screen. */
data class RevealRequest(
    val teamId: Long,
    /** Which round's field to focus, or null when the board has no editable value. */
    val roundId: Long?,
)

private data class QuizData(
    val teams: List<TeamEntity>,
    val rounds: List<RoundEntity>,
    val scores: List<ScoreEntity>,
    val config: ConfigEntity,
)

private data class PrizeData(
    val prizes: List<PrizeEntity>,
    val tieBreaks: List<TieBreakEntity>,
    val answers: List<TieBreakAnswerEntity>,
)

class QuizViewModel(private val repository: QuizRepository) : ViewModel() {

    private val quizData = combine(
        repository.teams,
        repository.rounds,
        repository.scores,
        repository.config,
    ) { teams, rounds, scores, config -> QuizData(teams, rounds, scores, config) }

    private val prizeData = combine(
        repository.prizes,
        repository.tieBreaks,
        repository.tieBreakAnswers,
    ) { prizes, tieBreaks, answers -> PrizeData(prizes, tieBreaks, answers) }

    val uiState: StateFlow<QuizUiState> =
        combine(quizData, prizeData) { quiz, prize ->
            val scores = Scoring.scoreTeams(quiz.teams, quiz.rounds, quiz.scores, quiz.config.feePerPlayer)
            val tieBreaks = tieBreakStates(prize)
            QuizUiState(
                teams = quiz.teams,
                rounds = quiz.rounds,
                scores = scores,
                prizes = prize.prizes,
                standings = Prizes.standings(scores, prize.prizes, tieBreaks),
                tieBreaks = tieBreaks,
                config = quiz.config,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QuizUiState(),
        )

    /** Tie-break state per contested rank, merging the question row with its answers. */
    private fun tieBreakStates(data: PrizeData): Map<Int, TieBreakState> {
        val ranks = (data.tieBreaks.map { it.rank } + data.answers.map { it.rank }).distinct()
        return ranks.associateWith { rank ->
            val row = data.tieBreaks.firstOrNull { it.rank == rank }
            TieBreakState(
                question = row?.question.orEmpty(),
                correctAnswer = row?.correctAnswer,
                answers = data.answers
                    .filter { it.rank == rank }
                    .associate { it.teamId to it.answer },
            )
        }
    }

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
    // Prizes
    // ---------------------------------------------------------------------

    fun addPrize() = viewModelScope.launch {
        val position = uiState.value.prizes.size + 1
        repository.addPrize(name = "$position")
    }

    fun updatePrize(prize: PrizeEntity) = viewModelScope.launch {
        repository.updatePrize(prize)
    }

    fun movePrize(prize: PrizeEntity, offset: Int) = viewModelScope.launch {
        repository.movePrize(prize, offset)
    }

    fun deletePrize(prize: PrizeEntity) = viewModelScope.launch {
        repository.deletePrize(prize)
    }

    // ---------------------------------------------------------------------
    // Tie-breaks
    // ---------------------------------------------------------------------

    fun setTieBreakQuestion(rank: Int, question: String) = viewModelScope.launch {
        repository.setTieBreakQuestion(rank, question)
    }

    fun setTieBreakAnswer(rank: Int, teamId: Long, answer: Double?) = viewModelScope.launch {
        repository.setTieBreakAnswer(rank, teamId, answer)
    }

    fun setTieBreakCorrectAnswer(rank: Int, answer: Double?) = viewModelScope.launch {
        repository.setTieBreakCorrectAnswer(rank, answer)
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
