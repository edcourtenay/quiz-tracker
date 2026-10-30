package com.coopers.quiztracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = QuizGreen,
    onPrimary = Color.White,
    primaryContainer = QuizGreenContainer,
    onPrimaryContainer = Color(0xFF0B2318),
    secondary = QuizBlue,
    onSecondary = Color.White,
    secondaryContainer = QuizBlueContainer,
    onSecondaryContainer = Color(0xFF10233F),
    error = QuizRed,
    onError = Color.White,
    errorContainer = QuizRedContainer,
    onErrorContainer = Color(0xFF3F1414),
    background = QuizSurfaceLight,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = QuizGreenDark,
    onPrimary = Color(0xFF06231A),
    primaryContainer = Color(0xFF1E4536),
    onPrimaryContainer = QuizGreenContainer,
    secondary = QuizBlue,
    onSecondary = Color(0xFF0A1B33),
    secondaryContainer = Color(0xFF26405F),
    onSecondaryContainer = QuizBlueContainer,
    error = QuizRed,
    onError = Color(0xFF3A1010),
    errorContainer = Color(0xFF5C2020),
    onErrorContainer = QuizRedContainer,
    background = QuizSurfaceDark,
    surface = Color(0xFF1B1F1D),
)

@Composable
fun QuizTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuizTypography,
        content = content,
    )
}
