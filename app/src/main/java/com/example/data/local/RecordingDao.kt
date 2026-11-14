package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAtMillis DESC")
    fun observeAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    fun observeRecordingById(id: String): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: String): RecordingEntity?

    @Query(
        """
        SELECT * FROM recordings
        WHERE title LIKE '%' || :query || '%'
           OR originalTranscript LIKE '%' || :query || '%'
           OR chineseTranslation LIKE '%' || :query || '%'
        ORDER BY createdAtMillis DESC
        """
    )
    fun searchRecordings(query: String): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)
}
