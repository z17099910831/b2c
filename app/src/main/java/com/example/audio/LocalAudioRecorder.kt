package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

enum class AudioRecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    INTERRUPTED
}

data class AudioChunk(
    val samples: ShortArray,
    val startMs: Long,
    val endMs: Long,
    val sampleRateHz: Int
)

data class CompletedRecording(
    val recordingId: String,
    val filePath: String,
    val durationMillis: Long
)

class LocalAudioRecorder(
    private val context: Context
) {
    companion object {
        const val SAMPLE_RATE_HZ = 16000
        private val CANDIDATE_SAMPLE_RATES = intArrayOf(16000, 48000, 44100, 22050, 8000)
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val WAV_HEADER_SIZE = 44
        private const val CHUNK_DURATION_MS = 1500L
        private const val MAX_WAVEFORM_BARS = 48

        /**
         * Safely resamples 16-bit mono PCM captured at [inputSampleRateHz] to [targetSampleRateHz] (16 kHz).
         */
        fun resamplePcm16To16k(
            input: ShortArray,
            inputCount: Int,
            inputSampleRateHz: Int,
            targetSampleRateHz: Int = SAMPLE_RATE_HZ
        ): ShortArray {
            if (inputCount <= 0) return ShortArray(0)
            if (inputSampleRateHz == targetSampleRateHz || inputSampleRateHz <= 0) {
                return input.copyOf(inputCount)
            }
            val ratio = inputSampleRateHz.toDouble() / targetSampleRateHz.toDouble()
            val outputLength = ((inputCount / ratio).toInt()).coerceAtLeast(1)
            val output = ShortArray(outputLength)
            for (i in 0 until outputLength) {
                val srcPos = i * ratio
                val idx0 = srcPos.toInt().coerceIn(0, inputCount - 1)
                val idx1 = (idx0 + 1).coerceIn(0, inputCount - 1)
                val frac = srcPos - idx0
                val interpolated = input[idx0] * (1.0 - frac) + input[idx1] * frac
                output[i] = interpolated.toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            return output
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _recordingState = MutableStateFlow(AudioRecordingState.IDLE)
    val recordingState: StateFlow<AudioRecordingState> = _recordingState.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow(List(MAX_WAVEFORM_BARS) { 0.04f })
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    private val _audioChunks = MutableSharedFlow<AudioChunk>(extraBufferCapacity = 32)
    val audioChunks: SharedFlow<AudioChunk> = _audioChunks.asSharedFlow()

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var activeFile: File? = null
    private var activeRecordingId: String? = null
    private var outputFileStream: RandomAccessFile? = null
    private var totalPcmBytesWritten: Long = 0L
    private var focusRequest: AudioFocusRequest? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_recordingState.value == AudioRecordingState.RECORDING) {
                    pauseInternal(interrupted = true)
                    _statusNotice.value = "Audio focus interrupted. Recording paused safely on device."
                }
            }
        }
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Synchronized
    fun startNewRecording(recordingId: String): Result<String> {
        if (!hasRecordPermission()) {
            return Result.failure(SecurityException("Microphone permission is required."))
        }
        stopAndDiscardIfIdle()

        val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val targetFile = File(recordingsDir, "rec_${recordingId}.wav")

        return runCatching {
            RandomAccessFile(targetFile, "rw").use { raf ->
                raf.setLength(0L)
                raf.write(ByteArray(WAV_HEADER_SIZE))
            }
            activeRecordingId = recordingId
            activeFile = targetFile
            totalPcmBytesWritten = 0L
            _elapsedMillis.value = 0L
            _amplitudeHistory.value = List(MAX_WAVEFORM_BARS) { 0.04f }
            _statusNotice.value = null

            outputFileStream = RandomAccessFile(targetFile, "rw").apply {
                seek(WAV_HEADER_SIZE.toLong())
            }
            beginAudioCapture()
            targetFile.absolutePath
        }.onFailure {
            cleanupResources()
        }
    }

    @Synchronized
    fun pauseRecording() {
        if (_recordingState.value == AudioRecordingState.RECORDING) {
            pauseInternal(interrupted = false)
            _statusNotice.value = "Recording paused. Audio flushed safely to local storage."
        }
    }

    @Synchronized
    fun resumeRecording(): Result<Unit> {
        if (!hasRecordPermission()) {
            return Result.failure(SecurityException("Microphone permission is required."))
        }
        val state = _recordingState.value
        if (state != AudioRecordingState.PAUSED && state != AudioRecordingState.INTERRUPTED) {
            return Result.success(Unit)
        }
        return runCatching {
            _statusNotice.value = null
            beginAudioCapture()
        }
    }

    @Synchronized
    fun stopRecording(): CompletedRecording? {
        val file = activeFile ?: return null
        val id = activeRecordingId ?: return null

        stopCaptureLoop()
        finalizeWavHeader()
        releaseAudioFocus()

        val finalDurationMs = _elapsedMillis.value
        _recordingState.value = AudioRecordingState.IDLE
        activeFile = null
        activeRecordingId = null

        return CompletedRecording(
            recordingId = id,
            filePath = file.absolutePath,
            durationMillis = finalDurationMs
        )
    }

    private fun pauseInternal(interrupted: Boolean) {
        stopCaptureLoop()
        finalizeWavHeader()
        releaseAudioFocus()
        RecordingForegroundService.stop(context)
        _recordingState.value = if (interrupted) {
            AudioRecordingState.INTERRUPTED
        } else {
            AudioRecordingState.PAUSED
        }
    }

    private fun openCompatibleAudioRecord(): Pair<AudioRecord, Int> {
        for (candidateRate in CANDIDATE_SAMPLE_RATES) {
            val minBuffer = AudioRecord.getMinBufferSize(
                candidateRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) continue

            val bufferSize = (minBuffer * 2).coerceAtLeast(candidateRate)
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    candidateRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            } catch (se: SecurityException) {
                throw se
            } catch (_: Exception) {
                null
            }

            if (record != null) {
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    return record to candidateRate
                }
                runCatching { record.release() }
            }
        }
        throw IllegalStateException("AudioRecord could not initialize at any supported sample rate.")
    }

    private fun beginAudioCapture() {
        requestAudioFocus()
        val (record, captureSampleRate) = openCompatibleAudioRecord()

        audioRecord = record
        record.startRecording()
        RecordingForegroundService.start(
            context,
            if (captureSampleRate == SAMPLE_RATE_HZ) {
                "Recording local 16 kHz audio"
            } else {
                "Recording audio (${captureSampleRate}Hz → 16kHz)"
            }
        )
        _recordingState.value = AudioRecordingState.RECORDING

        captureJob = scope.launch {
            val frameShorts = ShortArray(2048)
            val chunkSamples = mutableListOf<Short>()
            val samplesPerChunk = ((SAMPLE_RATE_HZ * CHUNK_DURATION_MS) / 1000L).toInt()
            var chunkStartMs = _elapsedMillis.value

            while (isActive && _recordingState.value == AudioRecordingState.RECORDING) {
                val readCount = record.read(frameShorts, 0, frameShorts.size)
                if (readCount > 0) {
                    val normalizedSamples = resamplePcm16To16k(
                        input = frameShorts,
                        inputCount = readCount,
                        inputSampleRateHz = captureSampleRate,
                        targetSampleRateHz = SAMPLE_RATE_HZ
                    )
                    val byteBuffer = ByteBuffer.allocate(normalizedSamples.size * 2)
                        .order(ByteOrder.LITTLE_ENDIAN)
                    var sumSquares = 0.0
                    for (sample in normalizedSamples) {
                        byteBuffer.putShort(sample)
                        sumSquares += (sample.toDouble() * sample.toDouble())
                        chunkSamples.add(sample)
                    }

                    synchronized(this@LocalAudioRecorder) {
                        outputFileStream?.write(byteBuffer.array(), 0, normalizedSamples.size * 2)
                        totalPcmBytesWritten += (normalizedSamples.size * 2L)
                    }

                    val totalSamples = totalPcmBytesWritten / 2L
                    val currentElapsedMs = (totalSamples * 1000L) / SAMPLE_RATE_HZ
                    _elapsedMillis.value = currentElapsedMs

                    if (normalizedSamples.isNotEmpty()) {
                        val rms = sqrt(sumSquares / normalizedSamples.size) / Short.MAX_VALUE.toDouble()
                        val normalizedLevel = (rms * 4.5).toFloat().coerceIn(0.05f, 1.0f)
                        val updatedBars = _amplitudeHistory.value.drop(1) + normalizedLevel
                        _amplitudeHistory.value = updatedBars
                    }

                    if (chunkSamples.size >= samplesPerChunk) {
                        val chunkEndMs = currentElapsedMs
                        val emitted = AudioChunk(
                            samples = chunkSamples.toShortArray(),
                            startMs = chunkStartMs,
                            endMs = chunkEndMs,
                            sampleRateHz = SAMPLE_RATE_HZ
                        )
                        _audioChunks.tryEmit(emitted)
                        chunkSamples.clear()
                        chunkStartMs = chunkEndMs
                    }
                }
            }
        }
    }

    private fun stopCaptureLoop() {
        captureJob?.cancel()
        captureJob = null
        RecordingForegroundService.stop(context)
        audioRecord?.let { record ->
            runCatching {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            }
        }
        audioRecord = null
    }

    private fun finalizeWavHeader() {
        val stream = outputFileStream ?: return
        runCatching {
            val pcmDataSize = totalPcmBytesWritten
            val byteRate = SAMPLE_RATE_HZ * CHANNELS * (BITS_PER_SAMPLE / 8)
            val blockAlign = (CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()
            val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt((36L + pcmDataSize).toInt())
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16) // PCM chunk size
                putShort(1) // PCM format
                putShort(CHANNELS.toShort())
                putInt(SAMPLE_RATE_HZ)
                putInt(byteRate)
                putShort(blockAlign)
                putShort(BITS_PER_SAMPLE.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(pcmDataSize.toInt())
            }.array()

            stream.seek(0L)
            stream.write(header)
            stream.seek(WAV_HEADER_SIZE + pcmDataSize)
        }
        if (_recordingState.value == AudioRecordingState.IDLE) {
            runCatching { stream.close() }
            outputFileStream = null
        }
    }

    private fun stopAndDiscardIfIdle() {
        stopCaptureLoop()
        runCatching { outputFileStream?.close() }
        outputFileStream = null
        releaseAudioFocus()
        _recordingState.value = AudioRecordingState.IDLE
    }

    private fun cleanupResources() {
        stopCaptureLoop()
        runCatching { outputFileStream?.close() }
        outputFileStream = null
        releaseAudioFocus()
        activeFile = null
        activeRecordingId = null
        _recordingState.value = AudioRecordingState.IDLE
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }
}
