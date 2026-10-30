package com.coopers.quiztracker.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coopers.quiztracker.export.ResultsExporter
import com.coopers.quiztracker.ui.QuizUiState
import com.coopers.quiztracker.ui.QuizViewModel
import com.coopers.quiztracker.ui.formatMoney

@Composable
fun SettingsScreen(
    state: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier,
) {
    SettingsScreenContent(
        state = state,
        onQuizNameChange = { viewModel.setQuizName(it) },
        onFeeChange = { viewModel.setFeePerPlayer(it) },
        onClearScores = { viewModel.clearAllScores() },
        onDeleteAllTeams = { viewModel.deleteAllTeams() },
        modifier = modifier,
    )
}

/**
 * Stateless Settings screen. Takes plain state and callbacks so it can be
 * rendered in `@Preview` without a ViewModel.
 */
@Composable
internal fun SettingsScreenContent(
    state: QuizUiState,
    onQuizNameChange: (String) -> Unit = {},
    onFeeChange: (Double) -> Unit = {},
    onClearScores: () -> Unit = {},
    onDeleteAllTeams: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var confirmClearScores by remember { mutableStateOf(false) }
    var confirmDeleteTeams by remember { mutableStateOf(false) }

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
                    text = "Entry fee is charged per player, mirroring the " +
                        "Amount = fee × players rule in the original sheet.",
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
                            "a per-player entry fee, scores for rounds 1-4 and 5-6, a Wipeout " +
                            "round, and automatic subtotals, totals, ranks and final positions " +
                            "with live leaderboards. Data stays on this device.",
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
            message = "Team names and player counts are kept, but every score will be removed.",
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
            message = "This removes every team and all of their scores. This cannot be undone.",
            confirmLabel = "Delete teams",
            onConfirm = {
                onDeleteAllTeams()
                confirmDeleteTeams = false
            },
            onDismiss = { confirmDeleteTeams = false },
        )
    }
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
