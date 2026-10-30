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
import uk.co.edcourtenay.quiztracker.domain.TeamScore
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import uk.co.edcourtenay.quiztracker.ui.formatScore

private enum class Board(val label: String) {
    Rounds14("Rounds 1-4"),
    Rounds56("Rounds 5-6"),
    Wipeout("Wipeout"),
    Final("Final"),
}

private data class BoardRow(
    val teamId: Long,
    val rank: Int?,
    val name: String,
    val value: Int?,
    val caption: String,
    val isLeader: Boolean,
    val isZeroWipeout: Boolean,
    /** True when another team shares this rank. */
    val isTied: Boolean = false,
)

@Composable
fun LeaderboardScreen(
    state: QuizUiState,
    selectedBoardIndex: Int,
    onBoardIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onTeamClick: (Long) -> Unit = {},
) {
    val boards = Board.entries
    val selected = selectedBoardIndex.coerceIn(0, boards.lastIndex)
    val board = boards[selected]

    val rows = remember(state.scores, board) { buildRows(board, state.scores) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            boards.forEachIndexed { index, item ->
                FilterChip(
                    selected = selected == index,
                    onClick = { onBoardIndexChange(index) },
                    label = { Text(item.label) },
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
                    LeaderboardRow(row = row, onClick = { onTeamClick(row.teamId) })
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
    val valueColor = when {
        row.isZeroWipeout -> MaterialTheme.colorScheme.error
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = formatScore(row.value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}

private fun buildRows(board: Board, scores: List<TeamScore>): List<BoardRow> {
    val rows = when (board) {
        Board.Rounds14 -> scores
            .filter { it.team.rounds1to4 != null }
            .sortedWith(compareBy({ it.rankRounds1to4 ?: Int.MAX_VALUE }, { it.team.createdAt }))
            .map { it.toRow(it.team.rounds1to4, it.rankRounds1to4, "Rounds 1-4") }

        Board.Rounds56 -> scores
            .filter { it.subtotal != null }
            .sortedWith(compareBy({ it.rankRounds5to6 ?: Int.MAX_VALUE }, { it.team.createdAt }))
            .map { it.toRow(it.subtotal, it.rankRounds5to6, "Rounds 5-6: ${formatScore(it.team.rounds5to6)}") }

        Board.Wipeout -> scores
            .filter { it.team.wipeout != null }
            .sortedWith(compareBy({ it.rankWipeout ?: Int.MAX_VALUE }, { it.team.createdAt }))
            .map {
                it.toRow(
                    value = it.team.wipeout,
                    rank = it.rankWipeout,
                    caption = "Subtotal: ${formatScore(it.subtotal)}",
                    isZeroWipeout = it.team.wipeout == 0,
                )
            }

        Board.Final -> scores
            .filter { it.total != null }
            .sortedWith(compareBy({ it.rankFinal ?: Int.MAX_VALUE }, { it.team.createdAt }))
            .map {
                it.toRow(
                    value = it.total,
                    rank = it.rankFinal,
                    caption = "Subtotal ${formatScore(it.subtotal)} + Wipeout ${formatScore(it.team.wipeout)}",
                )
            }
    }
    return rows.withTieFlags()
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

private fun TeamScore.toRow(
    value: Int?,
    rank: Int?,
    caption: String,
    isZeroWipeout: Boolean = false,
): BoardRow = BoardRow(
    teamId = team.id,
    rank = rank,
    name = name,
    value = value,
    caption = caption,
    isLeader = rank == 1,
    isZeroWipeout = isZeroWipeout,
)
