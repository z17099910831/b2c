package com.example.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(
    private val recordingDao: RecordingDao
) {
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.observeAllRecordings()

    fun observeRecording(id: String): Flow<RecordingEntity?> = recordingDao.observeRecordingById(id)

    fun searchRecordings(query: String): Flow<List<RecordingEntity>> {
        return if (query.isBlank()) {
            allRecordings
        } else {
            recordingDao.searchRecordings(query.trim())
        }
    }

    suspend fun getRecording(id: String): RecordingEntity? = recordingDao.getRecordingById(id)

    suspend fun saveRecording(recording: RecordingEntity) {
        recordingDao.insertRecording(recording)
    }

    suspend fun updateRecording(recording: RecordingEntity) {
        recordingDao.updateRecording(recording)
    }

    suspend fun deleteRecording(recording: RecordingEntity) {
        recordingDao.deleteRecordingById(recording.id)
        withContext(Dispatchers.IO) {
            if (recording.audioFilePath.isNotBlank()) {
                val file = File(recording.audioFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
}
