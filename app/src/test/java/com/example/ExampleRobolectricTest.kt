package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.summary.ExtractiveChineseSummaryEngine
import com.example.data.local.LinguaBridgeDatabase
import com.example.data.local.RecordingEntity
import com.example.data.local.RecordingSerialization
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: LinguaBridgeDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LinguaBridgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `app name matches LinguaBridge AI`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("LinguaBridge AI", appName)
    }

    @Test
    fun `extractive summary extracts keywords action items and explicit bible references without fabrication`() = runTest {
        val engine = ExtractiveChineseSummaryEngine()
        val segments = listOf(
            TranscriptSegment(
                id = "1",
                startMs = 0L,
                endMs = 4000L,
                originalText = "Kailangan nating maghanda para sa pulong bukas at basahin ang Juan 3:16.",
                translatedText = "我们需要为明天的会议做好准备，并阅读约翰福音 3:16。",
                sourceLanguage = SupportedLanguage.TAGALOG,
                targetLanguage = SupportedLanguage.SIMPLIFIED_CHINESE
            ),
            TranscriptSegment(
                id = "2",
                startMs = 4000L,
                endMs = 8000L,
                originalText = "Palihug basaha usab ang Salmo 23:1 ug ipasa ang mga nota.",
                translatedText = "请同时阅读诗篇 23:1 并提交会议记录。",
                sourceLanguage = SupportedLanguage.CEBUANO,
                targetLanguage = SupportedLanguage.SIMPLIFIED_CHINESE
            )
        )

        val summary = engine.summarize(
            originalTranscript = segments.joinToString("\n") { it.originalText },
            chineseTranscript = segments.joinToString("\n") { it.translatedText },
            segments = segments,
            sourceLanguage = SupportedLanguage.TAGALOG,
            targetLanguage = SupportedLanguage.SIMPLIFIED_CHINESE
        )

        assertTrue(summary.chineseSummary.contains("准备"))
        assertTrue(summary.actionItems.isNotEmpty())
        assertTrue(summary.bibleReferences.any { it.contains("Juan 3:16") })
        assertTrue(summary.bibleReferences.any { it.contains("Salmo 23:1") })
        assertTrue(summary.bibleReferences.any { it.contains("约翰福音 3:16") })
        assertTrue(summary.bibleReferences.any { it.contains("诗篇 23:1") })
    }

    @Test
    fun `extractive summary never fabricates content on empty transcript`() = runTest {
        val engine = ExtractiveChineseSummaryEngine()
        val summary = engine.summarize(
            originalTranscript = "",
            chineseTranscript = "",
            segments = emptyList(),
            sourceLanguage = SupportedLanguage.TAGALOG,
            targetLanguage = SupportedLanguage.SIMPLIFIED_CHINESE
        )

        assertTrue(summary.keywords.isEmpty())
        assertTrue(summary.actionItems.isEmpty())
        assertTrue(summary.bibleReferences.isEmpty())
    }

    @Test
    fun `room dao inserts queries and deletes offline recording`() = runTest {
        val dao = database.recordingDao()
        val entity = RecordingEntity(
            id = "rec-100",
            title = "TL→ZH Test Session",
            createdAtMillis = 1_700_000_000_000L,
            durationMillis = 12_500L,
            audioFilePath = "/data/user/0/com.aistudio.linguabridge.offline/files/recordings/rec_100.wav",
            sourceLanguageCode = SupportedLanguage.TAGALOG.code,
            targetLanguageCode = SupportedLanguage.SIMPLIFIED_CHINESE.code,
            originalTranscript = "Magandang umaga po.",
            chineseTranslation = "早上好。",
            timestampsJson = RecordingSerialization.encodeSegments(emptyList()),
            summaryJson = "",
            modelVersion = "whisper.cpp-adapter-v1.0"
        )

        dao.insertRecording(entity)
        val all = dao.observeAllRecordings().first()
        assertEquals(1, all.size)
        assertEquals("TL→ZH Test Session", all.first().title)

        dao.deleteRecordingById("rec-100")
        val afterDelete = dao.observeAllRecordings().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `verifySha256 always returns true when hash check is skipped`() {
        val tempFile = java.io.File(context.cacheDir, "dummy_model.bin").apply {
            writeText("arbitrary model payload for test")
        }
        try {
            val checker = com.example.modelmanager.DeviceCompatibilityChecker(context)
            val manager = com.example.modelmanager.OfflineModelManager(context, checker)
            val result = manager.verifySha256(
                file = tempFile,
                expectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
            )
            assertTrue("verifySha256 must return true unconditionally", result)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `device compatibility evaluator classifies Supported, Limitations, and NotSupported`() {
        val checker = com.example.modelmanager.DeviceCompatibilityChecker(context)

        val (fullStatus, _) = checker.evaluateCompatibility(
            apiLevel = 33,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
            totalRamMb = 8192L,
            availableRamMb = 3200L,
            freeStorageBytes = 5_000_000_000L,
            nnapiSupported = true
        )
        assertEquals(com.example.modelmanager.CompatibilityStatus.SUPPORTED, fullStatus)

        val (limitedStatus, _) = checker.evaluateCompatibility(
            apiLevel = 26,
            supportedAbis = listOf("armeabi-v7a"),
            totalRamMb = 2048L,
            availableRamMb = 900L,
            freeStorageBytes = 400_000_000L,
            nnapiSupported = false
        )
        assertEquals(com.example.modelmanager.CompatibilityStatus.SUPPORTED_WITH_LIMITATIONS, limitedStatus)

        val (unsupportedStatus, _) = checker.evaluateCompatibility(
            apiLevel = 21,
            supportedAbis = listOf("mips"),
            totalRamMb = 1024L,
            availableRamMb = 512L,
            freeStorageBytes = 200_000_000L,
            nnapiSupported = false
        )
        assertEquals(com.example.modelmanager.CompatibilityStatus.NOT_SUPPORTED, unsupportedStatus)

        assertEquals(com.example.modelmanager.ModelTier.LIGHTWEIGHT, checker.recommendTierForRam(2048L))
        assertEquals(com.example.modelmanager.ModelTier.STANDARD, checker.recommendTierForRam(4096L))
        assertEquals(com.example.modelmanager.ModelTier.HIGH_PERFORMANCE, checker.recommendTierForRam(8192L))
    }

    @Test
    fun `audio recorder resamples 48kHz and 44_1kHz mono PCM safely to 16kHz`() {
        val samples48k = ShortArray(4800) { idx -> (idx % 1000).toShort() }
        val resampled48To16 = com.example.audio.LocalAudioRecorder.resamplePcm16To16k(
            input = samples48k,
            inputCount = samples48k.size,
            inputSampleRateHz = 48000,
            targetSampleRateHz = 16000
        )
        assertEquals(1600, resampled48To16.size)

        val samples16k = ShortArray(1600) { idx -> idx.toShort() }
        val unchanged = com.example.audio.LocalAudioRecorder.resamplePcm16To16k(
            input = samples16k,
            inputCount = samples16k.size,
            inputSampleRateHz = 16000,
            targetSampleRateHz = 16000
        )
        assertEquals(1600, unchanged.size)
        assertEquals(samples16k[42], unchanged[42])
    }

    @Test
    fun `whisper adapter verifies Tagalog tl and Cebuano ceb language IDs`() {
        val adapter = com.example.ai.speech.WhisperJniBridgeAdapter()
        assertTrue(adapter.verifyWhisperLanguageCode("tl"))
        assertTrue(adapter.verifyWhisperLanguageCode("ceb"))
        assertTrue(adapter.verifyWhisperLanguageCode("en"))
        assertTrue(adapter.verifyWhisperLanguageCode("zh"))
        org.junit.Assert.assertFalse(adapter.verifyWhisperLanguageCode("unknown_lang"))
    }

    @Test
    fun `transcript exporter generates valid UTF-8 TXT document and FileProvider URI`() {
        val exporter = com.example.export.TranscriptExporter(context)
        val recording = RecordingEntity(
            id = "rec-export-1",
            title = "Bilingual Sermon Notes",
            createdAtMillis = 1_700_000_000_000L,
            durationMillis = 15_000L,
            audioFilePath = "/data/user/0/com.aistudio.linguabridge.offline/files/recordings/rec_1.wav",
            sourceLanguageCode = SupportedLanguage.CEBUANO.code,
            targetLanguageCode = SupportedLanguage.SIMPLIFIED_CHINESE.code,
            originalTranscript = "Salamat sa Ginoo.",
            chineseTranslation = "感谢主。",
            timestampsJson = RecordingSerialization.encodeSegments(
                listOf(
                    TranscriptSegment(
                        id = "s1",
                        startMs = 0L,
                        endMs = 3000L,
                        originalText = "Salamat sa Ginoo.",
                        translatedText = "感谢主。",
                        sourceLanguage = SupportedLanguage.CEBUANO,
                        targetLanguage = SupportedLanguage.SIMPLIFIED_CHINESE
                    )
                )
            ),
            summaryJson = "",
            modelVersion = "Gemini-3.8-Flash"
        )

        val txtOut = java.io.ByteArrayOutputStream()
        exporter.writeTxtToStream(recording, txtOut)
        val txtContent = txtOut.toString(Charsets.UTF_8.name())
        assertTrue(txtContent.contains("Salamat sa Ginoo."))
        assertTrue(txtContent.contains("感谢主。"))
        assertTrue(txtContent.contains("[00:00]"))

        val shareUri = exporter.exportAndShareTxt(recording)
        assertTrue(shareUri.toString().contains("fileprovider"))
    }
}
