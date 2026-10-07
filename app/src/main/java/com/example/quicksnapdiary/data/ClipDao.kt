package com.example.quicksnapdiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips WHERE dateKey = :dateKey ORDER BY orderIndex ASC")
    fun observeByDate(dateKey: String): Flow<List<Clip>>

    @Query("SELECT * FROM clips WHERE dateKey = :dateKey ORDER BY orderIndex ASC")
    suspend fun getByDate(dateKey: String): List<Clip>

    @Query("SELECT MAX(orderIndex) FROM clips WHERE dateKey = :dateKey")
    suspend fun maxOrder(dateKey: String): Int?

    @Insert
    suspend fun insert(clip: Clip): Long

    @Update
    suspend fun update(clip: Clip)

    @Query("UPDATE clips SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Delete
    suspend fun delete(clip: Clip)
}
