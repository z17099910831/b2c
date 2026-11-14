package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.ai.online.OnlineAiService
import com.example.ai.speech.LocalSpeechRecognizer
import com.example.ai.speech.RecognizerReadiness
import com.example.ai.speech.SpeechChunkResult
import com.example.ai.summary.LocalSummaryEngine
import com.example.ai.translation.LocalTranslationEngine
import com.example.ai.translation.OnnxCompatibilityReport
import com.example.audio.AudioPlaybackState
import com.example.audio.AudioRecordingState
import com.example.audio.LocalAudioPlayer
import com.example.audio.LocalAudioRecorder
import com.example.data.local.RecordingEntity
import com.example.data.local.RecordingRepository
import com.example.data.local.RecordingSerialization
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.domain.model.DevelopmentStageItem
import com.example.domain.model.StageStatus
import com.example.domain.model.StructuredSummary
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import com.example.domain.model.TranslationDirection
import com.example.export.TranscriptExporter
import com.example.modelmanager.DeviceHardwareStatus
import com.example.modelmanager.ManagedModelItem
import com.example.modelmanager.OfflineModelManager
import com.example.modelmanager.OfflineReadinessSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class LiveInterpreterUiState(
    val activeRecordingId: String? = null,
    val recordingTitle: String = "",
    val recordingState: AudioRecordingState = AudioRecordingState.IDLE,
    val elapsedMillis: Long = 0L,
    val amplitudeBars: List<Float> = List(48) { 0.04f },
    val liveSegments: List<TranscriptSegment> = emptyList(),
    val chunksCaptured: Int = 0,
    val engineDiagnostic: String = "Ready. Tap microphone or use AI simultaneous interpretation.",
    val statusNotice: String? = null,
    val isInterpretingOnline: Boolean = false
)

data class OnlineTranslationState(
    val inputText: String = "",
    val translatedText: String = "",
    val isTranslating: Boolean = false,
    val streamingChunks: String = "",
    val errorMessage: String? = null
)

data class ReadinessUiState(
    val speechReadiness: RecognizerReadiness = RecognizerReadiness.ModelsNotInstalled("Not checked"),
    val onnxReport: OnnxCompatibilityReport? = null,
    val summary: OfflineReadinessSummary = OfflineReadinessSummary(
        speechModelInstalled = false,
        translationModelInstalled = false,
        tokenizerInstalled = false,
        nativeSpeechEngineLinked = false,
        nativeOnnxEngineLinked = false,
        installedModelBytes = 0L
    )
)

