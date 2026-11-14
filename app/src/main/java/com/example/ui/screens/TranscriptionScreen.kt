package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.AudioPlaybackState
import com.example.data.local.RecordingEntity
import com.example.data.local.RecordingSerialization
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import com.example.ui.components.EmptyAudioBanner
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.MonoTimestampStyle
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyVoid
import java.io.OutputStream
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.OnlineTranslationState
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranscriptionScreen(
    recordings: List<RecordingEntity>,
    selectedRecording: RecordingEntity?,
    playbackState: AudioPlaybackState,
    onlineTranslationState: OnlineTranslationState,
    onSelectRecording: (String) -> Unit,
    onTogglePlayback: (RecordingEntity) -> Unit,
    onExecuteOnlineTranslation: (String) -> Unit,
    onUpdateOnlineInputText: (String) -> Unit,
    onAppendSegment: (RecordingEntity, String, String) -> Unit,
    onGenerateYuanbaoSummary: (RecordingEntity) -> Unit,
    onSaveEdits: (
        recording: RecordingEntity,
        newTitle: String,
        newOriginal: String,
        newChinese: String,
        updatedSegments: List<TranscriptSegment>
    ) -> Unit,
    onExportTxtStream: (RecordingEntity, OutputStream) -> Unit,
    onExportPdfStream: (RecordingEntity, OutputStream) -> Unit,
    onShareTxt: (RecordingEntity) -> Unit,
    onSharePdf: (RecordingEntity) -> Unit,
    onOpenAiSummary: () -> Unit,
    onBuildPlainTextForCopy: (RecordingEntity) -> String,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isEditing by remember(selectedRecording?.id) { mutableStateOf(false) }
    var editableTitle by remember(selectedRecording?.id, selectedRecording?.title) {
        mutableStateOf(selectedRecording?.title.orEmpty())
    }
    var editableOriginal by remember(selectedRecording?.id, selectedRecording?.originalTranscript) {
        mutableStateOf(selectedRecording?.originalTranscript.orEmpty())
    }
    var editableChinese by remember(selectedRecording?.id, selectedRecording?.chineseTranslation) {
        mutableStateOf(selectedRecording?.chineseTranslation.orEmpty())
    }

    val createTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val rec = selectedRecording ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                onExportTxtStream(rec, stream)
            }
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val rec = selectedRecording ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                onExportPdfStream(rec, stream)
            }
        }
    }

    if (selectedRecording == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyAudioBanner(
                title = "No Recordings in Local Database",
                subtitle = "Record a session in Live Interpreter first. Your audio, bilingual transcripts, and timestamps are saved privately in Room."
            )
        }
        return
    }

    val sourceLang = SupportedLanguage.fromCode(selectedRecording.sourceLanguageCode)
    val targetLang = SupportedLanguage.fromCode(selectedRecording.targetLanguageCode)
    val segments = remember(selectedRecording.id, selectedRecording.timestampsJson) {
        RecordingSerialization.decodeSegments(selectedRecording.timestampsJson)
    }

    val filteredSegments = remember(segments, searchQuery) {
        if (searchQuery.isBlank()) segments
        else {
            val q = searchQuery.trim()
            segments.filter {
                it.originalText.contains(q, ignoreCase = true) ||
                    it.translatedText.contains(q, ignoreCase = true) ||
                    it.formattedTimestamp.contains(q, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RecordingSelectorDropdown(
                recordings = recordings,
                selected = selectedRecording,
                onSelect = onSelectRecording
            )
        }

        item {
            AudioPlaybackCard(
                recording = selectedRecording,
                playbackState = playbackState,
                onTogglePlayback = { onTogglePlayback(selectedRecording) }
            )
        }

        item {
            OnlineTranslationWorkbenchCard(
                recording = selectedRecording,
                onlineState = onlineTranslationState,
                onInputChange = onUpdateOnlineInputText,
                onTranslate = onExecuteOnlineTranslation,
                onAppendSegment = onAppendSegment,
                onShowMessage = onShowMessage
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.testTag("transcript_edit_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Save else Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditing) "Close Editor" else "Edit Text")
                }

                OutlinedButton(
                    onClick = {
                        val text = onBuildPlainTextForCopy(selectedRecording)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(selectedRecording.title, text))
                        onShowMessage("Copied bilingual transcript to clipboard.")
                    },
                    modifier = Modifier.testTag("transcript_copy_button")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                OutlinedButton(
                    onClick = {
                        createTxtLauncher.launch("${selectedRecording.title.take(24)}.txt")
                    },
                    modifier = Modifier.testTag("transcript_export_txt_button")
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export TXT")
                }

                OutlinedButton(
                    onClick = {
                        createPdfLauncher.launch("${selectedRecording.title.take(24)}.pdf")
                    },
                    modifier = Modifier.testTag("transcript_export_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF")
                }

                OutlinedButton(
                    onClick = { onShareTxt(selectedRecording) },
                    modifier = Modifier.testTag("transcript_share_txt_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share TXT")
                }

                OutlinedButton(
                    onClick = { onSharePdf(selectedRecording) },
                    modifier = Modifier.testTag("transcript_share_pdf_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PDF")
                }

                Button(
                    onClick = {
                        onGenerateYuanbaoSummary(selectedRecording)
                        onOpenAiSummary()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady),
                    modifier = Modifier.testTag("transcript_open_summary_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✨ 元宝 AI 深度总结")
                }
            }
        }

        if (isEditing) {
            item {
                TranscriptEditorCard(
                    title = editableTitle,
                    originalText = editableOriginal,
                    chineseText = editableChinese,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    onTitleChange = { editableTitle = it },
                    onOriginalChange = { editableOriginal = it },
                    onChineseChange = { editableChinese = it },
                    onSave = {
                        val rebuiltSegments = rebuildTimestampedSegments(
                            originalText = editableOriginal,
                            chineseText = editableChinese,
                            durationMs = selectedRecording.durationMillis,
                            sourceLang = sourceLang,
                            targetLang = targetLang
                        )
                        onSaveEdits(
                            selectedRecording,
                            editableTitle,
                            editableOriginal,
                            editableChinese,
                            rebuiltSegments
                        )
                        isEditing = false
                    }
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search original, Chinese translation, or timestamp") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transcript_search_input"),
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (filteredSegments.isNotEmpty()) {
            items(filteredSegments, key = { it.id }) { segment ->
                TimestampedBilingualCard(segment = segment)
            }
        } else {
            item {
                BilingualFullTextCard(
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    originalText = selectedRecording.originalTranscript,
                    chineseText = selectedRecording.chineseTranslation,
                    searchQuery = searchQuery,
                    onTapEdit = { isEditing = true }
                )
            }
        }
    }
}

@Composable
private fun RecordingSelectorDropdown(
    recordings: List<RecordingEntity>,
    selected: RecordingEntity,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            color = NavyCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NavyBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recording_picker_dropdown")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECTED RECORDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Text(
                        text = selected.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch recording")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            recordings.forEach { rec ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${rec.title} (${TranscriptSegment.formatMillis(rec.durationMillis)})",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(rec.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun AudioPlaybackCard(
    recording: RecordingEntity,
    playbackState: AudioPlaybackState,
    onTogglePlayback: () -> Unit
) {
    val isThisPlaying = playbackState.playingFilePath == recording.audioFilePath && playbackState.isPlaying
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCardElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onTogglePlayback,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("transcript_audio_play_button")
            ) {
                Icon(
                    imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isThisPlaying) "Pause local WAV audio" else "Play local WAV audio",
                    tint = ElectricCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isThisPlaying) "Playing Local WAV Audio" else "Local WAV Audio Ready",
                    style = MaterialTheme.typography.titleSmall
                )
                val pos = if (playbackState.playingFilePath == recording.audioFilePath) {
                    TranscriptSegment.formatMillis(playbackState.positionMs)
                } else {
                    "00:00"
                }
                val total = TranscriptSegment.formatMillis(recording.durationMillis)
                Text(
                    text = "$pos / $total • Model: ${recording.modelVersion}",
                    style = MonoTimestampStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TranscriptEditorCard(
    title: String,
    originalText: String,
    chineseText: String,
    sourceLang: SupportedLanguage,
    targetLang: SupportedLanguage,
    onTitleChange: (String) -> Unit,
    onOriginalChange: (String) -> Unit,
    onChineseChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Local Transcript",
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan
            )
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Recording Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_title_field")
            )
            OutlinedTextField(
                value = originalText,
                onValueChange = onOriginalChange,
                label = { Text("Original Transcript (${sourceLang.displayName})") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_original_field")
            )
            OutlinedTextField(
                value = chineseText,
                onValueChange = onChineseChange,
                label = { Text("Translation (${targetLang.displayName})") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_chinese_field")
            )
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_transcript_edits_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes to Room DB")
            }
        }
    }
}

@Composable
private fun TimestampedBilingualCard(segment: TranscriptSegment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[${segment.formattedTimestamp}]",
                    style = MonoTimestampStyle,
                    color = ElectricCyan
                )
                Text(
                    text = segment.sourceLanguage.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = segment.originalText,
                style = MaterialTheme.typography.bodyLarge
            )
            if (segment.translatedText.isNotBlank()) {
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = segment.targetLanguage.nativeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldReady
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = segment.translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = EmeraldReady
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BilingualFullTextCard(
    sourceLang: SupportedLanguage,
    targetLang: SupportedLanguage,
    originalText: String,
    chineseText: String,
    searchQuery: String,
    onTapEdit: () -> Unit
) {
    val matchesFilter = searchQuery.isBlank() ||
        originalText.contains(searchQuery, ignoreCase = true) ||
        chineseText.contains(searchQuery, ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!matchesFilter) {
                Text(
                    text = "No matching text for \"$searchQuery\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (originalText.isBlank() && chineseText.isBlank()) {
                Text(
                    text = "No transcript lines generated yet.",
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricCyan
                )
                Text(
                    text = "Because native Whisper/NLLB weights are not yet installed, no synthetic text was fabricated. You can tap Edit Text to enter or annotate bilingual transcript lines manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onTapEdit) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Bilingual Text Manually")
                }
            } else {
                Text(
                    text = "Original (${sourceLang.displayName})",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = originalText.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Translation (${targetLang.displayName})",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldReady,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = chineseText.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = EmeraldReady
                )
            }
        }
    }
}

private fun rebuildTimestampedSegments(
    originalText: String,
    chineseText: String,
    durationMs: Long,
    sourceLang: SupportedLanguage,
    targetLang: SupportedLanguage
): List<TranscriptSegment> {
    val origLines = originalText.lines().map { it.trim() }.filter { it.isNotBlank() }
    val zhLines = chineseText.lines().map { it.trim() }.filter { it.isNotBlank() }
    val count = maxOf(origLines.size, zhLines.size)
    if (count == 0) return emptyList()
    val safeDuration = durationMs.coerceAtLeast(count * 2000L)
    val stepMs = safeDuration / count

    return List(count) { idx ->
        val start = idx * stepMs
        val end = ((idx + 1) * stepMs).coerceAtMost(safeDuration)
        TranscriptSegment(
            id = UUID.randomUUID().toString(),
            startMs = start,
            endMs = end,
            originalText = origLines.getOrElse(idx) { "" },
            translatedText = zhLines.getOrElse(idx) { "" },
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            isFinal = true
        )
    }
}

@Composable
private fun OnlineTranslationWorkbenchCard(
    recording: RecordingEntity,
    onlineState: OnlineTranslationState,
    onInputChange: (String) -> Unit,
    onTranslate: (String) -> Unit,
    onAppendSegment: (RecordingEntity, String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val srcLang = SupportedLanguage.fromCode(recording.sourceLanguageCode)
    val tgtLang = SupportedLanguage.fromCode(recording.targetLanguageCode)

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI 智能翻译工作台 (Gemini Flash)",
                        style = MaterialTheme.typography.titleSmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${srcLang.displayName} → ${tgtLang.displayName}",
                    style = MonoTimestampStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "针对菲语 Tagalog、宿务语 Cebuano (Bisaya) 与英语的自然讲道与口语语境，提供元宝级精准流利翻译。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = onlineState.inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("online_translation_input"),
                placeholder = {
                    Text(
                        "输入待翻译的句子或讲道片段（例：Ang Dios mao ang atong kalig-on ug dalangpanan...）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onTranslate(onlineState.inputText) },
                    enabled = onlineState.inputText.isNotBlank() && !onlineState.isTranslating,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("execute_translation_button")
                ) {
                    if (onlineState.isTranslating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NavyVoid,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("翻译中...")
                    } else {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即翻译")
                    }
                }
            }

            if (onlineState.translatedText.isNotBlank()) {
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "中文翻译结果：",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldReady
                        )
                        Text(
                            text = onlineState.translatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Translated Text", onlineState.translatedText))
                                    onShowMessage("已复制翻译结果。")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("复制")
                            }
                            Button(
                                onClick = {
                                    onAppendSegment(recording, onlineState.inputText, onlineState.translatedText)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("加入字幕")
                            }
                        }
                    }
                }
            }

            onlineState.errorMessage?.let { err ->
                Text(
                    text = "翻译提示: $err",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberNotice
                )
            }
        }
    }
}
