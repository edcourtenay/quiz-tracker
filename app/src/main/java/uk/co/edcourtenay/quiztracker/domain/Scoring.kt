package uk.co.edcourtenay.quiztracker.domain

import uk.co.edcourtenay.quiztracker.data.RoundEntity
import uk.co.edcourtenay.quiztracker.data.ScoreEntity
import uk.co.edcourtenay.quiztracker.data.TeamEntity

/**
 * A team plus every value the workbook derives for it, generalised to any number
 * of configurable rounds.
 *
 * The original sheet's formulas map onto this as follows (for the default three
 * rounds "Rounds 1-4", "Rounds 5-6", "Wipeout"):
 *
 *  - Amount        : `=if(B2, 2*B2, "")`      -> fee x players, blank when no players
 *  - Round value   : columns D, F, I          -> the score entered for that round
 *  - Running total : column G, then J         -> sum of rounds 0..i, blank until every
 *                                                one of those rounds has a score
 *  - Subtotal      : `=IF(F2, D2+F2, "")`     -> the running total after the *penultimate*
 *                                                round, blank when there is only one round
 *  - Total         : `=if(I2<>"", I2+G2, "")` -> the running total after the last round
 *  - Ranks         : `=Rank(value, column)`   -> Excel RANK.EQ (ties share a rank, the
 *                                                next rank is skipped)
 *
 * A running total is deliberately stricter than the sheet, which silently treats a
 * missing earlier round as 0. Here it stays blank until every round up to that
 * point has been entered, so a team that has not played an earlier round is not
 * ranked as though it scored nothing.
 */
data class TeamScore(
    val team: TeamEntity,
    val amount: Double?,
    /** roundId -> entered score. Absent means "not entered". */
    val values: Map<Long, Int>,
    /** roundId -> rank by that round's own score. */
    val ranks: Map<Long, Int?>,
    /** roundId -> running total through that round, or null while incomplete. */
    val runningTotals: Map<Long, Int?>,
    /** roundId -> rank by the running total through that round. */
    val runningRanks: Map<Long, Int?>,
    val subtotal: Int?,
    val total: Int?,
    val rankFinal: Int?,
) {
    val name: String get() = team.name.ifBlank { "Unnamed team" }

    fun value(roundId: Long): Int? = values[roundId]

    fun rank(roundId: Long): Int? = ranks[roundId]

    fun runningTotal(roundId: Long): Int? = runningTotals[roundId]

    fun runningRank(roundId: Long): Int? = runningRanks[roundId]
}

object Scoring {

    fun scoreTeams(
        teams: List<TeamEntity>,
        rounds: List<RoundEntity>,
        scores: List<ScoreEntity>,
        feePerPlayer: Double,
    ): List<TeamScore> {
        val valuesByTeam: Map<Long, Map<Long, Int>> = scores
            .groupBy { it.teamId }
            .mapValues { (_, rows) -> rows.associate { it.roundId to it.value } }

        // Running total through each round; null from the first missing round onwards.
        val runningTotals: Map<Long, Map<Long, Int?>> = teams.associate { team ->
            val values = valuesByTeam[team.id].orEmpty()
            var running = 0
            var complete = true
            val totals = LinkedHashMap<Long, Int?>(rounds.size)
            for (round in rounds) {
                val value = values[round.id]
                if (value == null) {
                    complete = false
                } else {
                    running += value
                }
                totals[round.id] = if (complete) running else null
            }
            team.id to totals
        }

        val ranksByRound: Map<Long, Map<Long, Int?>> = rounds.associate { round ->
            round.id to ranks(teams.associate { it.id to valuesByTeam[it.id]?.get(round.id) })
        }

        val runningRanksByRound: Map<Long, Map<Long, Int?>> = rounds.associate { round ->
            round.id to ranks(teams.associate { it.id to runningTotals[it.id]?.get(round.id) })
        }

        val penultimateRoundId = rounds.getOrNull(rounds.size - 2)?.id
        val lastRoundId = rounds.lastOrNull()?.id

        return teams.map { team ->
            TeamScore(
                team = team,
                amount = team.players?.let { it * feePerPlayer },
                values = valuesByTeam[team.id].orEmpty(),
                ranks = rounds.associate { it.id to ranksByRound.getValue(it.id)[team.id] },
                runningTotals = runningTotals[team.id].orEmpty(),
                runningRanks = rounds.associate { it.id to runningRanksByRound.getValue(it.id)[team.id] },
                subtotal = penultimateRoundId?.let { runningTotals[team.id]?.get(it) },
                total = lastRoundId?.let { runningTotals[team.id]?.get(it) },
                rankFinal = lastRoundId?.let { runningRanksByRound.getValue(it)[team.id] },
            )
        }
    }

    /**
     * Excel RANK.EQ semantics: a value's rank is 1 + the number of strictly
     * larger values. Ties therefore share a rank and the next rank is skipped
     * (e.g. 1, 2, 2, 4). Null values are unranked.
     */
    private fun ranks(values: Map<Long, Int?>): Map<Long, Int?> {
        val present = values.values.filterNotNull()
        return values.mapValues { (_, value) ->
            value?.let { 1 + present.count { other -> other > it } }
        }
    }
}
