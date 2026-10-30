package uk.co.edcourtenay.quiztracker

import android.app.Application
import uk.co.edcourtenay.quiztracker.data.QuizDatabase
import uk.co.edcourtenay.quiztracker.data.QuizRepository

class QuizTrackerApp : Application() {

    private val database by lazy { QuizDatabase.get(this) }

    val repository by lazy { QuizRepository(database.quizDao()) }
}
