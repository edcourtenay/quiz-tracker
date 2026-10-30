package com.coopers.quiztracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coopers.quiztracker.domain.TeamScore
import com.coopers.quiztracker.ui.QuizUiState
import com.coopers.quiztracker.ui.formatScore

private enum class Board(val label: String) {
    Rounds14("Rounds 1-4"),
    Rounds56("Rounds 5-6"),
    Wipeout("Wipeout"),
    Final("Final"),
}

private data class BoardRow(
    val rank: Int?,
    val name: String,
    val value: Int?,
    val caption: String,
    val isLeader: Boolean,
    val isZeroWipeout: Boolean,
)

@Composable
fun LeaderboardScreen(
    state: QuizUiState,
    modifier: Modifier = Modifier,
    initialBoardIndex: Int = 0,
) {
    var selected by rememberSaveable {
        mutableIntStateOf(initialBoardIndex.coerceIn(0, Board.entries.lastIndex))
    }
    val boards = Board.entries
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
                    onClick = { selected = index },
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
                items(rows) { row -> LeaderboardRow(row) }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(row: BoardRow) {
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (row.isLeader) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(34.dp),
            ) {
                Text(
                    text = row.rank?.toString() ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (row.isLeader) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 5.dp),
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

private fun buildRows(board: Board, scores: List<TeamScore>): List<BoardRow> = when (board) {
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

private fun TeamScore.toRow(
    value: Int?,
    rank: Int?,
    caption: String,
    isZeroWipeout: Boolean = false,
): BoardRow = BoardRow(
    rank = rank,
    name = name,
    value = value,
    caption = caption,
    isLeader = rank == 1,
    isZeroWipeout = isZeroWipeout,
)
