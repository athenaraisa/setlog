package com.example.quicksnapdiary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface VlogDao {
    @Query("SELECT * FROM vlogs ORDER BY dateKey DESC")
    fun observeAll(): Flow<List<Vlog>>

    @Query("SELECT * FROM vlogs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDate(dateKey: String): Vlog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vlog: Vlog): Long

    @Delete
    suspend fun delete(vlog: Vlog)
}
