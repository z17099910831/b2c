package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.RecordingEntity
import com.example.domain.model.DevelopmentStageItem
import com.example.domain.model.StageStatus
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.SupportedTranslationPairs
import com.example.domain.model.TranscriptSegment
import com.example.domain.model.TranslationDirection
import com.example.modelmanager.DeviceHardwareStatus
import com.example.ui.ReadinessUiState
import com.example.ui.components.PrivacyLocalBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberNoticeContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.EmeraldReadyContainer
import com.example.ui.theme.MonoTimestampStyle
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyVoid
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    readiness: ReadinessUiState,
    hardwareStatus: DeviceHardwareStatus,
    recentRecordings: List<RecordingEntity>,
    stages: List<DevelopmentStageItem>,
    isTabletLayout: Boolean,
    isOnlineAiEnabled: Boolean,
    onToggleOnlineAi: (Boolean) -> Unit,
    onLaunchDirection: (TranslationDirection) -> Unit,
    onOpenLiveInterpreter: () -> Unit,
    onOpenTranscription: () -> Unit,
    onOpenAiSummary: () -> Unit,
    onOpenModelManager: () -> Unit,
    onOpenRecording: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            OnlineAiEngineBanner(
                isOnline = isOnlineAiEnabled,
                isNetworkConnected = hardwareStatus.isInternetConnected,
                onToggleOnline = onToggleOnlineAi
            )
        }

        item {
            HeroReadinessBanner(
                readiness = readiness,
                isOnlineAi = isOnlineAiEnabled,
                onStartInterpreter = onOpenLiveInterpreter,
                onOpenModelManager = onOpenModelManager
            )
        }

        item {
            SectionHeader(
                title = "元宝四合一核心能力",
                subtitle = "记录 • 翻译 • 同传 • 总结，一站式多语言双轨处理"
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = if (isTabletLayout) 4 else 2
            ) {
                YuanbaoFeatureCard(
                    title = "实时记录",
                    subtitle = "16kHz本地高保真",
                    icon = Icons.Filled.Mic,
                    accentColor = ElectricCyan,
                    onClick = onOpenLiveInterpreter,
                    modifier = if (isTabletLayout) Modifier.weight(1f) else Modifier.weight(1f)
                )
                YuanbaoFeatureCard(
                    title = "双语同传",
                    subtitle = "实时流式翻译字幕",
                    icon = Icons.Filled.Bolt,
                    accentColor = EmeraldReady,
                    onClick = onOpenLiveInterpreter,
                    modifier = if (isTabletLayout) Modifier.weight(1f) else Modifier.weight(1f)
                )
                YuanbaoFeatureCard(
                    title = "在线翻译",
                    subtitle = "自然语境•讲道对话",
                    icon = Icons.Filled.Translate,
                    accentColor = ElectricCyan,
                    onClick = onOpenTranscription,
                    modifier = if (isTabletLayout) Modifier.weight(1f) else Modifier.weight(1f)
                )
                YuanbaoFeatureCard(
                    title = "深度总结",
                    subtitle = "纪要•要点•待办•经文",
                    icon = Icons.Filled.AutoAwesome,
                    accentColor = AmberNotice,
                    onClick = onOpenAiSummary,
                    modifier = if (isTabletLayout) Modifier.weight(1f) else Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeader(
                title = "Translation Directions",
                subtitle = "Prioritizing Tagalog & Cebuano to Simplified Chinese"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = if (isTabletLayout) 2 else 1
            ) {
                SupportedTranslationPairs.allDirections.forEach { direction ->
                    DirectionQuickCard(
                        direction = direction,
                        onClick = { onLaunchDirection(direction) },
                        modifier = if (isTabletLayout) {
                            Modifier.weight(1f, fill = true)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                }
            }
        }

        item {
            DeviceStatusCard(
                hardwareStatus = hardwareStatus,
                readiness = readiness,
                onOpenModelManager = onOpenModelManager
            )
        }

        if (recentRecordings.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Local Sessions",
                    subtitle = "${recentRecordings.size} stored privately on device"
                )
            }
            items(recentRecordings.take(3), key = { it.id }) { rec ->
                RecentRecordingCard(
                    recording = rec,
                    onClick = { onOpenRecording(rec.id) }
                )
            }
        }

        item {
            SectionHeader(
                title = "Architecture & Stage Readiness",
                subtitle = "Honest on-device capability tracker"
            )
            Spacer(modifier = Modifier.height(10.dp))
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
                    stages.forEach { stage ->
                        StageRow(stage = stage)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineAiEngineBanner(
    isOnline: Boolean,
    isNetworkConnected: Boolean,
    onToggleOnline: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isOnline) EmeraldReadyContainer.copy(alpha = 0.35f) else NavyCardElevated,
        border = BorderStroke(1.dp, if (isOnline) EmeraldReady.copy(alpha = 0.5f) else NavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = if (isOnline) EmeraldReady else NavyBorder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = NavyVoid,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isOnline) "在线 AI 模式 (Gemini 3.8 Flash)" else "100% 本地离线隐私模式",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOnline) EmeraldReady else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (isOnline) {
                            if (isNetworkConnected) "元宝级 AI 智能赋能：实时流式同传、上下文翻译与深度纪要。"
                            else "已启用云端 AI，当前设备网络离线，自动使用本地离线兜底。"
                        } else {
                            "零流量无联网，完全在掌机/设备本地运行录音与模型推理。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = isOnline,
                onCheckedChange = onToggleOnline,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EmeraldReady,
                    checkedTrackColor = EmeraldReadyContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = NavyCard
                ),
                modifier = Modifier.testTag("online_ai_mode_switch")
            )
        }
    }
}

