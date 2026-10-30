package com.coopers.quiztracker.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.coopers.quiztracker.domain.TeamScore
import com.coopers.quiztracker.ui.QuizUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns the current standings into a CSV that mirrors the columns of the
 * "Scores" sheet, then hands it to the system share sheet.
 */
object ResultsExporter {

    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    fun buildCsv(state: QuizUiState): String {
        val sb = StringBuilder()

        sb.appendLine(csvRow(listOf(state.config.quizName)))
        sb.appendLine(
            csvRow(
                listOf(
                    "Generated",
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                ),
            ),
        )
        sb.appendLine()

        sb.appendLine(
            csvRow(
                listOf(
                    "Team Name",
                    "Players",
                    "Amount",
                    "Rounds 1-4",
                    "Rounds 1-4 Rank",
                    "Rounds 5-6",
                    "Subtotal",
                    "Rounds 5-6 Rank",
                    "Wipeout",
                    "Total",
                    "Final Position",
                ),
            ),
        )

        val ordered = state.scores.sortedWith(
            compareBy(
                { it.rankFinal ?: Int.MAX_VALUE },
                { it.team.createdAt },
            ),
        )
        ordered.forEach { row ->
            sb.appendLine(
                csvRow(
                    listOf(
                        row.name,
                        row.team.players?.toString() ?: "",
                        money(row.amount),
                        row.team.rounds1to4?.toString() ?: "",
                        row.rankRounds1to4?.toString() ?: "",
                        row.team.rounds5to6?.toString() ?: "",
                        row.subtotal?.toString() ?: "",
                        row.rankRounds5to6?.toString() ?: "",
                        row.team.wipeout?.toString() ?: "",
                        row.total?.toString() ?: "",
                        row.rankFinal?.toString() ?: "",
                    ),
                ),
            )
        }

        sb.appendLine()
        sb.appendLine(csvRow(listOf("Total Players", state.totalPlayers.toString())))
        sb.appendLine(csvRow(listOf("Total Funds", money(state.totalFunds))))
        return sb.toString()
    }

    fun writeCsv(context: Context, state: QuizUiState): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "quiz-results.csv")
        file.writeText(buildCsv(state))
        return file
    }

    fun shareIntent(context: Context, state: QuizUiState): Intent {
        val file = writeCsv(context, state)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + FILE_PROVIDER_SUFFIX,
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${state.config.quizName} - results")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun csvRow(values: List<String>): String =
        values.joinToString(separator = ",") { escape(it) }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun money(value: Double?): String =
        value?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: ""
}
