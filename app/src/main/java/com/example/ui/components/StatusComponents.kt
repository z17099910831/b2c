package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.AudioRecordingState
import com.example.modelmanager.OfflineReadinessSummary
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberNoticeContainer
import com.example.ui.theme.CoralActive
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.EmeraldReadyContainer
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.TextMuted

@Composable
fun OfflineReadinessPill(
    readiness: OfflineReadinessSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, accentColor, icon, label) = when {
        readiness.isFullOfflineInferenceReady -> Quadruple(
            EmeraldReadyContainer,
            EmeraldReady,
            Icons.Filled.CheckCircle,
            "Offline Ready"
        )
        readiness.modelsDownloaded -> Quadruple(
            AmberNoticeContainer,
            AmberNotice,
            Icons.Filled.WarningAmber,
            "Models Ready"
        )
        else -> Quadruple(
            NavyCardElevated,
            AmberNotice,
            Icons.Filled.DownloadForOffline,
            "Models Needed"
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("offline_readiness_pill"),
        color = bgColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PrivacyLocalBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = EmeraldReadyContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "On-device privacy guarantee",
                tint = EmeraldReady,
                modifier = Modifier.size(14.dp)
            )
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = EmeraldReady,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "100% On-Device • Zero Audio Upload",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldReady
            )
        }
    }
}

@Composable
fun WaveformVisualizer(
    bars: List<Float>,
    recordingState: AudioRecordingState,
    modifier: Modifier = Modifier
) {
    val activeBrush = when (recordingState) {
        AudioRecordingState.RECORDING -> Brush.verticalGradient(
            listOf(ElectricCyan, EmeraldReady)
        )
        AudioRecordingState.PAUSED, AudioRecordingState.INTERRUPTED -> Brush.verticalGradient(
            listOf(AmberNotice, AmberNotice.copy(alpha = 0.65f))
        )
        AudioRecordingState.IDLE -> Brush.verticalGradient(
            listOf(NavyBorder, TextMuted.copy(alpha = 0.5f))
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            when (recordingState) {
                AudioRecordingState.RECORDING -> ElectricCyan.copy(alpha = 0.5f)
                AudioRecordingState.INTERRUPTED -> CoralActive.copy(alpha = 0.6f)
                else -> NavyBorder
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = bars.size.coerceAtLeast(1)
                val totalWidth = size.width
                val step = totalWidth / count
                val barWidth = (step * 0.58f).coerceIn(3f, 14f)
                val centerY = size.height / 2f

                bars.forEachIndexed { index, amplitude ->
                    val clamped = amplitude.coerceIn(0.05f, 1f)
                    val barHeight = (size.height * clamped).coerceAtLeast(6f)
                    val left = index * step + (step - barWidth) / 2f
                    val top = centerY - barHeight / 2f
                    drawRoundRect(
                        brush = activeBrush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun EmptyAudioBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
