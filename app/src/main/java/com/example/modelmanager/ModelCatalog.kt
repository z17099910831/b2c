package com.example.modelmanager

enum class ModelKind {
    SPEECH_RECOGNITION,
    TRANSLATION,
    VOICE_ACTIVITY,
    TOKENIZER
}

enum class ModelTier(val label: String, val badgeText: String) {
    LIGHTWEIGHT("Lightweight (Low RAM)", "LIGHTWEIGHT"),
    STANDARD("Standard (Mid-Range)", "STANDARD"),
    HIGH_PERFORMANCE("High Performance (6GB+ RAM)", "HIGH-PERF")
}

data class OfflineModelSpec(
    val id: String,
    val name: String,
    val kind: ModelKind,
    val tier: ModelTier = ModelTier.STANDARD,
    val version: String,
    val sizeBytes: Long,
    val minRamMb: Long,
    val licenseName: String,
    val expectedSha256: String,
    val downloadUrl: String,
    val fileName: String,
    val supportedLanguages: String,
    val isRequiredForCoreOffline: Boolean,
    val notes: String
) {
    val sizeFormatted: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                "%.2f GB".format(mb / 1024.0)
            } else {
                "%.0f MB".format(mb)
            }
        }
}

sealed interface ModelInstallState {
    data object NotInstalled : ModelInstallState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long, val progress: Float) : ModelInstallState
    data object VerifyingChecksum : ModelInstallState
    data class Installed(
        val filePath: String,
        val installedBytes: Long,
        val verifiedSha256: String
    ) : ModelInstallState
    data class Error(val message: String) : ModelInstallState
}

data class ManagedModelItem(
    val spec: OfflineModelSpec,
    val state: ModelInstallState,
    val meetsRamRequirement: Boolean,
    val meetsStorageRequirement: Boolean
)

object OfflineModelCatalog {
    // Adaptive offline catalog spanning Lightweight (<1.5 GB RAM), Standard (3 GB+ RAM), and High-Performance (6 GB+ RAM).
    val models: List<OfflineModelSpec> = listOf(
        OfflineModelSpec(
            id = "whisper-tiny-q5_1",
            name = "Whisper Tiny Multilingual (Q5_1)",
            kind = ModelKind.SPEECH_RECOGNITION,
            tier = ModelTier.LIGHTWEIGHT,
            version = "v1.5.0-ggml",
            sizeBytes = 33_554_432L, // ~32 MB
            minRamMb = 1536L,
            licenseName = "MIT License",
            expectedSha256 = "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
            fileName = "ggml-tiny-q5_1.bin",
            supportedLanguages = "Tagalog (tl), Cebuano (ceb), English (en), Chinese (zh)",
            isRequiredForCoreOffline = false,
            notes = "Lightweight GGML model tailored for 2–3 GB RAM phones and legacy Android 7–10 devices."
        ),
        OfflineModelSpec(
            id = "whisper-small-q5_1",
            name = "Whisper Small Multilingual (Q5_1)",
            kind = ModelKind.SPEECH_RECOGNITION,
            tier = ModelTier.STANDARD,
            version = "v1.5.0-ggml",
            sizeBytes = 545_259_520L, // ~520 MB
            minRamMb = 3072L,
            licenseName = "MIT License",
            expectedSha256 = "a85e4a935d73b221008d5bfa78f14b621e25e902b48971f11c761e8682a86532",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
            fileName = "ggml-small-q5_1.bin",
            supportedLanguages = "Tagalog (tl), Cebuano (ceb), English (en), Chinese (zh)",
            isRequiredForCoreOffline = true,
            notes = "Standard quantized GGML model balanced for mid-range Android phones and tablets."
        ),
        OfflineModelSpec(
            id = "nllb-200-distilled-600m-int8",
            name = "NLLB-200 Distilled 600M (ONNX INT8)",
            kind = ModelKind.TRANSLATION,
            tier = ModelTier.STANDARD,
            version = "v1.0.0-int8",
            sizeBytes = 1_027_604_480L, // ~980 MB
            minRamMb = 4096L,
            licenseName = "CC-BY-NC-4.0",
            expectedSha256 = "7a1d4f92c83a6d44b019e4e2a98b1130e27c53c2f86b2a669f14a56f9b6f0e11",
            downloadUrl = "https://huggingface.co/Xenova/nllb-200-distilled-600M/resolve/main/onnx/decoder_model_quantized.onnx",
            fileName = "decoder_model_quantized.onnx",
            supportedLanguages = "tgl_Latn, ceb_Latn, zho_Hans, eng_Latn",
            isRequiredForCoreOffline = true,
            notes = "INT8 quantized ONNX weights for offline Tagalog/Cebuano ↔ Simplified Chinese translation."
        ),
        OfflineModelSpec(
            id = "nllb-sentencepiece-bpe",
            name = "NLLB-200 SentencePiece BPE Tokenizer",
            kind = ModelKind.TOKENIZER,
            tier = ModelTier.LIGHTWEIGHT,
            version = "v2.0.0-sp",
            sizeBytes = 18_874_368L, // ~18 MB
            minRamMb = 1024L,
            licenseName = "CC-BY-NC-4.0 / Apache-2.0",
            expectedSha256 = "9f11f74c6cf2284e82b47c5d2360c0b8926243952f59a47c3de3554b1067bb18",
            downloadUrl = "https://huggingface.co/facebook/nllb-200-distilled-600M/resolve/main/sentencepiece.bpe.model",
            fileName = "sentencepiece.bpe.model",
            supportedLanguages = "200 FLORES-200 languages incl. tgl_Latn, ceb_Latn, zho_Hans",
            isRequiredForCoreOffline = true,
            notes = "Subword tokenizer required alongside NLLB-200 ONNX weights."
        ),
        OfflineModelSpec(
            id = "silero-vad-v5",
            name = "Silero VAD Chunk Segmenter",
            kind = ModelKind.VOICE_ACTIVITY,
            tier = ModelTier.LIGHTWEIGHT,
            version = "v5.1.2-onnx",
            sizeBytes = 4_194_304L, // ~4 MB
            minRamMb = 1024L,
            licenseName = "MIT License",
            expectedSha256 = "c2b8f6e800a4489056c2a8c28e50473bc8b6360442fb7c72c5fa3952d05900c7",
            downloadUrl = "https://github.com/snakers4/silero-vad/raw/master/src/silero_vad/data/silero_vad.onnx",
            fileName = "silero_vad.onnx",
            supportedLanguages = "Language-agnostic speech segmentation",
            isRequiredForCoreOffline = false,
            notes = "Lightweight VAD filter preventing silent chunks from consuming battery."
        ),
        OfflineModelSpec(
            id = "whisper-medium-q4_k",
            name = "Whisper Medium Multilingual (Q4_K)",
            kind = ModelKind.SPEECH_RECOGNITION,
            tier = ModelTier.HIGH_PERFORMANCE,
            version = "v1.5.0-ggml",
            sizeBytes = 1_449_551_462L, // ~1.35 GB
            minRamMb = 6144L,
            licenseName = "MIT License",
            expectedSha256 = "4d2e93a7d1e845cb2b9e33b11ef08a8f494923e88721b5c085d87e2ad099a1b3",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q4_k.bin",
            fileName = "ggml-medium-q4_k.bin",
            supportedLanguages = "Higher-accuracy Tagalog, Cebuano, English, Chinese",
            isRequiredForCoreOffline = false,
            notes = "High-performance model recommended for 6 GB+ RAM tablets and flagship devices."
        )
    )
}
