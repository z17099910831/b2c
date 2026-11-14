package com.example.ai.translation

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.example.domain.model.SupportedLanguage
import java.io.File

data class OnnxCompatibilityReport(
    val supportedAbis: List<String>,
    val is64BitArm: Boolean,
    val totalRamMb: Long,
    val meetsQuantizedInt8RamRequirement: Boolean,
    val nnapiAvailable: Boolean,
    val onnxRuntimeClassLinked: Boolean,
    val encoderModelPresent: Boolean,
    val decoderModelPresent: Boolean,
    val tokenizerModelPresent: Boolean
) {
    val isModelBundleInstalled: Boolean
        get() = encoderModelPresent && decoderModelPresent && tokenizerModelPresent

    val isFullyReadyForOfflineInference: Boolean
        get() = isModelBundleInstalled && onnxRuntimeClassLinked && meetsQuantizedInt8RamRequirement
}

sealed interface TranslationResult {
    data class Translated(
        val translatedText: String,
        val sourceFloresCode: String,
        val targetFloresCode: String,
        val engineVersion: String
    ) : TranslationResult

    data class EngineUnavailable(val reason: String) : TranslationResult
}

interface LocalTranslationEngine {
    val engineId: String
    val engineName: String
    val engineVersion: String

    suspend fun evaluateCompatibility(modelDir: File?): OnnxCompatibilityReport
    suspend fun initialize(modelDir: File): Result<Unit>
    suspend fun translate(
        text: String,
        source: SupportedLanguage,
        target: SupportedLanguage
    ): TranslationResult
    suspend fun release()
}

/**
 * Modular NLLB-200 Distilled 600M (ONNX INT8 quantized) engine adapter.
 * Evaluates real device ABI, RAM, NNAPI, and ONNX model artifacts without fabricating translations.
 */
class NllbOnnxEngineAdapter(
    private val context: Context
) : LocalTranslationEngine {
    override val engineId: String = "nllb-200-600m-onnx-int8"
    override val engineName: String = "NLLB-200 Distilled 600M (ONNX INT8)"
    override val engineVersion: String = "nllb-200-600m-int8-v1.0"

    private var activeModelDir: File? = null

    private val onnxRuntimeLinked: Boolean by lazy {
        try {
            Class.forName("ai.onnxruntime.OrtEnvironment")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    override suspend fun evaluateCompatibility(modelDir: File?): OnnxCompatibilityReport {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024L * 1024L)
        val abis = Build.SUPPORTED_ABIS.toList()
        val isArm64 = abis.any { it.contains("arm64-v8a", ignoreCase = true) }

        val encoderPresent = modelDir?.let { File(it, "encoder_model_quantized.onnx").exists() } == true
        val decoderPresent = modelDir?.let { File(it, "decoder_model_quantized.onnx").exists() } == true
        val tokenizerPresent = modelDir?.let { File(it, "sentencepiece.bpe.model").exists() } == true

        return OnnxCompatibilityReport(
            supportedAbis = abis,
            is64BitArm = isArm64,
            totalRamMb = totalRamMb,
            meetsQuantizedInt8RamRequirement = totalRamMb >= 3072L,
            nnapiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1,
            onnxRuntimeClassLinked = onnxRuntimeLinked,
            encoderModelPresent = encoderPresent,
            decoderModelPresent = decoderPresent,
            tokenizerModelPresent = tokenizerPresent
        )
    }

    override suspend fun initialize(modelDir: File): Result<Unit> {
        val report = evaluateCompatibility(modelDir)
        if (!report.isModelBundleInstalled) {
            return Result.failure(
                IllegalStateException("NLLB-200 quantized ONNX files or SentencePiece tokenizer are not installed.")
            )
        }
        if (!report.onnxRuntimeClassLinked) {
            return Result.failure(
                IllegalStateException("ONNX Runtime Mobile AAR is not linked in this build.")
            )
        }
        activeModelDir = modelDir
        return Result.success(Unit)
    }

    override suspend fun translate(
        text: String,
        source: SupportedLanguage,
        target: SupportedLanguage
    ): TranslationResult {
        if (text.isBlank()) {
            return TranslationResult.EngineUnavailable("No source text provided.")
        }
        val report = evaluateCompatibility(activeModelDir)
        if (!report.isModelBundleInstalled) {
            return TranslationResult.EngineUnavailable(
                "NLLB-200 600M model is not installed. Download models in Offline Model Manager first."
            )
        }
        if (!report.onnxRuntimeClassLinked) {
            return TranslationResult.EngineUnavailable(
                "NLLB-200 ONNX adapter ready (${source.nllbCode} → ${target.nllbCode}), awaiting com.microsoft.onnxruntime:onnxruntime-android native linking."
            )
        }
        return TranslationResult.EngineUnavailable(
            "ONNX session initialization hook ready for ${source.nllbCode} → ${target.nllbCode}."
        )
    }

    override suspend fun release() {
        activeModelDir = null
    }
}

class TranslationEngineRegistry(
    initialEngine: LocalTranslationEngine
) {
    private var currentEngine: LocalTranslationEngine = initialEngine

    fun getActiveEngine(): LocalTranslationEngine = currentEngine

    suspend fun replaceEngine(newEngine: LocalTranslationEngine) {
        currentEngine.release()
        currentEngine = newEngine
    }
}
