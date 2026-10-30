package com.coopers.quiztracker

import android.app.Application
import com.coopers.quiztracker.data.QuizDatabase
import com.coopers.quiztracker.data.QuizRepository

class QuizTrackerApp : Application() {

    private val database by lazy { QuizDatabase.get(this) }

    val repository by lazy { QuizRepository(database.quizDao()) }
}
