package uk.co.edcourtenay.quiztracker.ui

import java.util.Locale

/** "£4.00", or an em dash when the value is not yet known. */
fun formatMoney(value: Double?): String =
    value?.let { "£" + String.format(Locale.getDefault(), "%.2f", it) } ?: "—"

/** A score, or an em dash when not yet entered. */
fun formatScore(value: Int?): String = value?.toString() ?: "—"

/** "1st", "2nd", "3rd", "11th" ... or an em dash when unranked. */
fun formatPosition(value: Int?): String = value?.let(::ordinal) ?: "—"

fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) {
        "th"
    } else {
        when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$n$suffix"
}
