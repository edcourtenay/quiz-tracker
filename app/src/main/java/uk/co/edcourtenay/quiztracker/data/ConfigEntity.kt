package uk.co.edcourtenay.quiztracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row configuration for the quiz night.
 *
 * [feePerPlayer] mirrors the sheet's `Amount = 2 * Players` rule, but is
 * configurable so the app is not hard-coded to £2.
 */
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val quizName: String = DEFAULT_QUIZ_NAME,
    val feePerPlayer: Double = DEFAULT_FEE,
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_QUIZ_NAME = "Coopers Quiz"
        const val DEFAULT_FEE = 2.0

        val Default = ConfigEntity()
    }
}
