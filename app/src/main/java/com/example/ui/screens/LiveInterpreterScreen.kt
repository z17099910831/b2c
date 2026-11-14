package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.ContextCompat
import com.example.ai.speech.RecognizerReadiness
import com.example.audio.AudioRecordingState
import com.example.data.local.UserPreferences
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TranscriptSegment
import com.example.ui.LiveInterpreterUiState
import com.example.ui.ReadinessUiState
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberNoticeContainer
import com.example.ui.theme.CoralActive
import com.example.ui.theme.CoralActiveContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.EmeraldReadyContainer
import com.example.ui.theme.MonoTimestampStyle
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

@Composable
fun LiveInterpreterScreen(
    liveState: LiveInterpreterUiState,
    preferences: UserPreferences,
    readiness: ReadinessUiState,
    onSelectSourceLanguage: (SupportedLanguage) -> Unit,
    onSelectTargetLanguage: (SupportedLanguage) -> Unit,
    onSwapLanguages: () -> Unit,
    onToggleOnlineAi: (Boolean) -> Unit,
    onSendLiveSpeech: (String) -> Unit,
    onRequestStartRecording: (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopAndSaveRecording: () -> Unit,
    onOpenModelManager: () -> Unit,
    onPermissionDeniedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var liveInputText by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            onRequestStartRecording(true) {}
        } else {
            onPermissionDeniedMessage("Microphone permission denied. Grant RECORD_AUDIO in settings to record locally.")
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LiveModeToggleCard(
                isOnline = preferences.isOnlineAiEnabled,
                onToggleOnline = onToggleOnlineAi,
                readiness = readiness,
                onOpenModelManager = onOpenModelManager
            )
        }

        item {
            LanguagePairSelectorCard(
                sourceLanguage = preferences.sourceLanguage,
                targetLanguage = preferences.targetLanguage,
                enabled = liveState.recordingState == AudioRecordingState.IDLE,
                onSelectSource = onSelectSourceLanguage,
                onSelectTarget = onSelectTargetLanguage,
                onSwap = onSwapLanguages
            )
        }

        item {
            WaveformVisualizer(
                bars = liveState.amplitudeBars,
                recordingState = liveState.recordingState
            )
        }

        item {
            LiveRecordingControlDeck(
                liveState = liveState,
                isOnlineAi = preferences.isOnlineAiEnabled,
                onStart = {
                    hasMicPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    onRequestStartRecording(hasMicPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onPause = onPauseRecording,
                onResume = onResumeRecording,
                onStopAndSave = onStopAndSaveRecording
            )
        }

        item {
            SimultaneousInterpretationBar(
                inputText = liveInputText,
                onInputChange = { liveInputText = it },
                onSend = {
                    if (liveInputText.isNotBlank()) {
                        onSendLiveSpeech(liveInputText)
                        liveInputText = ""
                    }
                },
                isInterpreting = liveState.isInterpretingOnline,
                sourceLang = preferences.sourceLanguage,
                targetLang = preferences.targetLanguage
            )
        }

        item {
            BilingualLiveFeedHeader(
                sourceLanguage = preferences.sourceLanguage,
                targetLanguage = preferences.targetLanguage,
                chunksCaptured = liveState.chunksCaptured,
                diagnostic = liveState.engineDiagnostic,
                isInterpretingOnline = liveState.isInterpretingOnline
            )
        }

        if (liveState.liveSegments.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NavyCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, NavyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bilingual Subtitles Ready (${preferences.sourceLanguage.displayName} → ${preferences.targetLanguage.displayName})",
                            style = MaterialTheme.typography.titleSmall,
                            color = ElectricCyan
                        )
                        Text(
                            text = if (liveState.recordingState == AudioRecordingState.IDLE) {
                                "Tap the microphone button to start local 16 kHz WAV recording. Recorded audio stays strictly on-device and is saved to your Recording Library when stopped."
                            } else {
                                "Recording local audio chunks (${liveState.chunksCaptured} captured). Real speech-to-text & NLLB translation require installed offline model weights and native runtime integration."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(liveState.liveSegments, key = { it.id }) { segment ->
                BilingualSubtitleCard(segment = segment)
            }
        }
    }
}

@Composable
private fun LiveModeToggleCard(
    isOnline: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    readiness: ReadinessUiState,
    onOpenModelManager: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isOnline) EmeraldReadyContainer.copy(alpha = 0.4f) else NavyCardElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isOnline) EmeraldReady.copy(alpha = 0.5f) else NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = if (isOnline) EmeraldReady else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isOnline) "在线 AI 同传模式 (Gemini 3.8 Flash)" else "离线隐私录音模式 (Whisper & NLLB)",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isOnline) EmeraldReady else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isOnline,
                    onCheckedChange = onToggleOnline,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldReady,
                        checkedTrackColor = EmeraldReadyContainer
                    ),
                    modifier = Modifier.testTag("interpreter_online_switch")
                )
            }
            Text(
                text = if (isOnline) {
                    "像腾讯元宝一样流式同传，边说边译，低延迟呈现双语字幕，转录后可一键提炼深度会议/讲道纪要。"
                } else {
                    "100% 本地 16kHz WAV 音频采集与离线 NLLB 翻译。模型状态：${readiness.summary.statusHeadline}。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isOnline && !readiness.summary.isFullOfflineInferenceReady) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onOpenModelManager,
                        modifier = Modifier.testTag("interpreter_models_button")
                    ) {
                        Text("离线模型管理")
                    }
                }
            }
        }
    }
}

