package uk.co.edcourtenay.quiztracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.co.edcourtenay.quiztracker.ui.screens.LeaderboardScreen
import uk.co.edcourtenay.quiztracker.ui.screens.SettingsScreen
import uk.co.edcourtenay.quiztracker.ui.screens.TeamsScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    Scores("Scores", Icons.Filled.Edit),
    Leaderboard("Board", Icons.Filled.Star),
    Settings("Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTrackerRoot(
    viewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = Tab.entries

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.config.quizName.ifBlank { "Quiz Tracker" }) })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tabs[selectedTab]) {
                Tab.Scores -> TeamsScreen(state = state, viewModel = viewModel)
                Tab.Leaderboard -> LeaderboardScreen(
                    state = state,
                    onTeamClick = { teamId ->
                        // Jump to the Scores screen with that team brought into view.
                        viewModel.revealTeam(teamId)
                        selectedTab = Tab.Scores.ordinal
                    },
                )
                Tab.Settings -> SettingsScreen(state = state, viewModel = viewModel)
            }
        }
    }
}
