package com.example.ai.summary

import com.example.domain.model.StructuredSummary
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import java.util.Locale

interface LocalSummaryEngine {
    val engineId: String
    val engineVersion: String

    suspend fun summarize(
        originalTranscript: String,
        chineseTranscript: String,
        segments: List<TranscriptSegment>,
        sourceLanguage: SupportedLanguage,
        targetLanguage: SupportedLanguage
    ): StructuredSummary
}

/**
 * Deterministic, zero-internet extractive summary engine.
 * Extracts verbatim salient sentences, keywords, topics, action items, and explicitly stated
 * Bible verse references without fabricating or hallucinating any external content.
 */
class ExtractiveChineseSummaryEngine : LocalSummaryEngine {
    override val engineId: String = "extractive-offline-nlp"
    override val engineVersion: String = "Extractive-Offline-v1.0"

    private val latinStopwords = setOf(
        // English
        "the", "and", "for", "that", "this", "with", "from", "have", "will", "are", "was", "were",
        "been", "they", "their", "what", "when", "where", "which", "about", "there", "your", "you",
        "can", "could", "would", "should", "into", "more", "than", "then", "also", "just", "very",
        // Tagalog
        "ang", "mga", "nang", "para", "ito", "iyan", "iyon", "siya", "nila", "natin", "kami", "tayo",
        "kayo", "sila", "dito", "doon", "dahil", "pero", "kaya", "kung", "kapag", "habang", "yung",
        "lang", "naman", "po", "opo", "hindi", "oo", "may", "mayroon", "wala", "nasa", "saan",
        // Cebuano / Bisaya
        "ang", "mga", "nga", "ug", "sa", "ko", "mo", "niya", "ato", "kita", "kamo", "sila", "kani",
        "kini", "kana", "kadto", "diri", "didto", "tungod", "apan", "kung", "kon", "pero", "man",
        "lang", "gyud", "jud", "dili", "wala", "naa", "may", "unsa", "asa", "kinsa", "kanus"
    )

    private val actionCuePatterns = listOf(
        // Chinese
        "需要", "必须", "应该", "记得", "安排", "准备", "联系", "确认", "完成", "请", "明天", "下周", "负责",
        // English
        "need to", "must", "should", "remember to", "prepare", "schedule", "follow up", "action", "complete", "send", "call",
        // Tagalog
        "kailangan", "dapat", "gawin", "tandaan", "maghanda", "ayusin", "bukas", "pakisuyo", "ipasa",
        // Cebuano
        "kinahanglan", "buhaton", "himoa", "hinumdomi", "andamon", "ugma", "palihug", "atimanon"
    )

    // Matches explicit Bible verse citations across English, Tagalog, Cebuano, and Chinese
    private val latinBibleRegex = Regex(
        """\b((?:[1-3]\s+)?(?:Genesis|Exodus|Leviticus|Numbers|Deuteronomy|Joshua|Judges|Ruth|Samuel|Kings|Chronicles|Ezra|Nehemiah|Esther|Job|Psalms?|Proverbs|Ecclesiastes|Song of Solomon|Isaiah|Jeremiah|Lamentations|Ezekiel|Daniel|Hosea|Joel|Amos|Obadiah|Jonah|Micah|Nahum|Habakkuk|Zephaniah|Haggai|Zechariah|Malachi|Matthew|Mark|Luke|John|Acts|Romans|Corinthians|Galatians|Ephesians|Philippians|Colossians|Thessalonians|Timothy|Titus|Philemon|Hebrews|James|Peter|Jude|Revelation|Awit|Salmo|Mateo|Marcos|Lucas|Juan|Mga Gawa|Roma|Corinto|Galacia|Efeso|Filipos|Colosas|Tesalonica|Timoteo|Hebreo|Santiago|Pedro|Pahayag|Bugna|Mga Taga-Roma|Mga Buhat))\s+\d{1,3}:\d{1,3}(?:[-–]\d{1,3})?\b""",
        RegexOption.IGNORE_CASE
    )

