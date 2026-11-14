package com.example.ai.online

import android.util.Log
import com.example.domain.model.StructuredSummary
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "OnlineAiService"

data class OnlineTranslationResult(
    val translatedText: String,
    val detectedSourceLanguage: String? = null,
    val alternativeSuggestions: List<String> = emptyList()
)

class OnlineAiService {

    // Lazy initialization of Gemini 3.8 Flash models per Firebase AI Logic skill guidelines
    private val translationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.3f
            }
        )
    }

    private val summaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
            }
        )
    }

    /**
     * Translates a text snippet from source language to target language.
     * Specializes in natural dialogue, conversational nuance, and sermon/meeting phrasing
     * (especially Tagalog, Cebuano/Bisaya, English, and Chinese).
     */
    suspend fun translate(
        text: String,
        source: SupportedLanguage,
        target: SupportedLanguage
    ): Result<String> {
        if (text.isBlank()) return Result.success("")
        return try {
            withTimeout(45_000L) {
                val prompt = """
                    You are an expert bilingual simultaneous interpreter and translator (similar to Yuanbao / 元宝 AI 同传).
                    Translate the following text from ${source.displayName} (${source.nativeName}) to ${target.displayName} (${target.nativeName}).
                    
                    Instructions:
                    - Deliver an accurate, fluent, natural translation fitting the context (conversational, sermon, meeting).
                    - If translating to Chinese, provide clear, native Simplified Chinese (简体中文).
                    - Output ONLY the translated text without extra greetings, notes, explanations, or quotes.
                    
                    Text to translate:
                    $text
                """.trimIndent()

                val response = translationModel.generateContent(prompt)
                val translated = response.text?.trim().orEmpty()
                Result.success(translated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Online translation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Streaming simultaneous translation (AI 同传实时流式输出).
     * Yields progressive translated text chunks as they arrive from Gemini Flash.
     */
    fun translateStream(
        text: String,
        source: SupportedLanguage,
        target: SupportedLanguage
    ): Flow<String> = flow {
        if (text.isBlank()) return@flow
        val prompt = """
            You are a professional real-time simultaneous interpreter (AI 同传翻译).
            Translate the following speech transcript from ${source.displayName} to ${target.displayName}.
            Provide only the direct, fluent translation in ${target.displayName}. No preamble or markdown fences.
            
            Source speech:
            $text
        """.trimIndent()

        withTimeout(45_000L) {
            val accumulated = StringBuilder()
            translationModel.generateContentStream(prompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                accumulated.append(chunkText)
                emit(accumulated.toString())
            }
        }
    }

    /**
     * Generates a comprehensive, intelligent Yuanbao-style AI meeting/sermon summary (像元宝一样深度总结).
     * Extracts:
     * 1. 内容概要 (Executive Summary)
     * 2. 核心讨论要点 (Key Discussion Points & Insights)
     * 3. 待办行动指南 (Action Items & Commitments)
     * 4. 关键主题与关键词 (Topics & Keywords)
     * 5. 引用出处及圣经经文 (Scripture References & Quotes)
     */
    suspend fun generateDeepSummary(
        originalTranscript: String,
        chineseTranscript: String,
        segments: List<TranscriptSegment>,
        sourceLanguage: SupportedLanguage,
        targetLanguage: SupportedLanguage
    ): Result<StructuredSummary> {
        val fullContent = buildString {
            if (originalTranscript.isNotBlank()) {
                appendLine("=== 原文/Original (${sourceLanguage.displayName}) ===")
                appendLine(originalTranscript)
                appendLine()
            }
            if (chineseTranscript.isNotBlank()) {
                appendLine("=== 中文记录/Chinese Translation ===")
                appendLine(chineseTranscript)
                appendLine()
            }
            if (segments.isNotEmpty()) {
                appendLine("=== 时间戳字幕片段/Timestamped Segments ===")
                segments.take(50).forEach { seg ->
                    appendLine("[${seg.formattedTimestamp}] ${seg.originalText} -> ${seg.translatedText}")
                }
            }
        }.trim()

        if (fullContent.isBlank()) {
            return Result.failure(IllegalArgumentException("无可供总结的记录内容。请先录音或输入文本。"))
        }

        return try {
            withTimeout(60_000L) {
                val prompt = """
                    你是一位顶级 AI 智能会议与讲道记录专家（类似于腾讯元宝 AI 助手）。
                    请针对以下录音记录进行全面、深入且结构化的中文智能总结。
                    
                    输入记录内容：
                    $fullContent
                    
                    请务必严格按照以下 JSON 格式返回结果，不要添加任何 markdown 代码块或 ```json 标记：
                    {
                      "chineseSummary": "全面深入的内容概要（200-300字，高度凝练，突出中心思想）",
                      "keywords": ["关键词1", "关键词2", "关键词3", "关键词4", "关键词5"],
                      "topics": ["议题一：...", "议题二：...", "议题三：..."],
                      "keyPoints": ["核心要点1（结合时间戳或关键讨论）", "核心要点2", "核心要点3", "核心要点4"],
                      "actionItems": ["明确的行动事项或后续落实1", "行动事项2"],
                      "bibleReferences": ["引用的圣经经文1（如：约翰福音 3:16 / John 3:16）", "若未引用经文则为空列表 []"]
                    }
                """.trimIndent()

                val response = summaryModel.generateContent(prompt)
                val rawText = response.text?.trim().orEmpty()
                val jsonString = if (rawText.startsWith("```")) {
                    rawText.substringAfter("\n").substringBeforeLast("```").trim()
                } else {
                    rawText
                }

                val json = JSONObject(jsonString)
                val summary = json.optString("chineseSummary", "AI 总结已完成。")
                val keywords = json.optJSONArray("keywords").toStringList()
                val topics = json.optJSONArray("topics").toStringList()
                val keyPoints = json.optJSONArray("keyPoints").toStringList()
                val actionItems = json.optJSONArray("actionItems").toStringList()
                val bibleRefs = json.optJSONArray("bibleReferences").toStringList()

                val result = StructuredSummary(
                    chineseSummary = summary,
                    keywords = keywords,
                    topics = topics,
                    keyPoints = keyPoints,
                    actionItems = actionItems,
                    bibleReferences = bibleRefs,
                    generatedAtMillis = System.currentTimeMillis(),
                    engineVersion = "Gemini-3.8-Flash (Yuanbao-Style AI)",
                    isExtractiveOnly = false
                )
                Result.success(result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Online summary generation failed", e)
            Result.failure(e)
        }
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) {
                val item = optString(i, "")
                if (item.isNotBlank()) add(item)
            }
        }
    }
}