@Composable
private fun SimultaneousInterpretationBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isInterpreting: Boolean,
    sourceLang: SupportedLanguage,
    targetLang: SupportedLanguage
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isInterpreting) EmeraldReady else NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = EmeraldReady,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI 实时同传输入 (${sourceLang.shortTag} → ${targetLang.shortTag})",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldReady,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isInterpreting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = EmeraldReady,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "流式翻译中...",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldReady
                        )
                    }
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simultaneous_input_field"),
                placeholder = {
                    Text(
                        "输入语音识别文本或粘贴对话（例：Salamat sa Dios / Kamusta ka po）...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = NavyBorder,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick suggested phrases chips
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val suggestions = when (sourceLang) {
                        SupportedLanguage.TAGALOG -> listOf("Salamat sa Diyos", "Magandang umaga")
                        SupportedLanguage.CEBUANO -> listOf("Salamat sa Ginoo", "Maayong adlaw")
                        SupportedLanguage.ENGLISH -> listOf("Praise the Lord", "Good morning")
                        SupportedLanguage.SIMPLIFIED_CHINESE -> listOf("感谢上帝", "平安喜乐")
                    }
                    suggestions.forEach { chipText ->
                        Surface(
                            onClick = { onInputChange(chipText) },
                            color = NavyCardElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NavyBorder)
                        ) {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSend,
                    enabled = inputText.isNotBlank() && !isInterpreting,
                    modifier = Modifier.testTag("send_interpretation_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "启动同传",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("同传")
                }
            }
        }
    }
}

@Composable
private fun LanguagePairSelectorCard(
    sourceLanguage: SupportedLanguage,
    targetLanguage: SupportedLanguage,
    enabled: Boolean,
    onSelectSource: (SupportedLanguage) -> Unit,
    onSelectTarget: (SupportedLanguage) -> Unit,
    onSwap: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageDropdownChip(
                label = "SOURCE",
                selected = sourceLanguage,
                enabled = enabled,
                onSelect = onSelectSource,
                testTag = "source_language_selector",
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSwap,
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("swap_languages_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap source and target languages",
                    tint = if (enabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LanguageDropdownChip(
                label = "TARGET",
                selected = targetLanguage,
                enabled = enabled,
                onSelect = onSelectTarget,
                testTag = "target_language_selector",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LanguageDropdownChip(
    label: String,
    selected: SupportedLanguage,
    enabled: Boolean,
    onSelect: (SupportedLanguage) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            color = NavyCardElevated,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NavyBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = "$label • ${selected.nllbCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selected.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SupportedLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(lang.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${lang.nativeName} (${lang.nllbCode})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(lang)
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveRecordingControlDeck(
    liveState: LiveInterpreterUiState,
    isOnlineAi: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAndSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = TranscriptSegment.formatMillis(liveState.elapsedMillis),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = MonoTimestampStyle.fontFamily
                ),
                color = when (liveState.recordingState) {
                    AudioRecordingState.RECORDING -> CoralActive
                    AudioRecordingState.PAUSED, AudioRecordingState.INTERRUPTED -> AmberNotice
                    AudioRecordingState.IDLE -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = when (liveState.recordingState) {
                    AudioRecordingState.IDLE -> if (isOnlineAi) "云端 AI 同传待命 • 16 kHz Mono PCM 本地高保真" else "离线录音待命 (16 kHz Mono PCM)"
                    AudioRecordingState.RECORDING -> if (isOnlineAi) "Gemini Flash 同传中 • 本地保存音频与字幕" else "离线录音中 • ${liveState.chunksCaptured} chunk(s)"
                    AudioRecordingState.PAUSED -> "已暂停 • 本地 WAV 录音完整保持"
                    AudioRecordingState.INTERRUPTED -> "音频焦点中断 • 已安全自动暂存"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            liveState.statusNotice?.let { notice ->
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberNotice
                )
            }

            if (liveState.recordingState == AudioRecordingState.IDLE) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .size(88.dp)
                        .testTag("mic_start_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isOnlineAi) EmeraldReady else ElectricCyan)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Start local microphone recording",
                        modifier = Modifier.size(38.dp)
                    )
                }
                Text(
                    text = if (isOnlineAi) "点击开始元宝式同传录音" else "Tap to Start Recording",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isOnlineAi) EmeraldReady else ElectricCyan
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (liveState.recordingState == AudioRecordingState.RECORDING) {
                        OutlinedButton(
                            onClick = onPause,
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1f)
                                .testTag("mic_pause_button"),
                            border = BorderStroke(1.dp, AmberNotice)
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null, tint = AmberNotice)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", color = AmberNotice)
                        }
                    } else {
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1f)
                                .testTag("mic_resume_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume")
                        }
                    }

                    Button(
                        onClick = onStopAndSave,
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f)
                            .testTag("mic_stop_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralActiveContainer,
                            contentColor = CoralActive
                        ),
                        border = BorderStroke(1.dp, CoralActive)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop & Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun BilingualLiveFeedHeader(
    sourceLanguage: SupportedLanguage,
    targetLanguage: SupportedLanguage,
    chunksCaptured: Int,
    diagnostic: String,
    isInterpretingOnline: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bilingual Live Feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isInterpretingOnline) {
                    Surface(
                        color = EmeraldReadyContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⚡ 流式同传中",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldReady,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = "${sourceLanguage.shortTag} → ${targetLanguage.shortTag} • $chunksCaptured chunks",
                style = MonoTimestampStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = diagnostic,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BilingualSubtitleCard(segment: TranscriptSegment) {
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
                    text = "${segment.sourceLanguage.shortTag} → ${segment.targetLanguage.shortTag}",
                    style = MonoTimestampStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = segment.originalText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (segment.translatedText.isNotBlank()) {
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = segment.translatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = EmeraldReady,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
