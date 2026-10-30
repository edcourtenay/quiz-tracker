package uk.co.edcourtenay.quiztracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import uk.co.edcourtenay.quiztracker.ui.QuizTrackerRoot
import uk.co.edcourtenay.quiztracker.ui.theme.QuizTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            QuizTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuizTrackerRoot()
                }
            }
        }
    }
}
