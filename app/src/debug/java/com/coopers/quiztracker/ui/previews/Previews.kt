package com.coopers.quiztracker.ui.previews

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
import com.coopers.quiztracker.ui.components.NumberField
import com.coopers.quiztracker.ui.components.StatTile
import com.coopers.quiztracker.ui.components.ValueChip
import com.coopers.quiztracker.ui.screens.LeaderboardScreen
import com.coopers.quiztracker.ui.screens.SettingsScreenContent
import com.coopers.quiztracker.ui.screens.SummaryCard
import com.coopers.quiztracker.ui.screens.TeamCard
import com.coopers.quiztracker.ui.screens.TeamsScreenContent
import com.coopers.quiztracker.ui.theme.QuizTrackerTheme

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

@Preview(name = "Scores – with teams", showBackground = true, heightDp = 1100)
@Composable
private fun ScoresPreview() {
    QuizTrackerTheme {
        TeamsScreenContent(state = PreviewData.state())
    }
}

@Preview(
    name = "Scores – dark",
    showBackground = true,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ScoresDarkPreview() {
    QuizTrackerTheme {
        TeamsScreenContent(state = PreviewData.state())
    }
}

// ---------------------------------------------------------------------------
// Leaderboard screen – one preview per board
// ---------------------------------------------------------------------------

@Preview(name = "Board – Rounds 1-4", showBackground = true, heightDp = 700)
@Composable
private fun BoardRounds1to4Preview() {
    QuizTrackerTheme {
        LeaderboardScreen(state = PreviewData.state(), initialBoardIndex = 0)
    }
}

@Preview(name = "Board – Rounds 5-6", showBackground = true, heightDp = 700)
@Composable
private fun BoardRounds5to6Preview() {
    QuizTrackerTheme {
        LeaderboardScreen(state = PreviewData.state(), initialBoardIndex = 1)
    }
}

@Preview(name = "Board – Wipeout", showBackground = true, heightDp = 700)
@Composable
private fun BoardWipeoutPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(state = PreviewData.state(), initialBoardIndex = 2)
    }
}

@Preview(name = "Board – Final", showBackground = true, heightDp = 700)
@Composable
private fun BoardFinalPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(state = PreviewData.state(), initialBoardIndex = 3)
    }
}

@Preview(name = "Board – tied ranks (Rounds 1-4)", showBackground = true, heightDp = 700)
@Composable
private fun BoardTiedRounds14Preview() {
    QuizTrackerTheme {
        LeaderboardScreen(state = PreviewData.tiedState, initialBoardIndex = 0)
    }
}

@Preview(name = "Board – tied ranks (Final)", showBackground = true, heightDp = 700)
@Composable
private fun BoardTiedFinalPreview() {
    QuizTrackerTheme {
        LeaderboardScreen(state = PreviewData.tiedState, initialBoardIndex = 3)
    }
}

// ---------------------------------------------------------------------------
// Settings screen
// ---------------------------------------------------------------------------

@Preview(name = "Settings", showBackground = true, heightDp = 1200)
@Composable
private fun SettingsPreview() {
    QuizTrackerTheme {
        SettingsScreenContent(state = PreviewData.state())
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

@Preview(name = "Component – team card", showBackground = true, widthDp = 380, heightDp = 320)
@Composable
private fun TeamCardPreview() {
    QuizTrackerTheme {
        Surface {
            TeamCard(
                score = PreviewData.state().scores.first(),
                requestNameFocus = false,
                onNameFocusHandled = {},
                onNameChange = {},
                onPlayersChange = {},
                onRounds1to4Change = {},
                onRounds5to6Change = {},
                onWipeoutChange = {},
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
