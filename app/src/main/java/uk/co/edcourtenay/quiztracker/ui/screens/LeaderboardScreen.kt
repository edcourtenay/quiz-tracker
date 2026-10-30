package uk.co.edcourtenay.quiztracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.domain.TeamScore
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import uk.co.edcourtenay.quiztracker.ui.formatScore

private data class BoardRow(
    val teamId: Long,
    val rank: Int?,
    val name: String,
    /** The big number on the right. */
    val headline: Int?,
    val headlineIsAlert: Boolean,
    val caption: String,
    val captionIsAlert: Boolean,
    val isLeader: Boolean,
    /** True when another team shares this rank. */
    val isTied: Boolean = false,
)

/**
 * One board per configured round. Each board ranks teams either by the running
 * total through that round (the default) or by that round's own score — the
 * first round has no choice to make, since the two are identical there.
 */
@Composable
fun LeaderboardScreen(
    state: QuizUiState,
    selectedBoardId: Long?,
    onBoardSelected: (Long) -> Unit,
    orderByRunningTotal: Boolean,
    onOrderByRunningTotalChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTeamClick: (teamId: Long, roundId: Long?) -> Unit = { _, _ -> },
) {
    val rounds = state.rounds
    val selectedIndex = rounds.indexOfFirst { it.id == selectedBoardId }.takeIf { it >= 0 } ?: 0
    val board = rounds.getOrNull(selectedIndex)

    if (board == null) {
        Text(
            text = "No rounds configured yet. Add one in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(24.dp),
        )
        return
    }

    val rows = remember(state.scores, board, selectedIndex, orderByRunningTotal) {
        buildRows(board, selectedIndex, orderByRunningTotal, state.scores)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rounds.forEachIndexed { index, round ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onBoardSelected(round.id) },
                    label = { Text(round.name.ifBlank { "Round ${index + 1}" }) },
                )
            }
        }

        if (selectedIndex > 0) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Order by",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilterChip(
                    selected = orderByRunningTotal,
                    onClick = { onOrderByRunningTotalChange(true) },
                    label = { Text( if (selectedIndex != state.rounds.lastIndex) "Running total" else "Final total") },
                )
                FilterChip(
                    selected = !orderByRunningTotal,
                    onClick = { onOrderByRunningTotalChange(false) },
                    label = { Text("Round result") },
                )
            }
        }

        if (rows.isEmpty()) {
            Text(
                text = "No scores entered for this round yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows) { row ->
                    LeaderboardRow(
                        row = row,
                        onClick = { onTeamClick(row.teamId, board.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(row: BoardRow, onClick: () -> Unit) {
    val container = if (row.isLeader) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val headlineColor = when {
        row.headlineIsAlert -> MaterialTheme.colorScheme.error
        row.isLeader -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 34.dp, minHeight = 34.dp)
                    .clip(CircleShape)
                    .background(
                        if (row.isLeader) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.rankLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (row.isLeader) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (row.isLeader) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = row.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.captionIsAlert) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Text(
                text = formatScore(row.headline),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = headlineColor,
            )
        }
    }
}

private fun buildRows(
    round: RoundEntity,
    roundIndex: Int,
    orderByRunningTotal: Boolean,
    scores: List<TeamScore>,
): List<BoardRow> {
    // The first round's running total *is* its own score, so the choice is moot.
    val useRunningTotal = orderByRunningTotal && roundIndex > 0

    val rankOf: (TeamScore) -> Int? = { score ->
        if (useRunningTotal) score.runningRank(round.id) else score.rank(round.id)
    }
    val included = scores.filter { score ->
        if (useRunningTotal) score.runningTotal(round.id) != null else score.value(round.id) != null
    }

    return included
        .sortedWith(compareBy({ rankOf(it) ?: Int.MAX_VALUE }, { it.team.createdAt }))
        .map { it.toRow(round, useRunningTotal) }
        .withTieFlags()
}

private fun TeamScore.toRow(round: RoundEntity, useRunningTotal: Boolean): BoardRow {
    val roundValue = value(round.id)
    val runningTotal = runningTotal(round.id)
    val rank = if (useRunningTotal) runningRank(round.id) else rank(round.id)
    val isZeroAlert = roundValue == 0

    return BoardRow(
        teamId = team.id,
        rank = rank,
        name = name,
        headline = if (useRunningTotal) runningTotal else roundValue,
        headlineIsAlert = !useRunningTotal && isZeroAlert,
        caption = if (useRunningTotal) {
            "${round.name.ifBlank { "Round" }}: ${formatScore(roundValue)}"
        } else {
            "Total so far: ${formatScore(runningTotal)}"
        },
        captionIsAlert = useRunningTotal && isZeroAlert,
        isLeader = rank == 1,
    )
}

/**
 * Competition ranks repeat on a tie (1, 2, 2, 4). Flag every row whose rank is
 * shared by more than one team so its badge can render as "=2".
 */
private fun List<BoardRow>.withTieFlags(): List<BoardRow> {
    val counts = mapNotNull { it.rank }.groupingBy { it }.eachCount()
    return map { row ->
        val rank = row.rank
        row.copy(isTied = rank != null && (counts[rank] ?: 0) > 1)
    }
}

/** "3" normally, "=3" when the rank is shared, "-" when unranked. */
private fun BoardRow.rankLabel(): String = when {
    rank == null -> "-"
    isTied -> "=$rank"
    else -> rank.toString()
}
