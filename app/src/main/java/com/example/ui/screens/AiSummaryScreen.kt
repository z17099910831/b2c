package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RecordingEntity
import com.example.data.local.RecordingSerialization
import com.example.domain.model.StructuredSummary
import com.example.domain.model.TranscriptSegment
import com.example.ui.components.EmptyAudioBanner
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberNoticeContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.EmeraldReadyContainer
import com.example.ui.theme.MonoTimestampStyle
import com.example.ui.theme.NavyBorder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSummaryScreen(
    recordings: List<RecordingEntity>,
    selectedRecording: RecordingEntity?,
    isOnlineAiEnabled: Boolean,
    isGeneratingSummary: Boolean,
    onSelectRecording: (String) -> Unit,
    onGenerateYuanbaoSummary: (RecordingEntity) -> Unit,
    onGenerateOfflineSummary: (RecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (selectedRecording == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyAudioBanner(
                title = "No Recording Selected for AI Summary",
                subtitle = "Record or select a session from Recording Library to extract offline Chinese summaries, keywords, action items, and Bible references."
            )
        }
        return
    }

    val summary = remember(selectedRecording.id, selectedRecording.summaryJson) {
        RecordingSerialization.decodeSummary(selectedRecording.summaryJson)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SummaryRecordingPicker(
                recordings = recordings,
                selected = selectedRecording,
                onSelect = onSelectRecording
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavyCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (isOnlineAiEnabled) EmeraldReady.copy(alpha = 0.5f) else NavyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnlineAiEnabled) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (isOnlineAiEnabled) EmeraldReady else ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isOnlineAiEnabled) "元宝 AI 深度总结引擎 (Gemini 3.8 Flash)" else "本地离线提取式总结引擎 (Zero Internet)",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOnlineAiEnabled) EmeraldReady else ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isOnlineAiEnabled) {
                            "像腾讯元宝一样全面理解长录音与双语转录：智能归纳核心摘要、提取议题关键词、梳理核心论点、列出明确行动指南，并精确引证圣经章节。"
                        } else {
                            "零流量完全在设备端运行，提取核心原句与高频关键词。可在主页右上角或设置中启用云端 AI 获得更深度的元宝式理解总结。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onGenerateYuanbaoSummary(selectedRecording) },
                            enabled = !isGeneratingSummary,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("generate_yuanbao_summary_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ 元宝 AI 深度总结", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = { onGenerateOfflineSummary(selectedRecording) },
                            enabled = !isGeneratingSummary,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("generate_offline_summary_button")
                        ) {
                            Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🛡️ 离线提取总结", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (isGeneratingSummary) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = EmeraldReadyContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = EmeraldReady,
                            strokeWidth = 2.5.dp
                        )
                        Column {
                            Text(
                                text = "AI 正在分析双语转录...",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldReady
                            )
                            Text(
                                text = "正在生成结构化内容概要、讨论要点、行动指南与圣经经文引用...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (summary != null) {
            item {
                SummaryCard(
                    icon = Icons.Filled.Summarize,
                    title = "中文内容概要 (Executive Summary)",
                    accent = if (summary.isExtractiveOnly) ElectricCyan else EmeraldReady
                ) {
                    Text(
                        text = summary.chineseSummary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Engine: ${summary.engineVersion}",
                            style = MonoTimestampStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                val fullText = buildString {
                                    appendLine("【内容概要】")
                                    appendLine(summary.chineseSummary)
                                    if (summary.keyPoints.isNotEmpty()) {
                                        appendLine("\n【核心要点】")
                                        summary.keyPoints.forEach { appendLine("• $it") }
                                    }
                                    if (summary.actionItems.isNotEmpty()) {
                                        appendLine("\n【待办行动事项】")
                                        summary.actionItems.forEach { appendLine("☑ $it") }
                                    }
                                    if (summary.bibleReferences.isNotEmpty()) {
                                        appendLine("\n【经文引用】")
                                        summary.bibleReferences.forEach { appendLine("📖 $it") }
                                    }
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Summary", fullText))
                            },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制全文纪要")
                        }
                    }
                }
            }

            item {
                SummaryCard(
                    icon = Icons.Filled.Tag,
                    title = "关键词与主题 (Keywords & Topics)",
                    accent = EmeraldReady
                ) {
                    if (summary.topics.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            summary.topics.forEach { topic ->
                                Text(
                                    text = "• $topic",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldReady
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (summary.keywords.isEmpty()) {
                        Text(
                            text = "No recurring keywords extracted yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            summary.keywords.forEach { kw ->
                                Surface(
                                    color = ElectricCyanContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = kw,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SummaryListCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "核心要点 (Key Points)",
                    items = summary.keyPoints,
                    emptyText = "No key points extracted from current transcript.",
                    accent = ElectricCyan
                )
            }

            item {
                SummaryListCard(
                    icon = Icons.Filled.PlaylistAddCheck,
                    title = "行动事项 (Action Items)",
                    items = summary.actionItems,
                    emptyText = "No explicit action items detected in transcript.",
                    accent = AmberNotice
                )
            }

            item {
                SummaryCard(
                    icon = Icons.Filled.Book,
                    title = "圣经经文引用 (Explicit Bible References)",
                    accent = AmberNotice
                ) {
                    if (summary.bibleReferences.isEmpty()) {
                        Text(
                            text = "No Bible verse references were explicitly mentioned in this transcript.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            summary.bibleReferences.forEach { ref ->
                                Surface(
                                    color = AmberNoticeContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AmberNotice.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = ref,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AmberNotice,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRecordingPicker(
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
                .testTag("summary_recording_picker")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOURCE TRANSCRIPT",
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
                Text(
                    text = TranscriptSegment.formatMillis(selected.durationMillis),
                    style = MonoTimestampStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            recordings.forEach { rec ->
                DropdownMenuItem(
                    text = { Text(rec.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
private fun SummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SummaryListCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    items: List<String>,
    emptyText: String,
    accent: androidx.compose.ui.graphics.Color
) {
    SummaryCard(icon = icon, title = title, accent = accent) {
        if (items.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { entry ->
                    Surface(
                        color = NavyCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
