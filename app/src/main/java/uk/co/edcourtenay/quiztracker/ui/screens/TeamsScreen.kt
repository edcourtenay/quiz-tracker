package uk.co.edcourtenay.quiztracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.co.edcourtenay.quiztracker.data.TeamEntity
import uk.co.edcourtenay.quiztracker.domain.TeamScore
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import uk.co.edcourtenay.quiztracker.ui.QuizViewModel
import uk.co.edcourtenay.quiztracker.ui.components.NumberField
import uk.co.edcourtenay.quiztracker.ui.components.StatTile
import uk.co.edcourtenay.quiztracker.ui.components.ValueChip
import uk.co.edcourtenay.quiztracker.ui.formatMoney
import uk.co.edcourtenay.quiztracker.ui.formatPosition
import uk.co.edcourtenay.quiztracker.ui.formatScore

@Composable
fun TeamsScreen(
    state: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier,
) {
    val focusTeamId by viewModel.focusTeamId.collectAsStateWithLifecycle()

    TeamsScreenContent(
        state = state,
        focusTeamId = focusTeamId,
        onFocusHandled = viewModel::onFocusHandled,
        onAddTeam = { viewModel.addTeam() },
        onUpdateTeam = { viewModel.updateTeam(it) },
        onDeleteTeam = { viewModel.deleteTeam(it) },
        modifier = modifier,
    )
}

/**
 * Stateless Scores screen. Takes plain state and callbacks so it can be rendered
 * in `@Preview` without a ViewModel.
 */
@Composable
internal fun TeamsScreenContent(
    state: QuizUiState,
    focusTeamId: Long? = null,
    onFocusHandled: () -> Unit = {},
    onAddTeam: () -> Unit = {},
    onUpdateTeam: (TeamEntity) -> Unit = {},
    onDeleteTeam: (TeamEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // The LazyColumn starts with the summary card, plus an empty-state hint while
    // there are no teams, so team rows are offset by this many items.
    val teamRowOffset = if (state.teams.isEmpty()) 2 else 1

    // When a team is added, bring its row into view so its name field can focus.
    LaunchedEffect(focusTeamId, state.scores) {
        val id = focusTeamId ?: return@LaunchedEffect
        val index = state.scores.indexOfFirst { it.team.id == id }
        if (index >= 0) {
            listState.animateScrollToItem(index + teamRowOffset)
        }
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SummaryCard(state) }

            if (state.teams.isEmpty()) {
                item { EmptyHint() }
            }

            items(state.scores, key = { it.team.id }) { score ->
                TeamCard(
                    score = score,
                    requestNameFocus = focusTeamId == score.team.id,
                    onNameFocusHandled = onFocusHandled,
                    onNameChange = { onUpdateTeam(score.team.copy(name = it)) },
                    onPlayersChange = { onUpdateTeam(score.team.copy(players = it)) },
                    onRounds1to4Change = { onUpdateTeam(score.team.copy(rounds1to4 = it)) },
                    onRounds5to6Change = { onUpdateTeam(score.team.copy(rounds5to6 = it)) },
                    onWipeoutChange = { onUpdateTeam(score.team.copy(wipeout = it)) },
                    onDelete = { onDeleteTeam(score.team) },
                )
            }
        }

        FloatingActionButton(
            onClick = onAddTeam,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add team")
        }
    }
}

@Composable
internal fun SummaryCard(state: QuizUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatTile("Teams", state.teamCount.toString(), emphasis = true)
            StatTile("Players", state.totalPlayers.toString(), emphasis = true)
            StatTile("Funds", formatMoney(state.totalFunds), emphasis = true)
        }
    }
}

@Composable
private fun EmptyHint() {
    Text(
        text = "No teams yet. Tap + to add your first team.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 32.dp, horizontal = 8.dp),
    )
}

@Composable
internal fun TeamCard(
    score: TeamScore,
    requestNameFocus: Boolean,
    onNameFocusHandled: () -> Unit,
    onNameChange: (String) -> Unit,
    onPlayersChange: (Int?) -> Unit,
    onRounds1to4Change: (Int?) -> Unit,
    onRounds5to6Change: (Int?) -> Unit,
    onWipeoutChange: (Int?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val team: TeamEntity = score.team

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamNameField(
                    teamId = team.id,
                    initialName = team.name,
                    requestFocus = requestNameFocus,
                    onFocusHandled = onNameFocusHandled,
                    onNameChange = onNameChange,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete team",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            PlayersRow(
                players = team.players,
                onPlayersChange = onPlayersChange,
                amount = score.amount,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = team.rounds1to4,
                    label = "Rounds 1-4",
                    onValueChange = onRounds1to4Change,
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = team.rounds5to6,
                    label = "Rounds 5-6",
                    onValueChange = onRounds5to6Change,
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = team.wipeout,
                    label = "Wipeout",
                    onValueChange = onWipeoutChange,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueChip("Subtotal", formatScore(score.subtotal), Modifier.weight(1f))
                ValueChip("Total", formatScore(score.total), Modifier.weight(1f))
                ValueChip("Position", formatPosition(score.rankFinal), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TeamNameField(
    teamId: Long,
    initialName: String,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep the text local so the cursor does not jump while Room round-trips.
    var value by remember(teamId) { mutableStateOf(TextFieldValue(initialName)) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            // Select any existing text so typing replaces it.
            value = value.copy(selection = TextRange(0, value.text.length))
            keyboard?.show()
            onFocusHandled()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {
            value = it
            onNameChange(it.text)
        },
        label = { Text("Team name") },
        singleLine = true,
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Composable
private fun PlayersRow(
    players: Int?,
    onPlayersChange: (Int?) -> Unit,
    amount: Double?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Players",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
        IconButton(
            onClick = { onPlayersChange(((players ?: 0) - 1).coerceAtLeast(0).takeIf { it > 0 }) },
            enabled = (players ?: 0) > 0,
        ) {
            Text(
                text = "\u2212",
                style = MaterialTheme.typography.titleLarge,
                color = if ((players ?: 0) > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
        Text(
            text = (players ?: 0).toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp),
        )
        IconButton(onClick = { onPlayersChange((players ?: 0) + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Add player")
        }

        Spacer(Modifier.weight(1f))
        ValueChip("Fee", formatMoney(amount))
    }
}