    private val chineseBibleRegex = Regex(
        """(?:创世记|出埃及记|利未记|民数记|申命记|约书亚记|士师记|路得记|撒母耳记[上下]|列王纪[上下]|历代志[上下]|以斯拉记|尼希米记|以斯帖记|约伯记|诗篇|箴言|传道书|雅歌|以赛亚书|耶利米书|耶利米哀歌|以西结书|但以理书|何西阿书|约珥书|阿摩司书|俄巴底亚书|约拿书|弥迦书|那鸿书|哈巴谷书|西番雅书|哈该书|撒迦利亚书|玛拉基书|马太福音|马可福音|路加福音|约翰福音|使徒行传|罗马书|哥林多[前后]书|加拉太书|以弗所书|腓立比书|歌罗西书|帖撒罗尼迦[前后]书|提摩太[前后]书|提多书|腓利门书|希伯来书|雅各书|彼得[前后]书|约翰[一二三]书|犹大书|启示录)\s*\d{1,3}\s*(?::|：|章)\s*\d{1,3}(?:\s*[-–至]\s*\d{1,3})?节?"""
    )

    override suspend fun summarize(
        originalTranscript: String,
        chineseTranscript: String,
        segments: List<TranscriptSegment>,
        sourceLanguage: SupportedLanguage,
        targetLanguage: SupportedLanguage
    ): StructuredSummary {
        val cleanOriginal = originalTranscript.trim()
        val cleanChinese = chineseTranscript.trim()
        val combinedText = listOf(cleanOriginal, cleanChinese).filter { it.isNotBlank() }.joinToString("\n")

        if (combinedText.isBlank() && segments.isEmpty()) {
            return StructuredSummary(
                chineseSummary = "暂无可提取的转录文本（离线提取引擎未生成虚构内容）。",
                keywords = emptyList(),
                topics = emptyList(),
                keyPoints = emptyList(),
                actionItems = emptyList(),
                bibleReferences = emptyList(),
                generatedAtMillis = System.currentTimeMillis(),
                engineVersion = engineVersion,
                isExtractiveOnly = true
            )
        }

        val chineseSentences = splitSentences(cleanChinese).ifEmpty {
            segments.mapNotNull { seg -> seg.translatedText.trim().takeIf { it.isNotBlank() } }
        }
        val originalSentences = splitSentences(cleanOriginal).ifEmpty {
            segments.mapNotNull { seg -> seg.originalText.trim().takeIf { it.isNotBlank() } }
        }

        val keywords = extractKeywords(cleanChinese, cleanOriginal)
        val rankedChinese = rankSentences(chineseSentences, keywords)
        val rankedOriginal = rankSentences(originalSentences, keywords)

        val chineseSummaryText = when {
            rankedChinese.isNotEmpty() -> {
                rankedChinese.take(3).joinToString(" ")
            }
            rankedOriginal.isNotEmpty() -> {
                "尚无中文译文段落，以下为原文高权重摘录：\n" +
                    rankedOriginal.take(2).joinToString(" ")
            }
            else -> "暂无足够语句用于生成摘要。"
        }

        val keyPoints = buildList {
            if (segments.isNotEmpty()) {
                val scoredSegments = segments.mapIndexed { idx, seg ->
                    val text = seg.translatedText.ifBlank { seg.originalText }
                    val score = scoreSentence(text, keywords, idx, segments.size)
                    seg to score
                }.sortedByDescending { it.second }.take(4).sortedBy { it.first.startMs }
                for ((seg, _) in scoredSegments) {
                    val primaryLine = seg.translatedText.ifBlank { seg.originalText }.trim()
                    if (primaryLine.isNotBlank()) {
                        add("[${seg.formattedTimestamp}] $primaryLine")
                    }
                }
            } else {
                val fallbackPool = if (rankedChinese.isNotEmpty()) rankedChinese else rankedOriginal
                addAll(fallbackPool.take(4))
            }
        }.distinct()

        val allSentences = (chineseSentences + originalSentences).distinct()
        val actionItems = allSentences.filter { sentence ->
            val lower = sentence.lowercase(Locale.ROOT)
            actionCuePatterns.any { cue -> lower.contains(cue) }
        }.take(5)

        val bibleReferences = buildSet {
            latinBibleRegex.findAll(combinedText).forEach { add(it.value.trim()) }
            chineseBibleRegex.findAll(combinedText).forEach { add(it.value.trim()) }
        }.toList()

        val topics = buildTopics(keywords, bibleReferences, actionItems)

        return StructuredSummary(
            chineseSummary = chineseSummaryText,
            keywords = keywords.take(8),
            topics = topics,
            keyPoints = keyPoints,
            actionItems = actionItems,
            bibleReferences = bibleReferences,
            generatedAtMillis = System.currentTimeMillis(),
            engineVersion = engineVersion,
            isExtractiveOnly = true
        )
    }

