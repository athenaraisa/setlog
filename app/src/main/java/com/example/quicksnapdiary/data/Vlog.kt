package com.example.quicksnapdiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vlogs")
data class Vlog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val filePath: String,
    val clipCount: Int,
    val durationMs: Long,
    val createdAt: Long
)
