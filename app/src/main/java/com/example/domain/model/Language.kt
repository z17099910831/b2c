package com.example.domain.model

enum class SupportedLanguage(
    val code: String,
    val nllbCode: String,
    val whisperCode: String,
    val displayName: String,
    val nativeName: String,
    val shortTag: String
) {
    TAGALOG(
        code = "tl",
        nllbCode = "tgl_Latn",
        whisperCode = "tl",
        displayName = "Tagalog",
        nativeName = "Tagalog / Filipino",
        shortTag = "TL"
    ),
    CEBUANO(
        code = "ceb",
        nllbCode = "ceb_Latn",
        whisperCode = "ceb",
        displayName = "Cebuano / Bisaya",
        nativeName = "Binisaya",
        shortTag = "CEB"
    ),
    SIMPLIFIED_CHINESE(
        code = "zh",
        nllbCode = "zho_Hans",
        whisperCode = "zh",
        displayName = "Simplified Chinese",
        nativeName = "简体中文",
        shortTag = "ZH"
    ),
    ENGLISH(
        code = "en",
        nllbCode = "eng_Latn",
        whisperCode = "en",
        displayName = "English",
        nativeName = "English",
        shortTag = "EN"
    );

    companion object {
        fun fromCode(code: String): SupportedLanguage {
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true) ||
                    it.nllbCode.equals(code, ignoreCase = true)
            } ?: TAGALOG
        }
    }
}

data class TranslationDirection(
    val source: SupportedLanguage,
    val target: SupportedLanguage,
    val isPrimary: Boolean,
    val subtitle: String
) {
    val label: String get() = "${source.displayName} → ${target.displayName}"
    val nllbPairLabel: String get() = "${source.nllbCode} → ${target.nllbCode}"
}

object SupportedTranslationPairs {
    val allDirections: List<TranslationDirection> = listOf(
        TranslationDirection(
            source = SupportedLanguage.TAGALOG,
            target = SupportedLanguage.SIMPLIFIED_CHINESE,
            isPrimary = true,
            subtitle = "Primary • Whisper TL + NLLB tgl_Latn→zho_Hans"
        ),
        TranslationDirection(
            source = SupportedLanguage.CEBUANO,
            target = SupportedLanguage.SIMPLIFIED_CHINESE,
            isPrimary = true,
            subtitle = "Primary • Whisper CEB + NLLB ceb_Latn→zho_Hans"
        ),
        TranslationDirection(
            source = SupportedLanguage.ENGLISH,
            target = SupportedLanguage.SIMPLIFIED_CHINESE,
            isPrimary = false,
            subtitle = "Secondary • Whisper EN + NLLB eng_Latn→zho_Hans"
        ),
        TranslationDirection(
            source = SupportedLanguage.SIMPLIFIED_CHINESE,
            target = SupportedLanguage.TAGALOG,
            isPrimary = false,
            subtitle = "Secondary • Whisper ZH + NLLB zho_Hans→tgl_Latn"
        ),
        TranslationDirection(
            source = SupportedLanguage.SIMPLIFIED_CHINESE,
            target = SupportedLanguage.CEBUANO,
            isPrimary = false,
            subtitle = "Secondary • Whisper ZH + NLLB zho_Hans→ceb_Latn"
        )
    )
}
