package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.modelmanager.DeviceHardwareStatus
import com.example.modelmanager.ManagedModelItem
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.CoralActive
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NavyCardElevated

@Composable
fun RecordingConsentDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCardElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = ElectricCyan
            )
        },
        title = {
            Text(
                text = "Local Recording & Privacy Notice",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LinguaBridge AI records audio strictly to your device's internal private storage as local WAV files.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• No audio is ever uploaded to any cloud server.\n• No cloud transcription or external LLM API is used.\n• Please ensure participants consent to local recording before starting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.testTag("accept_consent_button")
            ) {
                Text("I Understand & Consent")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_consent_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ModelDownloadConfirmationDialog(
    modelItem: ManagedModelItem,
    hardwareStatus: DeviceHardwareStatus,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCardElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = ElectricCyan
            )
        },
        title = {
            Text(
                text = "Confirm Offline Model Download",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${modelItem.spec.name} (${modelItem.spec.version})",
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricCyan
                )
                Text(
                    text = "Download Size: ${modelItem.spec.sizeFormatted}\nLicense: ${modelItem.spec.licenseName}\nFree Storage: ${"%.2f".format(hardwareStatus.freeStorageGb)} GB\nDevice RAM: ${"%.1f".format(hardwareStatus.totalRamGb)} GB (Min ${modelItem.spec.minRamMb / 1024.0} GB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!modelItem.meetsRamRequirement || !modelItem.meetsStorageRequirement) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Warning: Device RAM or free storage is below the recommended minimum for this model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberNotice
                    )
                }
                Text(
                    text = "Models are downloaded once over internet (Wi-Fi recommended). After installation, offline inference stays strictly local on device.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = modelItem.meetsStorageRequirement && hardwareStatus.isInternetConnected,
                modifier = Modifier.testTag("confirm_model_download_button")
            ) {
                Text(
                    if (!hardwareStatus.isInternetConnected) "Offline (No Network)"
                    else "Download (${modelItem.spec.sizeFormatted})"
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_model_download_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCardElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = CoralActive
            )
        },
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CoralActive),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete Permanently")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Keep")
            }
        }
    )
}
