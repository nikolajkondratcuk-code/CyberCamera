package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordedClipDao {
    @Query("SELECT * FROM recorded_clips ORDER BY timestamp DESC")
    fun getAllClips(): Flow<List<RecordedClip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: RecordedClip)

    @Delete
    suspend fun deleteClip(clip: RecordedClip)

    @Query("DELETE FROM recorded_clips WHERE id = :id")
    suspend fun deleteClipById(id: Int)

    @Query("DELETE FROM recorded_clips")
    suspend fun clearAllClips()
}
