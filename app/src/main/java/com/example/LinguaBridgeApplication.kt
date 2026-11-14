package com.example

import android.app.Application
import android.content.Context
import com.example.ai.online.OnlineAiService
import com.example.ai.speech.LocalSpeechRecognizer
import com.example.ai.speech.WhisperJniBridgeAdapter
import com.example.ai.summary.ExtractiveChineseSummaryEngine
import com.example.ai.summary.LocalSummaryEngine
import com.example.ai.translation.NllbOnnxEngineAdapter
import com.example.ai.translation.TranslationEngineRegistry
import com.example.audio.LocalAudioPlayer
import com.example.audio.LocalAudioRecorder
import com.example.data.local.LinguaBridgeDatabase
import com.example.data.local.RecordingRepository
import com.example.data.local.UserPreferencesRepository
import com.example.export.TranscriptExporter
import com.example.modelmanager.DeviceCompatibilityChecker
import com.example.modelmanager.OfflineModelManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: LinguaBridgeDatabase by lazy {
        LinguaBridgeDatabase.getInstance(appContext)
    }

    val recordingRepository: RecordingRepository by lazy {
        RecordingRepository(database.recordingDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val compatibilityChecker: DeviceCompatibilityChecker by lazy {
        DeviceCompatibilityChecker(appContext)
    }

    val offlineModelManager: OfflineModelManager by lazy {
        OfflineModelManager(appContext, compatibilityChecker)
    }

    val audioRecorder: LocalAudioRecorder by lazy {
        LocalAudioRecorder(appContext)
    }

    val audioPlayer: LocalAudioPlayer by lazy {
        LocalAudioPlayer()
    }

    val speechRecognizer: LocalSpeechRecognizer by lazy {
        WhisperJniBridgeAdapter()
    }

    val translationEngineRegistry: TranslationEngineRegistry by lazy {
        TranslationEngineRegistry(NllbOnnxEngineAdapter(appContext))
    }

    val summaryEngine: LocalSummaryEngine by lazy {
        ExtractiveChineseSummaryEngine()
    }

    val onlineAiService: OnlineAiService by lazy {
        OnlineAiService()
    }

    val transcriptExporter: TranscriptExporter by lazy {
        TranscriptExporter(appContext)
    }
}

class LinguaBridgeApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
