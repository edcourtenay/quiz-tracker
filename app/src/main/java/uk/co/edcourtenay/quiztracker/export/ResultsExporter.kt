package uk.co.edcourtenay.quiztracker.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import uk.co.edcourtenay.quiztracker.ui.QuizUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns the current standings into a CSV, generalised to whatever rounds are
 * configured: the identity columns, then each round's score and rank, then the
 * subtotal (when there is more than one round), total and final position.
 *
 * The per-round rank is the rank by the running total through that round, which
 * is what the leaderboards order by by default.
 */
object ResultsExporter {

    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    fun buildCsv(state: QuizUiState): String {
        val rounds = state.rounds
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
                buildList {
                    add("Team Name")
                    add("Players")
                    add("Amount")
                    rounds.forEach { round ->
                        val name = round.name.ifBlank { "Round" }
                        add(name)
                        add("$name Rank")
                    }
                    if (rounds.size >= 2) add("Subtotal")
                    add("Total")
                    add("Final Position")
                },
            ),
        )

        val ordered = state.scores.sortedWith(
            compareBy(
                { it.rankFinal ?: Int.MAX_VALUE },
                { it.team.createdAt },
            ),
        )
        ordered.forEach { score ->
            sb.appendLine(
                csvRow(
                    buildList {
                        add(score.name)
                        add(score.team.players?.toString() ?: "")
                        add(money(score.amount))
                        rounds.forEach { round ->
                            add(score.value(round.id)?.toString() ?: "")
                            add(score.runningRank(round.id)?.toString() ?: "")
                        }
                        if (rounds.size >= 2) add(score.subtotal?.toString() ?: "")
                        add(score.total?.toString() ?: "")
                        add(score.rankFinal?.toString() ?: "")
                    },
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
