package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.modelmanager.CompatibilityStatus
import com.example.modelmanager.DeviceHardwareStatus
import com.example.modelmanager.ManagedModelItem
import com.example.modelmanager.ModelInstallState
import com.example.modelmanager.ModelTier
import com.example.ui.ReadinessUiState
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberNoticeContainer
import com.example.ui.theme.CoralActive
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.EmeraldReadyContainer
import com.example.ui.theme.MonoTimestampStyle
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated

@Composable
fun ModelManagerScreen(
    models: List<ManagedModelItem>,
    hardwareStatus: DeviceHardwareStatus,
    readiness: ReadinessUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRequestDownload: (ManagedModelItem) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("model_manager_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back from Model Manager"
                        )
                    }
                    Column {
                        Text(
                            text = "Offline Model Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target Offline Footprint: 1–3 GB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("model_manager_refresh_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh model storage checks")
                }
            }
        }

        item {
            HardwareCompatibilitySummaryCard(
                hardwareStatus = hardwareStatus,
                readiness = readiness
            )
        }

        items(models, key = { it.spec.id }) { item ->
            ModelPackageCard(
                item = item,
                onRequestDownload = { onRequestDownload(item) },
                onCancelDownload = { onCancelDownload(item.spec.id) },
                onDeleteModel = { onDeleteModel(item.spec.id) }
            )
        }
    }
}

@Composable
private fun HardwareCompatibilitySummaryCard(
    hardwareStatus: DeviceHardwareStatus,
    readiness: ReadinessUiState
) {
    val installedGb = readiness.summary.installedModelBytes / (1024.0 * 1024.0 * 1024.0)
    val statusColor = when (hardwareStatus.compatibilityStatus) {
        CompatibilityStatus.SUPPORTED -> EmeraldReady
        CompatibilityStatus.SUPPORTED_WITH_LIMITATIONS -> AmberNotice
        CompatibilityStatus.NOT_SUPPORTED -> CoralActive
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    Icon(Icons.Filled.Memory, contentDescription = null, tint = ElectricCyan)
                    Text(
                        text = "Device Compatibility Report",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = hardwareStatus.compatibilityStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("device_compatibility_status_badge")
                    )
                }
            }
            Text(
                text = "Android ${hardwareStatus.androidVersion} (API ${hardwareStatus.apiLevel}) • Primary ABI: ${hardwareStatus.primaryAbi}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "RAM: ${"%.1f".format(hardwareStatus.totalRamGb)} GB total (${hardwareStatus.availableRamMb} MB avail) • Storage: ${"%.2f".format(hardwareStatus.freeStorageGb)} GB free",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "ABIs: ${hardwareStatus.supportedAbis.joinToString(", ")} • NNAPI: ${if (hardwareStatus.nnapiAvailable) "Yes" else "No"} • Vulkan GPU: ${if (hardwareStatus.vulkanGpuAvailable) "Yes" else "No"}",
                style = MonoTimestampStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Recommended Model Tier: ${hardwareStatus.recommendedTier.label} • Installed: ${"%.2f".format(installedGb)} GB",
                style = MaterialTheme.typography.bodySmall,
                color = ElectricCyan
            )
            hardwareStatus.compatibilityNotes.forEach { note ->
                Text(
                    text = "• $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val onnxReport = readiness.onnxReport
            if (onnxReport != null) {
                Text(
                    text = "ONNX INT8 RAM OK: ${if (onnxReport.meetsQuantizedInt8RamRequirement) "Yes" else "Low RAM (Use Lightweight)"} • Native Runtime: ${if (onnxReport.onnxRuntimeClassLinked) "Linked" else "Pending Android Studio AAR"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (onnxReport.onnxRuntimeClassLinked) EmeraldReady else AmberNotice
                )
            }
        }
    }
}

@Composable
private fun ModelPackageCard(
    item: ManagedModelItem,
    onRequestDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: () -> Unit
) {
    val spec = item.spec
    val state = item.state

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("model_card_${spec.id}"),
        color = NavyCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            when (state) {
                is ModelInstallState.Installed -> EmeraldReady.copy(alpha = 0.5f)
                is ModelInstallState.Error -> CoralActive.copy(alpha = 0.5f)
                else -> NavyBorder
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tierColor = when (spec.tier) {
                            ModelTier.LIGHTWEIGHT -> EmeraldReady
                            ModelTier.STANDARD -> ElectricCyan
                            ModelTier.HIGH_PERFORMANCE -> AmberNotice
                        }
                        Surface(
                            color = tierColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = spec.tier.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = tierColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "${spec.sizeFormatted} • ${spec.version}",
                            style = MonoTimestampStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = spec.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = spec.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Languages: ${spec.supportedLanguages}\nLicense: ${spec.licenseName} • Min RAM: ${"%.1f".format(spec.minRamMb / 1024.0)} GB • Storage Needed: ${spec.sizeFormatted}",
                style = MonoTimestampStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!item.meetsRamRequirement) {
                Surface(
                    color = AmberNoticeContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = AmberNotice,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Device RAM is below recommended ${"%.1f".format(spec.minRamMb / 1024.0)} GB.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberNotice
                        )
                    }
                }
            }

            when (state) {
                is ModelInstallState.NotInstalled -> {
                    Button(
                        onClick = onRequestDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("download_model_${spec.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download (${spec.sizeFormatted})")
                    }
                }
                is ModelInstallState.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading ${(state.progress * 100).toInt()}%…",
                                style = MonoTimestampStyle,
                                color = ElectricCyan
                            )
                            OutlinedButton(onClick = onCancelDownload) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
                is ModelInstallState.VerifyingChecksum -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Preparing downloaded model…",
                            style = MonoTimestampStyle,
                            color = EmeraldReady
                        )
                    }
                }
                is ModelInstallState.Installed -> {
                    Surface(
                        color = EmeraldReadyContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldReady)
                                Text(
                                    text = "Installed & Ready (SHA-256 Skipped)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EmeraldReady,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onDeleteModel,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_model_${spec.id}")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${spec.name}", tint = CoralActive)
                            }
                        }
                    }
                }
                is ModelInstallState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralActive
                        )
                        OutlinedButton(
                            onClick = onRequestDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Download")
                        }
                    }
                }
            }
        }
    }
}