@Composable
private fun YuanbaoFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("yuanbao_card_${title}"),
        color = NavyCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeroReadinessBanner(
    readiness: ReadinessUiState,
    isOnlineAi: Boolean,
    onStartInterpreter: () -> Unit,
    onOpenModelManager: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NavyBorder),
        color = NavyCard
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_offline_bridge),
                contentDescription = "LinguaBridge acoustic wave bridge",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.34f
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NavyVoid.copy(alpha = 0.45f),
                                NavyVoid.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrivacyLocalBadge()
                    if (isOnlineAi) {
                        Surface(
                            color = EmeraldReadyContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Gemini Flash 云端同传就绪",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldReady,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "多语言语音记录、同传与智能总结",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isOnlineAi) "Gemini 3.8 Flash 云端 AI 引擎已激活，具备流式双语同传与深度纪要能力。" else readiness.summary.statusHeadline,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isOnlineAi || readiness.summary.isFullOfflineInferenceReady) EmeraldReady else AmberNotice
                )
                Text(
                    text = "支持 Tagalog、Cebuano (Bisaya)、英语到简体中文的双语互译。可在在线超快 AI 同传与 100% 离线模型隐私记录间随心切换。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartInterpreter,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_open_interpreter_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isOnlineAi) "开始同传 / 录音" else "Live Interpreter", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onOpenModelManager,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_open_models_button"),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("离线模型管理", color = ElectricCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionQuickCard(
    direction: TranslationDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("direction_card_${direction.source.code}_${direction.target.code}"),
        color = if (direction.isPrimary) NavyCardElevated else NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (direction.isPrimary) ElectricCyan.copy(alpha = 0.45f) else NavyBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (direction.isPrimary) ElectricCyanContainer else NavyCardElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (direction.isPrimary) "PRIMARY" else "SECONDARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (direction.isPrimary) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = direction.nllbPairLabel,
                        style = MonoTimestampStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = direction.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = direction.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Launch ${direction.label}",
                tint = if (direction.isPrimary) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceStatusCard(
    hardwareStatus: DeviceHardwareStatus,
    readiness: ReadinessUiState,
    onOpenModelManager: () -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Text(
                        text = "Hardware & Offline Model Readiness",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenModelManager)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("device_status_manage_models")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = "Device RAM",
                    value = "${"%.1f".format(hardwareStatus.totalRamGb)} GB",
                    accent = if (hardwareStatus.totalRamMb >= 3072L) EmeraldReady else AmberNotice,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Free Storage",
                    value = "${"%.1f".format(hardwareStatus.freeStorageGb)} GB",
                    accent = if (hardwareStatus.freeStorageBytes >= 1_600_000_000L) EmeraldReady else AmberNotice,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Whisper / NLLB",
                    value = if (readiness.summary.modelsDownloaded) "Installed" else "0 / 2",
                    accent = if (readiness.summary.modelsDownloaded) EmeraldReady else AmberNotice,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (hardwareStatus.isInternetConnected) Icons.Filled.Security else Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (hardwareStatus.isInternetConnected) {
                        "Network available for optional one-time model download (user-confirmed only)."
                    } else {
                        "Device is currently offline. Local recording, Room DB, and summaries work normally."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = NavyCardElevated,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentRecordingCard(
    recording: RecordingEntity,
    onClick: () -> Unit
) {
    val src = SupportedLanguage.fromCode(recording.sourceLanguageCode)
    val tgt = SupportedLanguage.fromCode(recording.targetLanguageCode)
    val dateText = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        .format(Date(recording.createdAtMillis))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("recent_recording_${recording.id}"),
        color = NavyCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${src.shortTag} → ${tgt.shortTag} • $dateText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = TranscriptSegment.formatMillis(recording.durationMillis),
                style = MonoTimestampStyle,
                color = ElectricCyan
            )
        }
    }
}

@Composable
private fun StageRow(stage: DevelopmentStageItem) {
    val (color, bg, icon, tag) = when (stage.status) {
        StageStatus.IMPLEMENTED -> listOf(EmeraldReady, EmeraldReadyContainer, Icons.Filled.CheckCircle, "Active")
        StageStatus.INTERFACE_READY -> listOf(ElectricCyan, ElectricCyanContainer, Icons.Filled.Translate, "Bridge Ready")
        StageStatus.PENDING_ON_DEVICE_MODELS -> listOf(AmberNotice, AmberNoticeContainer, Icons.Filled.Pending, "Pending Models")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
            contentDescription = null,
            tint = color as androidx.compose.ui.graphics.Color,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Stage ${stage.stageNumber}: ${stage.title}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stage.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = bg as androidx.compose.ui.graphics.Color,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = tag as String,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
