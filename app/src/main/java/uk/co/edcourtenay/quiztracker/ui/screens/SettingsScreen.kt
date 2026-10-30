package uk.co.edcourtenay.quiztracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.co.edcourtenay.quiztracker.data.PrizeEntity
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.export.ResultsExporter
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import uk.co.edcourtenay.quiztracker.ui.QuizViewModel
import uk.co.edcourtenay.quiztracker.ui.components.DecimalField
import uk.co.edcourtenay.quiztracker.ui.formatMoney

@Composable
fun SettingsScreen(
    state: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier,
) {
    SettingsScreenContent(
        state = state,
        modifier = modifier,
        onQuizNameChange = { viewModel.setQuizName(it) },
        onFeeChange = { viewModel.setFeePerPlayer(it) },
        onClearScores = { viewModel.clearAllScores() },
        onDeleteAllTeams = { viewModel.deleteAllTeams() },
        onAddRound = { viewModel.addRound() },
        onRenameRound = { round, name -> viewModel.renameRound(round, name) },
        onMoveRound = { round, offset -> viewModel.moveRound(round, offset) },
        onDeleteRound = { viewModel.deleteRound(it) },
        onCountScoresForRound = { viewModel.countScoresForRound(it) },
        onAddPrize = { viewModel.addPrize() },
        onUpdatePrize = { viewModel.updatePrize(it) },
        onMovePrize = { prize, offset -> viewModel.movePrize(prize, offset) },
    ) { viewModel.deletePrize(it) }
}

/**
 * Stateless Settings screen. Takes plain state and callbacks so it can be
 * rendered in `@Preview` without a ViewModel.
 */
