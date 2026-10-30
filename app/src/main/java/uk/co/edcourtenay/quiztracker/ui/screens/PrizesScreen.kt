package uk.co.edcourtenay.quiztracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.edcourtenay.quiztracker.domain.ContestedRank
import uk.co.edcourtenay.quiztracker.domain.PrizeAward
import uk.co.edcourtenay.quiztracker.domain.Prizes
import uk.co.edcourtenay.quiztracker.domain.TieBreakState
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import uk.co.edcourtenay.quiztracker.ui.QuizViewModel
import uk.co.edcourtenay.quiztracker.ui.components.DecimalField
import uk.co.edcourtenay.quiztracker.ui.formatMoney
import uk.co.edcourtenay.quiztracker.ui.ordinal

@Composable
fun PrizesScreen(
    state: QuizUiState,
    viewModel: QuizViewModel,
    onTeamClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrizesScreenContent(
        state = state,
        modifier = modifier,
        onTeamClick = onTeamClick,
        onQuestionChange = { rank, question -> viewModel.setTieBreakQuestion(rank, question) },
        onAnswerChange = { rank, teamId, answer -> viewModel.setTieBreakAnswer(rank, teamId, answer) },
    ) { rank, answer -> viewModel.setTieBreakCorrectAnswer(rank, answer) }
}

/**
 * Stateless Prizes screen. Takes plain state and callbacks so it can be rendered
 * in `@Preview` without a ViewModel.
 */
@Composable
internal fun PrizesScreenContent(
    state: QuizUiState,
    modifier: Modifier = Modifier,
    onTeamClick: (Long) -> Unit = {},
    onQuestionChange: (rank: Int, question: String) -> Unit = { _, _ -> },
    onAnswerChange: (rank: Int, teamId: Long, answer: Double?) -> Unit = { _, _, _ -> },
    onCorrectAnswerChange: (rank: Int, answer: Double?) -> Unit = { _, _ -> },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.prizes.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No prizes configured yet. Add them in Settings to award them " +
                        "to the top-scoring teams.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@Column
        }

        if (!state.standings.complete) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text(
                    text = if (state.teams.isEmpty()) {
                        "No teams yet."
                    } else {
                        "Not all rounds have been scored yet — these standings may still change."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
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
                Text("Prizes", style = MaterialTheme.typography.titleMedium)

                state.standings.awards.forEach { award ->
                    PrizeRow(award = award, onTeamClick = onTeamClick)
                }

                if (state.totalPrizeValue > 0.0) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total prize fund", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatMoney(state.totalPrizeValue), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        state.standings.contested.forEach { contested ->
            TieBreakCard(
                contested = contested,
                state = state.tieBreaks[contested.rank],
                onQuestionChange = { onQuestionChange(contested.rank, it) },
                onAnswerChange = { teamId, answer -> onAnswerChange(contested.rank, teamId, answer) },
                onCorrectAnswerChange = { onCorrectAnswerChange(contested.rank, it) },
                onTeamClick = onTeamClick,
            )
        }
    }
}

@Composable
private fun PrizeRow(award: PrizeAward, onTeamClick: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = award.prize().name.ifBlank { "Prize" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 12.dp),
        )

        when (award) {
            is PrizeAward.Awarded -> TextButton(
                onClick = { onTeamClick(award.team.team.id) },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = award.team.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }

            is PrizeAward.Contested -> Text(
                text = "Tied — see the tie-break below",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )

            is PrizeAward.Unclaimed -> Text(
                text = "Unclaimed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }

        award.prize().value?.let { value ->
            Text(formatMoney(value), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TieBreakCard(
    contested: ContestedRank,
    state: TieBreakState?,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (teamId: Long, answer: Double?) -> Unit,
    onCorrectAnswerChange: (Double?) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val resolved = Prizes.resolve(contested.teams, state)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tie-break for ${ordinal(contested.rank)} place",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${contested.teams.size} teams are tied on " +
                    "${contested.teams.first().total ?: 0} points. Ask a closest-number question.",
                style = MaterialTheme.typography.bodySmall,
            )

            TieBreakTextField(
                value = state?.question.orEmpty(),
                label = "Question",
                onValueChange = onQuestionChange,
            )

            contested.teams.forEach { team ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { onTeamClick(team.team.id) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(team.name, maxLines = 1)
                    }
                    DecimalField(
                        value = state?.answers?.get(team.team.id),
                        label = "Answer",
                        onValueChange = { onAnswerChange(team.team.id, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            DecimalField(
                value = state?.correctAnswer,
                label = "Correct answer",
                onValueChange = onCorrectAnswerChange,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            Text(
                text = if (resolved != null) {
                    "Order: " + resolved.joinToString(" → ") { it.name }
                } else {
                    tieBreakHint(contested, state)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun tieBreakHint(contested: ContestedRank, state: TieBreakState?): String {
    val answers = state?.answers.orEmpty()
    val missing = contested.teams.count { answers[it.team.id] == null }
    val duplicates = answers.values
        .filter { answer -> contested.teams.any { answers[it.team.id] == answer } }
        .groupingBy { it }
        .eachCount()
        .any { it.value > 1 }

    return when {
        missing > 0 -> "Waiting for $missing team(s) to answer."
        state?.correctAnswer == null -> "Enter the correct answer to settle this tie-break."
        duplicates -> "Two teams gave the same answer — ask another question."
        else -> "Tie-break unresolved."
    }
}

@Composable
private fun TieBreakTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    // Local state so the cursor does not jump while Room round-trips.
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

private fun PrizeAward.prize() = when (this) {
    is PrizeAward.Awarded -> prize
    is PrizeAward.Contested -> prize
    is PrizeAward.Unclaimed -> prize
}
