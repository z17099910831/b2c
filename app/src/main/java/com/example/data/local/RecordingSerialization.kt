package com.example.data.local

import com.example.domain.model.StructuredSummary
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import org.json.JSONArray
import org.json.JSONObject

object RecordingSerialization {
    fun encodeSegments(segments: List<TranscriptSegment>): String {
        val array = JSONArray()
        for (segment in segments) {
            val obj = JSONObject().apply {
                put("id", segment.id)
                put("startMs", segment.startMs)
                put("endMs", segment.endMs)
                put("originalText", segment.originalText)
                put("translatedText", segment.translatedText)
                put("sourceLanguage", segment.sourceLanguage.code)
                put("targetLanguage", segment.targetLanguage.code)
                put("isFinal", segment.isFinal)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeSegments(json: String): List<TranscriptSegment> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        TranscriptSegment(
                            id = obj.optString("id", "seg_$i"),
                            startMs = obj.optLong("startMs", 0L),
                            endMs = obj.optLong("endMs", 0L),
                            originalText = obj.optString("originalText", ""),
                            translatedText = obj.optString("translatedText", ""),
                            sourceLanguage = SupportedLanguage.fromCode(
                                obj.optString("sourceLanguage", SupportedLanguage.TAGALOG.code)
                            ),
                            targetLanguage = SupportedLanguage.fromCode(
                                obj.optString("targetLanguage", SupportedLanguage.SIMPLIFIED_CHINESE.code)
                            ),
                            isFinal = obj.optBoolean("isFinal", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeSummary(summary: StructuredSummary): String {
        val obj = JSONObject().apply {
            put("chineseSummary", summary.chineseSummary)
            put("keywords", JSONArray(summary.keywords))
            put("topics", JSONArray(summary.topics))
            put("keyPoints", JSONArray(summary.keyPoints))
            put("actionItems", JSONArray(summary.actionItems))
            put("bibleReferences", JSONArray(summary.bibleReferences))
            put("generatedAtMillis", summary.generatedAtMillis)
            put("engineVersion", summary.engineVersion)
            put("isExtractiveOnly", summary.isExtractiveOnly)
        }
        return obj.toString()
    }

    fun decodeSummary(json: String): StructuredSummary? {
        if (json.isBlank()) return null
        return runCatching {
            val obj = JSONObject(json)
            StructuredSummary(
                chineseSummary = obj.optString("chineseSummary", ""),
                keywords = obj.optJSONArray("keywords").toStringList(),
                topics = obj.optJSONArray("topics").toStringList(),
                keyPoints = obj.optJSONArray("keyPoints").toStringList(),
                actionItems = obj.optJSONArray("actionItems").toStringList(),
                bibleReferences = obj.optJSONArray("bibleReferences").toStringList(),
                generatedAtMillis = obj.optLong("generatedAtMillis", 0L),
                engineVersion = obj.optString("engineVersion", "Extractive-v1"),
                isExtractiveOnly = obj.optBoolean("isExtractiveOnly", true)
            )
        }.getOrNull()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) {
                val item = optString(i, "")
                if (item.isNotBlank()) add(item)
            }
        }
    }
}
