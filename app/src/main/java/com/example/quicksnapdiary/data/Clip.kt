package com.example.quicksnapdiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clips")
data class Clip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val timestamp: Long,
    val filePath: String,
    val durationMs: Long,
    val hasAudio: Boolean,
    val orderIndex: Int,
    val note: String = "",
    val videoRotation: Int = 0
)
