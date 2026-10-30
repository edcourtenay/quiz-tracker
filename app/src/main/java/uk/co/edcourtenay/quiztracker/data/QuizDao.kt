package uk.co.edcourtenay.quiztracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    // ---------------------------------------------------------------------
    // Teams
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM teams ORDER BY createdAt ASC, id ASC")
    fun observeTeams(): Flow<List<TeamEntity>>

    @Insert
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    /** Removes every team and (by cascade) their scores. Rounds are untouched. */
    @Query("DELETE FROM teams")
    suspend fun deleteAllTeams()

    // ---------------------------------------------------------------------
    // Rounds
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM rounds ORDER BY position ASC, id ASC")
    fun observeRounds(): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds ORDER BY position ASC, id ASC")
    suspend fun getRounds(): List<RoundEntity>

    @Insert
    suspend fun insertRound(round: RoundEntity): Long

    @Update
    suspend fun updateRound(round: RoundEntity)

    @Delete
    suspend fun deleteRound(round: RoundEntity)

    @Query("UPDATE rounds SET position = :position WHERE id = :roundId")
    suspend fun updateRoundPosition(roundId: Long, position: Int)

    // ---------------------------------------------------------------------
    // Scores
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM scores")
    fun observeScores(): Flow<List<ScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScore(score: ScoreEntity)

    @Query("DELETE FROM scores WHERE teamId = :teamId AND roundId = :roundId")
    suspend fun deleteScore(teamId: Long, roundId: Long)

    /** Clears every entered score. Teams and rounds are untouched. */
    @Query("DELETE FROM scores")
    suspend fun clearAllScores()

    /** How many teams have a score recorded for a round. */
    @Query("SELECT COUNT(*) FROM scores WHERE roundId = :roundId")
    suspend fun countScoresForRound(roundId: Long): Int

    // ---------------------------------------------------------------------
    // Prizes
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM prizes ORDER BY position ASC, id ASC")
    fun observePrizes(): Flow<List<PrizeEntity>>

    @Query("SELECT * FROM prizes ORDER BY position ASC, id ASC")
    suspend fun getPrizes(): List<PrizeEntity>

    @Insert
    suspend fun insertPrize(prize: PrizeEntity): Long

    @Update
    suspend fun updatePrize(prize: PrizeEntity)

    @Delete
    suspend fun deletePrize(prize: PrizeEntity)

    @Query("UPDATE prizes SET position = :position WHERE id = :prizeId")
    suspend fun updatePrizePosition(prizeId: Long, position: Int)

    // ---------------------------------------------------------------------
    // Tie-breaks
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM tiebreaks")
    fun observeTieBreaks(): Flow<List<TieBreakEntity>>

    @Query("SELECT * FROM tiebreaks WHERE rank = :rank")
    suspend fun getTieBreak(rank: Int): TieBreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTieBreak(tieBreak: TieBreakEntity)

    @Query("DELETE FROM tiebreaks WHERE rank = :rank")
    suspend fun deleteTieBreak(rank: Int)

    @Query("SELECT * FROM tiebreak_answers")
    fun observeTieBreakAnswers(): Flow<List<TieBreakAnswerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTieBreakAnswer(answer: TieBreakAnswerEntity)

    @Query("DELETE FROM tiebreak_answers WHERE rank = :rank AND teamId = :teamId")
    suspend fun deleteTieBreakAnswer(rank: Int, teamId: Long)

    /** Drops every tie-break; used when a score changes and the ties may have moved. */
    @Query("DELETE FROM tiebreaks")
    suspend fun clearTieBreaks()

    @Query("DELETE FROM tiebreak_answers")
    suspend fun clearTieBreakAnswers()

    // ---------------------------------------------------------------------
    // Config
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM config WHERE id = ${ConfigEntity.SINGLETON_ID}")
    fun observeConfig(): Flow<ConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: ConfigEntity)
}