@Composable
internal fun SettingsScreenContent(
    state: QuizUiState,
    modifier: Modifier = Modifier,
    onQuizNameChange: (String) -> Unit = {},
    onFeeChange: (Double) -> Unit = {},
    onClearScores: () -> Unit = {},
    onDeleteAllTeams: () -> Unit = {},
    onAddRound: () -> Unit = {},
    onRenameRound: (RoundEntity, String) -> Unit = { _, _ -> },
    onMoveRound: (RoundEntity, Int) -> Unit = { _, _ -> },
    onDeleteRound: (RoundEntity) -> Unit = {},
    onCountScoresForRound: suspend (Long) -> Int = { 0 },
    onAddPrize: () -> Unit = {},
    onUpdatePrize: (PrizeEntity) -> Unit = {},
    onMovePrize: (PrizeEntity, Int) -> Unit = { _, _ -> },
    onDeletePrize: (PrizeEntity) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClearScores by remember { mutableStateOf(false) }
    var confirmDeleteTeams by remember { mutableStateOf(false) }
    var pendingRoundDelete by remember { mutableStateOf<Pair<RoundEntity, Int>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Quiz setup", style = MaterialTheme.typography.titleMedium)

                ConfigTextField(
                    label = "Quiz name",
                    value = state.config.quizName,
                    onValueChange = onQuizNameChange,
                )

                FeeField(
                    fee = state.config.feePerPlayer,
                    onFeeChange = onFeeChange,
                )

                Text(
                    text = "Entry fee charged per player",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Rounds", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "The stages teams are scored on, in play order. " +
                        "The last round is excluded from the subtotal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                state.rounds.forEachIndexed { index, round ->
                    RoundRow(
                        round = round,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.rounds.lastIndex,
                        canDelete = state.rounds.size > 1,
                        onNameChange = { onRenameRound(round, it) },
                        onMoveUp = { onMoveRound(round, -1) },
                        onMoveDown = { onMoveRound(round, 1) },
                        onDelete = {
                            scope.launch {
                                pendingRoundDelete = round to onCountScoresForRound(round.id)
                            }
                        },
                    )
                }

                Button(
                    onClick = onAddRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Add round")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Prizes", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Awarded to the top-scoring teams once every round has been " +
                        "scored. Leave the value blank for a non-cash prize.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                state.prizes.forEachIndexed { index, prize ->
                    PrizeRow(
                        prize = prize,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.prizes.lastIndex,
                        onNameChange = { onUpdatePrize(prize.copy(name = it)) },
                        onValueChange = { onUpdatePrize(prize.copy(value = it)) },
                        onMoveUp = { onMovePrize(prize, -1) },
                        onMoveDown = { onMovePrize(prize, 1) },
                        onDelete = { onDeletePrize(prize) },
                    )
                }

                Button(
                    onClick = onAddPrize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Add prize")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Tonight", style = MaterialTheme.typography.titleMedium)
                SummaryLine("Teams", state.teamCount.toString())
                SummaryLine("Players", state.totalPlayers.toString())
                SummaryLine("Funds collected", formatMoney(state.totalFunds))

                HorizontalDivider()

                Button(
                    onClick = {
                        val intent = ResultsExporter.shareIntent(context, state)
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "Share results"),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.teams.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Share results (CSV)")
                }

                OutlinedButton(
                    onClick = { confirmClearScores = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.teams.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("  Clear all scores")
                }

                OutlinedButton(
                    onClick = { confirmDeleteTeams = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.teams.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("  Delete all teams")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Quiz Tracker scores a pub quiz night: teams and players, " +
                            "a per-player entry fee, scores for each configured round, and " +
                            "automatic subtotals, totals, ranks and final positions with " +
                            "live leaderboards. Data stays on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    if (confirmClearScores) {
        ConfirmDialog(
            title = "Clear all scores?",
            message = "Every score will be removed. Team names, player counts and the " +
                "configured rounds are kept.",
            confirmLabel = "Clear scores",
            onConfirm = {
                onClearScores()
                confirmClearScores = false
            },
            onDismiss = { confirmClearScores = false },
        )
    }

    if (confirmDeleteTeams) {
        ConfirmDialog(
            title = "Delete all teams?",
            message = "This removes every team and their scores. The configured rounds " +
                "are kept. This cannot be undone.",
            confirmLabel = "Delete teams",
            onConfirm = {
                onDeleteAllTeams()
                confirmDeleteTeams = false
            },
            onDismiss = { confirmDeleteTeams = false },
        )
    }

    pendingRoundDelete?.let { (round, scoreCount) ->
        ConfirmDialog(
            title = "Delete \"${round.name.ifBlank { "round" }}\"?",
            message = if (scoreCount > 0) {
                "$scoreCount team(s) have a score recorded for this round. Deleting the " +
                    "round will remove those scores. Teams are not affected."
            } else {
                "This round has no scores recorded. Teams are not affected."
            },
            confirmLabel = "Delete round",
            onConfirm = {
                onDeleteRound(round)
                pendingRoundDelete = null
            },
            onDismiss = { pendingRoundDelete = null },
        )
    }
}

@Composable
private fun PrizeRow(
    prize: PrizeEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onNameChange: (String) -> Unit,
    onValueChange: (Double?) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
            }
            PrizeNameField(
                prizeId = prize.id,
                initialName = prize.name,
                onNameChange = onNameChange,
                modifier = Modifier.weight(1f, true),
            )
            DecimalField(
                value = prize.value,
                label = "Value (£)",
                onValueChange = onValueChange,
                //modifier = Modifier
                //    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                //    .width(160.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete prize",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }


    }
}

@Composable
private fun PrizeNameField(
    prizeId: Long,
    initialName: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local state so the cursor does not jump while Room round-trips.
    var text by remember(prizeId) { mutableStateOf(initialName) }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onNameChange(it)
        },
        label = { Text("Prize") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun RoundRow(
    round: RoundEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
        }
        RoundNameField(
            roundId = round.id,
            initialName = round.name,
            onNameChange = onNameChange,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete, enabled = canDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete round",
                tint = if (canDelete) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}

@Composable
private fun RoundNameField(
    roundId: Long,
    initialName: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local state so the cursor does not jump while Room round-trips.
    var text by remember(roundId) { mutableStateOf(initialName) }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onNameChange(it)
        },
        label = { Text("Round name") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FeeField(
    fee: Double,
    onFeeChange: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(formatFeeInput(fee)) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val cleaned = input.filter { it.isDigit() || it == '.' }
            text = cleaned
            cleaned.toDoubleOrNull()?.let(onFeeChange)
        },
        label = { Text("Entry fee per player (£)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun formatFeeInput(fee: Double): String =
    if (fee == fee.toLong().toDouble()) fee.toLong().toString() else fee.toString()

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
