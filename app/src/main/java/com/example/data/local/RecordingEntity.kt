package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAtMillis: Long,
    val durationMillis: Long,
    val audioFilePath: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val originalTranscript: String,
    val chineseTranslation: String,
    val timestampsJson: String,
    val summaryJson: String,
    val modelVersion: String
)
