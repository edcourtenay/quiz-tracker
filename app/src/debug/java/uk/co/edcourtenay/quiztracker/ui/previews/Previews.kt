package uk.co.edcourtenay.quiztracker.ui.previews

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.edcourtenay.quiztracker.ui.RevealRequest
import uk.co.edcourtenay.quiztracker.ui.components.NumberField
import uk.co.edcourtenay.quiztracker.ui.components.StatTile
import uk.co.edcourtenay.quiztracker.ui.components.ValueChip
import uk.co.edcourtenay.quiztracker.ui.screens.LeaderboardScreen
import uk.co.edcourtenay.quiztracker.ui.screens.PrizesScreenContent
import uk.co.edcourtenay.quiztracker.ui.screens.SettingsScreenContent
import uk.co.edcourtenay.quiztracker.ui.screens.SummaryCard
import uk.co.edcourtenay.quiztracker.ui.screens.TeamCard
import uk.co.edcourtenay.quiztracker.ui.screens.TeamsScreenContent
import uk.co.edcourtenay.quiztracker.ui.theme.QuizTrackerTheme

// ---------------------------------------------------------------------------
// Scores screen
// ---------------------------------------------------------------------------

@Preview(name = "Scores – empty", showBackground = true, heightDp = 800)
@Composable
private fun ScoresEmptyPreview() {
    QuizTrackerTheme {
        TeamsScreenContent(state = PreviewData.emptyState)
    }
}

@Preview(name = "Scores – with teams", showBackground = true, heightDp = 1400)
@Composable
private fun ScoresPreview() {
    QuizTrackerTheme {
        TeamsScreenContent(state = PreviewData.state())
    }
}

@Preview(
    name = "Scores – dark",
    showBackground = true,
    heightDp = 1400,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ScoresDarkPreview() {
    QuizTrackerTheme {
        TeamsScreenContent(state = PreviewData.state())
    }
}

@Preview(name = "Scores – revealed team", showBackground = true, heightDp = 1400)
@Composable
private fun ScoresRevealedPreview() {
    QuizTrackerTheme {
        // Mirrors tapping "Groovy Gang" on the Wipeout board.
        TeamsScreenContent(
            state = PreviewData.state(),
            revealRequest = RevealRequest(teamId = 4, roundId = 3),
        )
    }
}

// ---------------------------------------------------------------------------
// Leaderboard screen – one preview per board
// ---------------------------------------------------------------------------

@Preview(name = "Board – round 1 (running total)", showBackground = true, heightDp = 700)
@Composable
private fun BoardFirstPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(
            state = PreviewData.state(),
            selectedBoardId = PreviewData.rounds[0].id,
            onBoardSelected = {},
            orderByRunningTotal = true,
            onOrderByRunningTotalChange = {},
        )
    }
}

@Preview(name = "Board – running total", showBackground = true, heightDp = 760)
@Composable
private fun BoardRunningTotalPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(
            state = PreviewData.state(),
            selectedBoardId = PreviewData.rounds[1].id,
            onBoardSelected = {},
            orderByRunningTotal = true,
            onOrderByRunningTotalChange = {},
        )
    }
}

@Preview(name = "Board – round result", showBackground = true, heightDp = 760)
@Composable
private fun BoardRoundResultPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(
            state = PreviewData.state(),
            selectedBoardId = PreviewData.rounds[1].id,
            onBoardSelected = {},
            orderByRunningTotal = false,
            onOrderByRunningTotalChange = {},
        )
    }
}

@Preview(name = "Board – wipeout (zero flagged)", showBackground = true, heightDp = 760)
@Composable
private fun BoardWipeoutPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(
            state = PreviewData.state(),
            selectedBoardId = PreviewData.rounds[2].id,
            onBoardSelected = {},
            orderByRunningTotal = true,
            onOrderByRunningTotalChange = {},
        )
    }
}

@Preview(name = "Board – tied ranks", showBackground = true, heightDp = 760)
@Composable
private fun BoardTiedPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(
            state = PreviewData.tiedState,
            selectedBoardId = PreviewData.rounds[1].id,
            onBoardSelected = {},
            orderByRunningTotal = true,
            onOrderByRunningTotalChange = {},
        )
    }
}

// ---------------------------------------------------------------------------
// Settings screen
// ---------------------------------------------------------------------------

@Preview(name = "Settings", showBackground = true, heightDp = 2000)
@Composable
private fun SettingsPreview() {
    QuizTrackerTheme {
        SettingsScreenContent(state = PreviewData.state(prizes = PreviewData.prizes))
    }
}

// ---------------------------------------------------------------------------
// Prizes screen
// ---------------------------------------------------------------------------

@Preview(name = "Prizes – no prizes configured", showBackground = true, heightDp = 700)
@Composable
private fun PrizesEmptyPreview() {
    QuizTrackerTheme {
        PrizesScreenContent(state = PreviewData.state())
    }
}

@Preview(name = "Prizes – awarded", showBackground = true, heightDp = 900)
@Composable
private fun PrizesAwardedPreview() {
    QuizTrackerTheme {
        PrizesScreenContent(state = PreviewData.prizesAwardedState)
    }
}

@Preview(name = "Prizes – tie-break pending", showBackground = true, heightDp = 1300)
@Composable
private fun PrizesTieBreakPreview() {
    QuizTrackerTheme {
        PrizesScreenContent(state = PreviewData.tiedForPrizesState)
    }
}

@Preview(name = "Prizes – tie-break resolved", showBackground = true, heightDp = 1300)
@Composable
private fun PrizesTieBreakResolvedPreview() {
    QuizTrackerTheme {
        PrizesScreenContent(state = PreviewData.tieBreakResolvedState)
    }
}

// ---------------------------------------------------------------------------
// Individual components
// ---------------------------------------------------------------------------

@Preview(name = "Component – summary card", showBackground = true, widthDp = 380)
@Composable
private fun SummaryCardPreview() {
    QuizTrackerTheme {
        Surface {
            SummaryCard(state = PreviewData.state())
        }
    }
}

@Preview(name = "Component – team card", showBackground = true, widthDp = 380, heightDp = 420)
@Composable
private fun TeamCardPreview() {
    QuizTrackerTheme {
        Surface {
            TeamCard(
                score = PreviewData.state().scores.first(),
                rounds = PreviewData.rounds,
                requestNameFocus = false,
                isRevealed = false,
                focusRoundId = null,
                onNameFocusHandled = {},
                onNameChange = {},
                onPlayersChange = {},
                onScoreChange = { _, _ -> },
                onDelete = {},
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Preview(name = "Component – number field", showBackground = true, widthDp = 380, heightDp = 130)
@Composable
private fun NumberFieldPreview() {
    QuizTrackerTheme {
        Surface {
            NumberField(
                value = 21,
                label = "Rounds 1-4",
                onValueChange = {},
                modifier = Modifier
                    .padding(12.dp)
                    .width(180.dp),
            )
        }
    }
}

@Preview(name = "Component – chips", showBackground = true, widthDp = 380, heightDp = 180)
@Composable
private fun ChipsPreview() {
    QuizTrackerTheme {
        Surface {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Teams", "5")
                    StatTile("Players", "18")
                    StatTile("Funds", "£36.00")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueChip("Subtotal", "33", Modifier.weight(1f))
                    ValueChip("Total", "38", Modifier.weight(1f))
                    ValueChip("Position", "1st", Modifier.weight(1f))
                }
            }
        }
    }
}
