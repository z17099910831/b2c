package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioPlaybackState
import com.example.data.local.RecordingEntity
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.CoralActive
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.MonoTimestampStyle
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingLibraryScreen(
    recordings: List<RecordingEntity>,
    searchQuery: String,
    playbackState: AudioPlaybackState,
    onSearchQueryChange: (String) -> Unit,
    onTogglePlayback: (RecordingEntity) -> Unit,
    onOpenTranscription: (String) -> Unit,
    onOpenSummary: (String) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onStartNewRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    pendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Local Recording?",
            message = "This permanently deletes \"${target.title}\", its local WAV audio file, transcript, and summary from this device.",
            onConfirm = {
                onDeleteRecording(target)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search local recordings & transcripts") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input")
            )
        }

        if (recordings.isEmpty()) {
            item {
                EmptyLibraryStateCard(
                    hasQuery = searchQuery.isNotBlank(),
                    onStartNewRecording = onStartNewRecording
                )
            }
        } else {
            items(recordings, key = { it.id }) { recording ->
                LibraryRecordingCard(
                    recording = recording,
                    isPlaying = playbackState.playingFilePath == recording.audioFilePath && playbackState.isPlaying,
                    onTogglePlayback = { onTogglePlayback(recording) },
                    onOpenTranscription = { onOpenTranscription(recording.id) },
                    onOpenSummary = { onOpenSummary(recording.id) },
                    onRequestDelete = { pendingDelete = recording }
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryStateCard(
    hasQuery: Boolean,
    onStartNewRecording: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_recordings),
                contentDescription = "Local recording studio illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = if (hasQuery) "No Matching Recordings" else "Your Local Recording Library Is Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (hasQuery) {
                    "Try clearing or adjusting your search query."
                } else {
                    "All recorded audio files, bilingual transcripts, timestamps, and summaries stay 100% private in Room and internal device storage."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasQuery) {
                Button(
                    onClick = onStartNewRecording,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("library_start_recording_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record First Session")
                }
            }
        }
    }
}

@Composable
private fun LibraryRecordingCard(
    recording: RecordingEntity,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onOpenTranscription: () -> Unit,
    onOpenSummary: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val sourceLang = SupportedLanguage.fromCode(recording.sourceLanguageCode)
    val targetLang = SupportedLanguage.fromCode(recording.targetLanguageCode)
    val dateLabel = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(Date(recording.createdAtMillis))
    val audioExists = remember(recording.audioFilePath) {
        recording.audioFilePath.isNotBlank() && File(recording.audioFilePath).exists()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library_item_${recording.id}"),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = ElectricCyanContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${sourceLang.shortTag} → ${targetLang.shortTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "$dateLabel • ${TranscriptSegment.formatMillis(recording.durationMillis)}",
                            style = MonoTimestampStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_recording_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete recording ${recording.title}",
                        tint = CoralActive
                    )
                }
            }

            if (recording.originalTranscript.isNotBlank() || recording.chineseTranslation.isNotBlank()) {
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (recording.originalTranscript.isNotBlank()) {
                            Text(
                                text = recording.originalTranscript,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (recording.chineseTranslation.isNotBlank()) {
                            Text(
                                text = recording.chineseTranslation,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldReady,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTogglePlayback,
                    enabled = audioExists,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("play_recording_${recording.id}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlaying) "Pause" else "Play", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenTranscription,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("open_transcript_${recording.id}")
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit/Export", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onOpenSummary,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("open_summary_${recording.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Summary", maxLines = 1)
                }
            }
        }
    }
}
