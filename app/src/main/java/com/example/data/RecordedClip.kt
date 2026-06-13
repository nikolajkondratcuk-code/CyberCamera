package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recorded_clips")
data class RecordedClip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filename: String,
    val durationSeconds: Int,
    val timestamp: Long,
    val sizeBytes: Long,
    val resolution: String,
    val frameRate: Int,
    val codec: String,
    val isoUsed: Int,
    val shutterUsed: String,
    val whiteBalanceUsed: Int,
    val tintUsed: Int,
    val lutUsed: String,
    val focalDistUsed: Float,
    val isPhoto: Boolean = false,
    val cameraMode: String = "PHOTO"
)
