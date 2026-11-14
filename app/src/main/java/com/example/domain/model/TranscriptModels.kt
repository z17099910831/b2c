package com.example.domain.model

data class TranscriptSegment(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: SupportedLanguage,
    val targetLanguage: SupportedLanguage,
    val isFinal: Boolean = true
) {
    val formattedTimestamp: String
        get() = formatMillis(startMs)

    companion object {
        fun formatMillis(ms: Long): String {
            val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}

data class StructuredSummary(
    val chineseSummary: String,
    val keywords: List<String>,
    val topics: List<String>,
    val keyPoints: List<String>,
    val actionItems: List<String>,
    val bibleReferences: List<String>,
    val generatedAtMillis: Long,
    val engineVersion: String,
    val isExtractiveOnly: Boolean = true
) {
    val isEmpty: Boolean
        get() = chineseSummary.isBlank() &&
            keywords.isEmpty() &&
            topics.isEmpty() &&
            keyPoints.isEmpty() &&
            actionItems.isEmpty() &&
            bibleReferences.isEmpty()
}

data class DevelopmentStageItem(
    val stageNumber: Int,
    val title: String,
    val detail: String,
    val status: StageStatus
)

enum class StageStatus {
    IMPLEMENTED,
    INTERFACE_READY,
    PENDING_ON_DEVICE_MODELS
}