    private fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text.split(Regex("(?<=[。！？.!?;；\\n])"))
            .map { it.trim() }
            .filter { it.length >= 4 }
    }

    private fun extractKeywords(chineseText: String, originalText: String): List<String> {
        val counts = linkedMapOf<String, Int>()

        // CJK 2-3 character sequence frequency
        val cjkSegments = Regex("[\\u4e00-\\u9fff]{2,}").findAll(chineseText).map { it.value }
        for (segment in cjkSegments) {
            if (segment.length <= 4) {
                counts[segment] = (counts[segment] ?: 0) + 2
            } else {
                for (i in 0..(segment.length - 2)) {
                    val bigram = segment.substring(i, i + 2)
                    counts[bigram] = (counts[bigram] ?: 0) + 1
                }
            }
        }

        // Latin word frequency (Tagalog / Cebuano / English)
        val latinWords = Regex("[A-Za-zÀ-ÿ]{3,}").findAll(originalText)
            .map { it.value.lowercase(Locale.ROOT) }
            .filter { it !in latinStopwords }
        for (word in latinWords) {
            counts[word] = (counts[word] ?: 0) + 1
        }

        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .map { it.key }
            .distinct()
            .take(10)
    }

    private fun rankSentences(sentences: List<String>, keywords: List<String>): List<String> {
        if (sentences.isEmpty()) return emptyList()
        return sentences.mapIndexed { index, sentence ->
            sentence to scoreSentence(sentence, keywords, index, sentences.size)
        }.sortedByDescending { it.second }
            .map { it.first }
    }

    private fun scoreSentence(
        sentence: String,
        keywords: List<String>,
        index: Int,
        total: Int
    ): Double {
        val lower = sentence.lowercase(Locale.ROOT)
        val keywordHits = keywords.count { kw -> lower.contains(kw.lowercase(Locale.ROOT)) }
        val positionBonus = if (index == 0 || index == total - 1) 1.2 else 0.4
        val actionBonus = if (actionCuePatterns.any { lower.contains(it) }) 1.0 else 0.0
        val lengthScore = (sentence.length.coerceAtMost(80) / 40.0)
        return keywordHits * 2.0 + positionBonus + actionBonus + lengthScore
    }

    private fun buildTopics(
        keywords: List<String>,
        bibleReferences: List<String>,
        actionItems: List<String>
    ): List<String> {
        val topics = mutableListOf<String>()
        if (keywords.isNotEmpty()) {
            topics.add("核心词群: ${keywords.take(3).joinToString(" · ")}")
        }
        if (bibleReferences.isNotEmpty()) {
            topics.add("经文引用 (${bibleReferences.size})")
        }
        if (actionItems.isNotEmpty()) {
            topics.add("行动事项 (${actionItems.size})")
        }
        if (topics.isEmpty()) {
            topics.add("常规对话记录")
        }
        return topics
    }
}
