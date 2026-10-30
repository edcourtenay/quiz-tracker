package uk.co.edcourtenay.quiztracker.domain

import uk.co.edcourtenay.quiztracker.data.PrizeEntity

/** A tie-break question and its answers for one contested final position. */
data class TieBreakState(
    val question: String = "",
    val correctAnswer: Double? = null,
    /** teamId -> that team's guess. */
    val answers: Map<Long, Double> = emptyMap(),
)

/** A final position shared by more than one team in the prize places. */
data class ContestedRank(
    val rank: Int,
    val teams: List<TeamScore>,
)

/** What happened to one prize. */
sealed interface PrizeAward {
    /** Awarded outright, or decided by a resolved tie-break. */
    data class Awarded(val prize: PrizeEntity, val team: TeamScore) : PrizeAward

    /** Still tied: [candidates] are the teams in the running. */
    data class Contested(
        val prize: PrizeEntity,
        val rank: Int,
        val candidates: List<TeamScore>,
    ) : PrizeAward

    /** Nobody left to take it. */
    data class Unclaimed(val prize: PrizeEntity) : PrizeAward
}

/**
 * The end-of-night picture: what each prize went to, the resolved standings, and
 * which positions still need a tie-break.
 */
data class Standings(
    /** True once every team has a total (all rounds scored). */
    val complete: Boolean,
    val awards: List<PrizeAward>,
    /** Ranked teams, with contested groups ordered by their tie-break where resolved. */
    val order: List<TeamScore>,
    val prizeByTeam: Map<Long, PrizeEntity>,
    val contested: List<ContestedRank>,
)

/**
 * Prize assignment and tie-break resolution.
 *
 * A tie only matters where it touches the prizes, so a rank is "contested" when
 * two or more teams share it *and* at least one of the prize positions falls on
 * it. A single closest-number question ranks the whole tied group by distance,
 * which also settles a group larger than the slots remaining (e.g. three teams
 * tied for the last two prizes — the furthest guess misses out).
 */
object Prizes {

    fun standings(
        scores: List<TeamScore>,
        prizes: List<PrizeEntity>,
        tieBreaks: Map<Int, TieBreakState>,
    ): Standings {
        val ranked = scores
            .filter { it.rankFinal != null }
            .sortedWith(compareBy({ it.rankFinal }, { it.team.createdAt }))

        val complete = scores.isNotEmpty() && scores.all { it.rankFinal != null }
        val groups = ranked.groupBy { it.rankFinal!! }.toSortedMap()

        val contested = groups
            .filter { (rank, teams) -> teams.size > 1 && rank <= prizes.size }
            .map { (rank, teams) -> ContestedRank(rank, teams) }

        // Order within each group: tie-break result where resolved, else entry order.
        val order = mutableListOf<TeamScore>()
        groups.forEach { (rank, teams) ->
            order += if (teams.size == 1) teams else resolve(teams, tieBreaks[rank]) ?: teams
        }

        val awards = mutableListOf<PrizeAward>()
        var slot = 0
        groups.forEach { (rank, teams) ->
            if (slot >= prizes.size) return@forEach
            val resolution = if (teams.size > 1) resolve(teams, tieBreaks[rank]) else null
            val settled = teams.size == 1 || resolution != null
            val sequence = resolution ?: teams
            sequence.forEach { team ->
                if (slot >= prizes.size) return@forEach
                awards += if (settled) {
                    PrizeAward.Awarded(prizes[slot], team)
                } else {
                    PrizeAward.Contested(prizes[slot], rank, teams)
                }
                slot++
            }
        }
        while (slot < prizes.size) {
            awards += PrizeAward.Unclaimed(prizes[slot])
            slot++
        }

        val prizeByTeam = awards
            .filterIsInstance<PrizeAward.Awarded>()
            .associate { it.team.team.id to it.prize }

        return Standings(
            complete = complete,
            awards = awards,
            order = order,
            prizeByTeam = prizeByTeam,
            contested = contested,
        )
    }

    /**
     * Ranks a tied group by how close each guess is to the correct answer.
     *
     * Returns null while the tie is unresolved — the correct answer is missing,
     * a team has not answered, or two teams gave the same guess (which no
     * closest-number rule can separate). An exact match wins; otherwise the lower
     * guess wins, the usual pub-quiz convention.
     */
    fun resolve(teams: List<TeamScore>, state: TieBreakState?): List<TeamScore>? {
        val correct = state?.correctAnswer ?: return null
        val guesses = teams.map { team -> state.answers[team.team.id] }
        if (guesses.any { it == null }) return null
        if (guesses.filterNotNull().distinct().size != teams.size) return null

        return teams
            .map { team -> team to (state.answers.getValue(team.team.id) - correct) }
            .sortedWith(compareBy({ kotlin.math.abs(it.second) }, { it.second }))
            .map { it.first }
    }
}
