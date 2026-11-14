package com.example.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlaybackState(
    val playingFilePath: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

class LocalAudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    fun togglePlayPause(filePath: String) {
        val current = _playbackState.value
        if (current.playingFilePath == filePath && mediaPlayer != null) {
            if (current.isPlaying) {
                mediaPlayer?.pause()
                progressJob?.cancel()
                _playbackState.value = current.copy(isPlaying = false)
            } else {
                mediaPlayer?.start()
                _playbackState.value = current.copy(isPlaying = true)
                startProgressUpdates()
            }
            return
        }

        stop()
        val file = File(filePath)
        if (!file.exists() || file.length() <= 44L) {
            _playbackState.value = AudioPlaybackState(
                errorMessage = "Audio file is empty or missing on local storage."
            )
            return
        }

        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    stop()
                }
                start()
            }
            mediaPlayer = player
            _playbackState.value = AudioPlaybackState(
                playingFilePath = filePath,
                isPlaying = true,
                positionMs = 0L,
                durationMs = player.duration.toLong().coerceAtLeast(0L)
            )
            startProgressUpdates()
        }.onFailure { err ->
            stop()
            _playbackState.value = AudioPlaybackState(
                errorMessage = err.localizedMessage ?: "Unable to play local WAV audio."
            )
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _playbackState.value = AudioPlaybackState()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer != null && _playbackState.value.isPlaying) {
                val player = mediaPlayer ?: break
                _playbackState.value = _playbackState.value.copy(
                    positionMs = player.currentPosition.toLong().coerceAtLeast(0L),
                    durationMs = player.duration.toLong().coerceAtLeast(0L)
                )
                delay(200L)
            }
        }
    }
}
