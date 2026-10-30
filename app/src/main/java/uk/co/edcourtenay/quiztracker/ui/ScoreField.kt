package uk.co.edcourtenay.quiztracker.ui

/**
 * An editable score field on a team's card.
 *
 * Used to focus the right input when the user jumps to a team from a
 * leaderboard board. The Final board has no single editable value, so it maps
 * to no field.
 */
enum class ScoreField {
    Rounds1to4,
    Rounds5to6,
    Wipeout,
}
