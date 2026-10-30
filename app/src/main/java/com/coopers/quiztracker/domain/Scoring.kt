package com.coopers.quiztracker.domain

import com.coopers.quiztracker.data.TeamEntity

/**
 * A team plus every value the workbook derives for it.
 *
 * The formulas below are a faithful port of the "Scores" sheet:
 *
 *  - Amount   : `=if(B2, 2*B2, "")`            -> fee x players, blank when no players
 *  - Subtotal : `=IF(F2, D2+F2, "")`           -> rounds1-4 + rounds5-6, blank until rounds 5-6 entered
 *  - Total    : `=if(I2<>"", I2+G2, "")`       -> subtotal + wipeout, blank until wipeout entered
 *  - Ranks    : `=Rank(value, column)`         -> Excel RANK.EQ (ties share a rank, next rank is skipped)
 */
data class TeamScore(
    val team: TeamEntity,
    val amount: Double?,
    val subtotal: Int?,
    val total: Int?,
    val rankRounds1to4: Int?,
    val rankRounds5to6: Int?,
    val rankWipeout: Int?,
    val rankFinal: Int?,
) {
    val name: String get() = team.name.ifBlank { "Unnamed team" }
}

object Scoring {

    fun scoreTeams(teams: List<TeamEntity>, feePerPlayer: Double): List<TeamScore> {
        val subtotals: Map<Long, Int?> = teams.associate { team ->
            team.id to if (team.rounds5to6 != null) {
                (team.rounds1to4 ?: 0) + team.rounds5to6
            } else {
                null
            }
        }

        val totals: Map<Long, Int?> = teams.associate { team ->
            val subtotal = subtotals[team.id]
            team.id to if (team.wipeout != null && subtotal != null) {
                subtotal + team.wipeout
            } else {
                null
            }
        }

        val rankRounds1to4 = ranks(teams.associate { it.id to it.rounds1to4 })
        val rankRounds5to6 = ranks(subtotals)
        val rankWipeout = ranks(teams.associate { it.id to it.wipeout })
        val rankFinal = ranks(totals)

        return teams.map { team ->
            TeamScore(
                team = team,
                amount = team.players?.let { it * feePerPlayer },
                subtotal = subtotals[team.id],
                total = totals[team.id],
                rankRounds1to4 = rankRounds1to4[team.id],
                rankRounds5to6 = rankRounds5to6[team.id],
                rankWipeout = rankWipeout[team.id],
                rankFinal = rankFinal[team.id],
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