class MainViewModel(
    private val recordingRepository: RecordingRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val modelManager: OfflineModelManager,
    private val audioRecorder: LocalAudioRecorder,
    private val audioPlayer: LocalAudioPlayer,
    private val speechRecognizer: LocalSpeechRecognizer,
    private val translationEngine: LocalTranslationEngine,
    private val summaryEngine: LocalSummaryEngine,
    private val onlineAiService: OnlineAiService,
    private val exporter: TranscriptExporter
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val hardwareStatus: StateFlow<DeviceHardwareStatus> = modelManager.hardwareStatus
    val managedModels: StateFlow<List<ManagedModelItem>> = modelManager.models
    val playbackState: StateFlow<AudioPlaybackState> = audioPlayer.playbackState

    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val recordings: StateFlow<List<RecordingEntity>> = _librarySearchQuery
        .flatMapLatest { query -> recordingRepository.searchRecordings(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRecordingId = MutableStateFlow<String?>(null)
    val selectedRecordingId: StateFlow<String?> = _selectedRecordingId.asStateFlow()

    val selectedRecording: StateFlow<RecordingEntity?> = combine(
        recordings,
        _selectedRecordingId
    ) { list, selectedId ->
        if (selectedId != null) {
            list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
        } else {
            list.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _readinessState = MutableStateFlow(ReadinessUiState())
    val readinessState: StateFlow<ReadinessUiState> = _readinessState.asStateFlow()

    private val _liveState = MutableStateFlow(LiveInterpreterUiState())
    val liveState: StateFlow<LiveInterpreterUiState> = _liveState.asStateFlow()

    private val _onlineTranslationState = MutableStateFlow(OnlineTranslationState())
    val onlineTranslationState: StateFlow<OnlineTranslationState> = _onlineTranslationState.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private val _showConsentDialog = MutableStateFlow(false)
    val showConsentDialog: StateFlow<Boolean> = _showConsentDialog.asStateFlow()

    private val _pendingDownloadModel = MutableStateFlow<ManagedModelItem?>(null)
    val pendingDownloadModel: StateFlow<ManagedModelItem?> = _pendingDownloadModel.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val developmentStages: List<DevelopmentStageItem> = listOf(
        DevelopmentStageItem(1, "Native UI & Adaptive Navigation", "Dark navy M3 interface for phone & tablet.", StageStatus.IMPLEMENTED),
        DevelopmentStageItem(2, "Local Audio & Room Storage", "16 kHz WAV capture, focus handling, Room DB.", StageStatus.IMPLEMENTED),
        DevelopmentStageItem(3, "Online Gemini AI & Simultaneous Interpretation", "Yuanbao-style AI interpretation, translation & deep summary.", StageStatus.IMPLEMENTED),
        DevelopmentStageItem(4, "Whisper & NLLB Offline Engines", "Local JNI & ONNX fallback when offline.", StageStatus.IMPLEMENTED),
        DevelopmentStageItem(5, "Offline Extractive Summary", "Zero-internet Chinese summary, keywords, Bible refs.", StageStatus.IMPLEMENTED)
    )

    init {
        observeRecorderStreams()
        observeModelReadiness()
        refreshReadinessDiagnostics()
    }

    private fun observeRecorderStreams() {
        viewModelScope.launch {
            combine(
                audioRecorder.recordingState,
                audioRecorder.elapsedMillis,
                audioRecorder.amplitudeHistory,
                audioRecorder.statusNotice
            ) { state, elapsed, bars, notice ->
                _liveState.value.copy(
                    recordingState = state,
                    elapsedMillis = elapsed,
                    amplitudeBars = bars,
                    statusNotice = notice
                )
            }.collect { updated ->
                _liveState.value = updated
            }
        }

        viewModelScope.launch {
            audioRecorder.audioChunks.collect { chunk ->
                val prefs = userPreferences.value
                val isOnline = prefs.isOnlineAiEnabled && hardwareStatus.value.isInternetConnected

                if (!isOnline) {
                    val result = speechRecognizer.processAudioChunk(
                        chunk = chunk,
                        languageHint = prefs.sourceLanguage,
                        targetLanguage = prefs.targetLanguage
                    )
                    _liveState.update { current ->
                        when (result) {
                            is SpeechChunkResult.Recognized -> current.copy(
                                chunksCaptured = current.chunksCaptured + 1,
                                liveSegments = current.liveSegments + result.segments,
                                engineDiagnostic = "Offline Whisper inference active (${result.segments.size} segment(s))."
                            )
                            is SpeechChunkResult.SilenceOrVadFiltered -> current.copy(
                                chunksCaptured = current.chunksCaptured + 1
                            )
                            is SpeechChunkResult.EngineUnavailable -> current.copy(
                                chunksCaptured = current.chunksCaptured + 1,
                                engineDiagnostic = result.message
                            )
                        }
                    }
                } else {
                    _liveState.update { current ->
                        current.copy(
                            chunksCaptured = current.chunksCaptured + 1,
                            engineDiagnostic = "Online AI Simultaneous Interpreter active (Gemini Flash)."
                        )
                    }
                }
            }
        }
    }

    private fun observeModelReadiness() {
        viewModelScope.launch {
            modelManager.models.collect {
                refreshReadinessDiagnostics()
            }
        }
    }

    fun refreshReadinessDiagnostics() {
        viewModelScope.launch {
            val speechFile = modelManager.getSpeechModelFile()
            val translationDir = modelManager.getTranslationModelDir()
            val speechReadiness = speechRecognizer.checkReadiness(speechFile)
            val onnxReport = translationEngine.evaluateCompatibility(translationDir)
            val summary = modelManager.buildReadinessSummary(
                nativeSpeechLinked = speechReadiness is RecognizerReadiness.Ready,
                nativeOnnxLinked = onnxReport.onnxRuntimeClassLinked
            )
            _readinessState.value = ReadinessUiState(
                speechReadiness = speechReadiness,
                onnxReport = onnxReport,
                summary = summary
            )
        }
    }

    fun setOnlineAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setOnlineAiEnabled(enabled)
            val label = if (enabled) "Switched to Online Cloud AI (Gemini Flash)" else "Switched to 100% Offline Mode"
            postMessage(label)
        }
    }

    fun selectLanguageDirection(direction: TranslationDirection) {
        viewModelScope.launch {
            preferencesRepository.setLanguagePair(direction.source, direction.target)
        }
    }

    fun setSourceLanguage(language: SupportedLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguagePair(language, userPreferences.value.targetLanguage)
        }
    }

    fun setTargetLanguage(language: SupportedLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguagePair(userPreferences.value.sourceLanguage, language)
        }
    }

    fun swapLanguages() {
        val current = userPreferences.value
        viewModelScope.launch {
            preferencesRepository.setLanguagePair(current.targetLanguage, current.sourceLanguage)
        }
    }

    // --- Online Translation & Simultaneous Interpretation (像元宝一样翻译与同传) ---

    fun updateOnlineInputText(text: String) {
        _onlineTranslationState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun executeOnlineTranslation(text: String) {
        if (text.isBlank()) return
        val prefs = userPreferences.value
        viewModelScope.launch {
            _onlineTranslationState.update { it.copy(isTranslating = true, errorMessage = null) }
            val result = onlineAiService.translate(
                text = text.trim(),
                source = prefs.sourceLanguage,
                target = prefs.targetLanguage
            )
            result.onSuccess { translated ->
                _onlineTranslationState.update {
                    it.copy(
                        translatedText = translated,
                        isTranslating = false
                    )
                }
            }.onFailure { err ->
                _onlineTranslationState.update {
                    it.copy(
                        isTranslating = false,
                        errorMessage = err.localizedMessage ?: "Translation failed."
                    )
                }
                postMessage("Translation error: ${err.localizedMessage}")
            }
        }
    }

    /**
     * Sends speech or text to the live simultaneous interpretation feed using Gemini streaming.
     */
    fun sendLiveSpeechForInterpretation(spokenText: String) {
        if (spokenText.isBlank()) return
        val prefs = userPreferences.value
        val startTime = _liveState.value.elapsedMillis
        val segmentId = UUID.randomUUID().toString()

        viewModelScope.launch {
            _liveState.update { it.copy(isInterpretingOnline = true) }
            val accumulated = StringBuilder()

            try {
                onlineAiService.translateStream(
                    text = spokenText.trim(),
                    source = prefs.sourceLanguage,
                    target = prefs.targetLanguage
                ).collect { partial ->
                    accumulated.clear()
                    accumulated.append(partial)
                    val endTime = _liveState.value.elapsedMillis.coerceAtLeast(startTime + 1500L)
                    val activeSeg = TranscriptSegment(
                        id = segmentId,
                        startMs = startTime,
                        endMs = endTime,
                        originalText = spokenText.trim(),
                        translatedText = partial,
                        sourceLanguage = prefs.sourceLanguage,
                        targetLanguage = prefs.targetLanguage,
                        isFinal = false
                    )
                    _liveState.update { curr ->
                        val updated = curr.liveSegments.filterNot { it.id == segmentId } + activeSeg
                        curr.copy(liveSegments = updated)
                    }
                }

                // Finalize segment
                val finalEndTime = _liveState.value.elapsedMillis.coerceAtLeast(startTime + 2000L)
                val finalSeg = TranscriptSegment(
                    id = segmentId,
                    startMs = startTime,
                    endMs = finalEndTime,
                    originalText = spokenText.trim(),
                    translatedText = accumulated.toString().ifBlank { spokenText.trim() },
                    sourceLanguage = prefs.sourceLanguage,
                    targetLanguage = prefs.targetLanguage,
                    isFinal = true
                )
                _liveState.update { curr ->
                    val updated = curr.liveSegments.filterNot { it.id == segmentId } + finalSeg
                    curr.copy(
                        liveSegments = updated,
                        isInterpretingOnline = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _liveState.update { it.copy(isInterpretingOnline = false) }
                postMessage("AI 同传错误: ${e.localizedMessage}")
            }
        }
    }

    // --- Recording Session Lifecycle ---

    fun requestRecordingStart(hasMicPermission: Boolean, onNeedMicPermission: () -> Unit) {
        if (!hasMicPermission) {
            onNeedMicPermission()
            return
        }
        if (!userPreferences.value.recordingConsentAccepted) {
            _showConsentDialog.value = true
            return
        }
        startRecordingSession()
    }

    fun acceptRecordingConsent(hasMicPermission: Boolean, onNeedMicPermission: () -> Unit) {
        _showConsentDialog.value = false
        viewModelScope.launch {
            preferencesRepository.setRecordingConsentAccepted(true)
            if (hasMicPermission) {
                startRecordingSession()
            } else {
                onNeedMicPermission()
            }
        }
    }

    fun dismissConsentDialog() {
        _showConsentDialog.value = false
    }

    private fun startRecordingSession() {
        audioPlayer.stop()
        val recordingId = UUID.randomUUID().toString()
        val timeLabel = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
        val prefs = userPreferences.value
        val title = "${prefs.sourceLanguage.shortTag}→${prefs.targetLanguage.shortTag} • $timeLabel"

        val startResult = audioRecorder.startNewRecording(recordingId)
        startResult.onSuccess {
            _liveState.update {
                it.copy(
                    activeRecordingId = recordingId,
                    recordingTitle = title,
                    liveSegments = emptyList(),
                    chunksCaptured = 0,
                    engineDiagnostic = if (prefs.isOnlineAiEnabled) {
                        "Gemini Flash AI Simultaneous Interpretation Ready."
                    } else {
                        "Offline Local Recorder Active."
                    }
                )
            }
        }.onFailure { err ->
            postMessage(err.localizedMessage ?: "Unable to start microphone recording.")
        }
    }

    fun pauseRecording() {
        audioRecorder.pauseRecording()
    }

    fun resumeRecording() {
        audioRecorder.resumeRecording().onFailure { err ->
            postMessage(err.localizedMessage ?: "Unable to resume microphone recording.")
        }
    }

    fun stopAndSaveRecording() {
        val completed = audioRecorder.stopRecording() ?: return
        val prefs = userPreferences.value
        val snapshot = _liveState.value
        val segments = snapshot.liveSegments
        val originalText = segments.joinToString("\n") { it.originalText }.trim()
        val chineseText = segments.joinToString("\n") { it.translatedText }.trim()
        val modelTag = if (prefs.isOnlineAiEnabled) {
            "Gemini-3.8-Flash (Cloud AI)"
        } else {
            "${speechRecognizer.engineVersion} | ${translationEngine.engineVersion}"
        }

        viewModelScope.launch {
            val entity = RecordingEntity(
                id = completed.recordingId,
                title = snapshot.recordingTitle.ifBlank { "Bilingual Session" },
                createdAtMillis = System.currentTimeMillis(),
                durationMillis = completed.durationMillis,
                audioFilePath = completed.filePath,
                sourceLanguageCode = prefs.sourceLanguage.code,
                targetLanguageCode = prefs.targetLanguage.code,
                originalTranscript = originalText,
                chineseTranslation = chineseText,
                timestampsJson = RecordingSerialization.encodeSegments(segments),
                summaryJson = "",
                modelVersion = modelTag
            )
            recordingRepository.saveRecording(entity)
            _selectedRecordingId.value = entity.id
            _liveState.update {
                it.copy(
                    activeRecordingId = null,
                    recordingTitle = "",
                    chunksCaptured = 0
                )
            }
            postMessage("Session and local WAV audio saved to Room Database.")
        }
    }

    fun selectRecording(recordingId: String) {
        _selectedRecordingId.value = recordingId
    }

    fun updateLibrarySearchQuery(query: String) {
        _librarySearchQuery.value = query
    }

    fun toggleAudioPlayback(recording: RecordingEntity) {
        audioPlayer.togglePlayPause(recording.audioFilePath)
    }

    fun stopAudioPlayback() {
        audioPlayer.stop()
    }

    fun saveTranscriptEdits(
        recording: RecordingEntity,
        newTitle: String,
        newOriginalTranscript: String,
        newChineseTranslation: String,
        updatedSegments: List<TranscriptSegment>
    ) {
        viewModelScope.launch {
            val updated = recording.copy(
                title = newTitle.trim().ifBlank { recording.title },
                originalTranscript = newOriginalTranscript.trim(),
                chineseTranslation = newChineseTranslation.trim(),
                timestampsJson = RecordingSerialization.encodeSegments(updatedSegments)
            )
            recordingRepository.updateRecording(updated)
            postMessage("Transcript saved locally.")
        }
    }

    fun appendSegmentToRecording(
        recording: RecordingEntity,
        originalText: String,
        translatedText: String
    ) {
        if (originalText.isBlank() && translatedText.isBlank()) return
        viewModelScope.launch {
            val existing = RecordingSerialization.decodeSegments(recording.timestampsJson)
            val startTime = (existing.lastOrNull()?.endMs ?: 0L) + 500L
            val endTime = startTime + 3000L
            val newSeg = TranscriptSegment(
                id = UUID.randomUUID().toString(),
                startMs = startTime,
                endMs = endTime,
                originalText = originalText.trim(),
                translatedText = translatedText.trim(),
                sourceLanguage = SupportedLanguage.fromCode(recording.sourceLanguageCode),
                targetLanguage = SupportedLanguage.fromCode(recording.targetLanguageCode),
                isFinal = true
            )
            val updatedList = existing + newSeg
            val updatedOriginal = if (recording.originalTranscript.isBlank()) {
                originalText.trim()
            } else {
                "${recording.originalTranscript}\n${originalText.trim()}"
            }
            val updatedChinese = if (recording.chineseTranslation.isBlank()) {
                translatedText.trim()
            } else {
                "${recording.chineseTranslation}\n${translatedText.trim()}"
            }
            val updatedEntity = recording.copy(
                originalTranscript = updatedOriginal,
                chineseTranslation = updatedChinese,
                timestampsJson = RecordingSerialization.encodeSegments(updatedList)
            )
            recordingRepository.updateRecording(updatedEntity)
            postMessage("已将翻译内容加入会话记录。")
        }
    }

    // --- Yuanbao-Style AI Deep Summary & Offline Extractive Summary ---

    fun generateYuanbaoDeepSummary(recording: RecordingEntity) {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            val segments = RecordingSerialization.decodeSegments(recording.timestampsJson)
            val sourceLang = SupportedLanguage.fromCode(recording.sourceLanguageCode)
            val targetLang = SupportedLanguage.fromCode(recording.targetLanguageCode)

            val result = onlineAiService.generateDeepSummary(
                originalTranscript = recording.originalTranscript,
                chineseTranscript = recording.chineseTranslation,
                segments = segments,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang
            )

            result.onSuccess { summary ->
                val updated = recording.copy(
                    summaryJson = RecordingSerialization.encodeSummary(summary)
                )
                recordingRepository.updateRecording(updated)
                _isGeneratingSummary.value = false
                postMessage("Gemini 元宝式 AI 深度总结生成成功！")
            }.onFailure { err ->
                _isGeneratingSummary.value = false
                postMessage("AI 总结失败: ${err.localizedMessage}，正在切换至本地离线提取...")
                generateOfflineSummary(recording)
            }
        }
    }

    fun generateOfflineSummary(recording: RecordingEntity) {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            val segments = RecordingSerialization.decodeSegments(recording.timestampsJson)
            val sourceLang = SupportedLanguage.fromCode(recording.sourceLanguageCode)
            val targetLang = SupportedLanguage.fromCode(recording.targetLanguageCode)

            val summary = withContext(Dispatchers.Default) {
                summaryEngine.summarize(
                    originalTranscript = recording.originalTranscript,
                    chineseTranscript = recording.chineseTranslation,
                    segments = segments,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang
                )
            }

            val updated = recording.copy(
                summaryJson = RecordingSerialization.encodeSummary(summary)
            )
            recordingRepository.updateRecording(updated)
            _isGeneratingSummary.value = false
            postMessage("离线提取式总结已更新 (${summary.engineVersion})。")
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            if (playbackState.value.playingFilePath == recording.audioFilePath) {
                audioPlayer.stop()
            }
            recordingRepository.deleteRecording(recording)
            if (_selectedRecordingId.value == recording.id) {
                _selectedRecordingId.value = null
            }
            postMessage("Recording and local WAV audio deleted.")
        }
    }

    fun exportTxtToStream(recording: RecordingEntity, outputStream: OutputStream) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                exporter.writeTxtToStream(recording, outputStream)
            }
            postMessage("Exported UTF-8 TXT document.")
        }
    }

    fun exportPdfToStream(recording: RecordingEntity, outputStream: OutputStream) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                exporter.writePdfToStream(recording, outputStream)
            }
            postMessage("Exported PDF document.")
        }
    }

    fun buildShareTxtIntent(recording: RecordingEntity) =
        exporter.createShareIntent(
            uri = exporter.exportAndShareTxt(recording),
            mimeType = "text/plain",
            chooserTitle = "Share TXT Transcript"
        )

    fun buildSharePdfIntent(recording: RecordingEntity) =
        exporter.createShareIntent(
            uri = exporter.exportAndSharePdf(recording),
            mimeType = "application/pdf",
            chooserTitle = "Share PDF Transcript"
        )

    // Model Manager Actions
    fun refreshModelManager() {
        modelManager.refreshCatalogState()
        refreshReadinessDiagnostics()
    }

    fun requestModelDownload(item: ManagedModelItem) {
        _pendingDownloadModel.value = item
    }

    fun confirmModelDownload() {
        val item = _pendingDownloadModel.value ?: return
        _pendingDownloadModel.value = null
        modelManager.startUserConfirmedDownload(item.spec.id)
    }

    fun dismissDownloadConfirmation() {
        _pendingDownloadModel.value = null
    }

    fun cancelModelDownload(modelId: String) {
        modelManager.cancelDownload(modelId)
    }

    fun deleteModel(modelId: String) {
        modelManager.deleteModel(modelId)
        postMessage("Model files removed from device storage.")
    }

    fun buildPlainTextForCopy(recording: RecordingEntity): String {
        return exporter.buildPlainTextDocument(recording)
    }

    fun postMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeSnackbarMessage() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(
                        recordingRepository = container.recordingRepository,
                        preferencesRepository = container.userPreferencesRepository,
                        modelManager = container.offlineModelManager,
                        audioRecorder = container.audioRecorder,
                        audioPlayer = container.audioPlayer,
                        speechRecognizer = container.speechRecognizer,
                        translationEngine = container.translationEngineRegistry.getActiveEngine(),
                        summaryEngine = container.summaryEngine,
                        onlineAiService = container.onlineAiService,
                        exporter = container.transcriptExporter
                    ) as T
                }
            }
        }
    }
}
