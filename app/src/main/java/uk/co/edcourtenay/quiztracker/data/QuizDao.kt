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

    @Query("SELECT * FROM teams ORDER BY createdAt ASC, id ASC")
    fun observeTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams ORDER BY createdAt ASC, id ASC")
    suspend fun getTeams(): List<TeamEntity>

    @Insert
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    @Query("DELETE FROM teams")
    suspend fun deleteAllTeams()

    /** Clears every entered score but keeps the team names and player counts. */
    @Query("UPDATE teams SET rounds1to4 = NULL, rounds5to6 = NULL, wipeout = NULL")
    suspend fun clearAllScores()

    @Query("SELECT * FROM config WHERE id = ${ConfigEntity.SINGLETON_ID}")
    fun observeConfig(): Flow<ConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: ConfigEntity)
}
