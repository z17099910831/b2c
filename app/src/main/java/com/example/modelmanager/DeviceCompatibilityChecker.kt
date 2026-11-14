package com.example.modelmanager

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs

enum class CompatibilityStatus(val label: String) {
    SUPPORTED("Supported"),
    SUPPORTED_WITH_LIMITATIONS("Supported with limitations"),
    NOT_SUPPORTED("Not supported")
}

data class DeviceHardwareStatus(
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val androidVersion: String = Build.VERSION.RELEASE ?: "Unknown",
    val totalRamMb: Long,
    val availableRamMb: Long,
    val freeStorageBytes: Long,
    val totalStorageBytes: Long,
    val primaryAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
    val supportedAbis: List<String>,
    val nnapiAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1,
    val vulkanGpuAvailable: Boolean = false,
    val recommendedTier: ModelTier = ModelTier.STANDARD,
    val compatibilityStatus: CompatibilityStatus = CompatibilityStatus.SUPPORTED,
    val compatibilityNotes: List<String> = emptyList(),
    val isInternetConnected: Boolean
) {
    val totalRamGb: Double get() = totalRamMb / 1024.0
    val availableRamGb: Double get() = availableRamMb / 1024.0
    val freeStorageGb: Double get() = freeStorageBytes / (1024.0 * 1024.0 * 1024.0)
    val meetsBaselineOfflineSpec: Boolean
        get() = freeStorageBytes >= 1_600_000_000L
}

typealias DeviceCapabilityManager = DeviceCompatibilityChecker

class DeviceCompatibilityChecker(
    private val context: Context
) {
    fun inspectHardware(): DeviceHardwareStatus {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024L * 1024L)
        val availableRamMb = memInfo.availMem / (1024L * 1024L)

        val statFs = StatFs(context.filesDir.absolutePath)
        val freeBytes = statFs.availableBytes
        val totalBytes = statFs.totalBytes

        val abis = Build.SUPPORTED_ABIS.toList()
        val primaryAbi = abis.firstOrNull() ?: "unknown"
        val apiLevel = Build.VERSION.SDK_INT
        val nnapiSupported = apiLevel >= Build.VERSION_CODES.O_MR1
        val vulkanSupported = if (apiLevel >= Build.VERSION_CODES.N) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        } else {
            false
        }

        val recommendedTier = recommendTierForRam(totalRamMb)
        val (status, notes) = evaluateCompatibility(
            apiLevel = apiLevel,
            supportedAbis = abis,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            freeStorageBytes = freeBytes,
            nnapiSupported = nnapiSupported
        )

        val connected = checkNetworkConnected()

        return DeviceHardwareStatus(
            apiLevel = apiLevel,
            androidVersion = Build.VERSION.RELEASE ?: apiLevel.toString(),
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            freeStorageBytes = freeBytes,
            totalStorageBytes = totalBytes,
            primaryAbi = primaryAbi,
            supportedAbis = abis,
            nnapiAvailable = nnapiSupported,
            vulkanGpuAvailable = vulkanSupported,
            recommendedTier = recommendedTier,
            compatibilityStatus = status,
            compatibilityNotes = notes,
            isInternetConnected = connected
        )
    }

    fun recommendTierForRam(totalRamMb: Long): ModelTier = when {
        totalRamMb >= 6144L -> ModelTier.HIGH_PERFORMANCE
        totalRamMb >= 3072L -> ModelTier.STANDARD
        else -> ModelTier.LIGHTWEIGHT
    }

    fun evaluateCompatibility(
        apiLevel: Int,
        supportedAbis: List<String>,
        totalRamMb: Long,
        availableRamMb: Long,
        freeStorageBytes: Long,
        nnapiSupported: Boolean
    ): Pair<CompatibilityStatus, List<String>> {
        val notes = mutableListOf<String>()
        val hasArm64 = supportedAbis.any { it.equals("arm64-v8a", ignoreCase = true) }
        val hasArm32 = supportedAbis.any { it.equals("armeabi-v7a", ignoreCase = true) }
        val hasX8664 = supportedAbis.any { it.equals("x86_64", ignoreCase = true) }

        if (apiLevel < 24) {
            notes += "Android API $apiLevel is below recommended Android 7.0 (API 24); NNAPI and modern audio focus APIs unavailable."
            return CompatibilityStatus.NOT_SUPPORTED to notes
        }
        if (!hasArm64 && !hasArm32 && !hasX8664) {
            notes += "CPU ABI (${supportedAbis.joinToString()}) is not among supported ABIs (arm64-v8a / armeabi-v7a / x86_64)."
            return CompatibilityStatus.NOT_SUPPORTED to notes
        }

        var hasLimitations = false
        if (!hasArm64) {
            hasLimitations = true
            notes += "Non-arm64 primary ABI (${supportedAbis.firstOrNull()}): local audio, Room DB, summary & export work fully; heavy 64-bit native LLMs may be constrained."
        }
        if (totalRamMb < 2500L || availableRamMb < 1024L) {
            hasLimitations = true
            notes += "Limited RAM (${totalRamMb} MB total / ${availableRamMb} MB free): use Lightweight Whisper Tiny (~32 MB) instead of Small/Medium."
        }
        if (freeStorageBytes < 600_000_000L) {
            hasLimitations = true
            notes += "Low internal storage (${"%.2f".format(freeStorageBytes / (1024.0 * 1024.0 * 1024.0))} GB free): insufficient for full Standard offline models (~1.5 GB), Lightweight/cloud supported."
        }
        if (!nnapiSupported) {
            hasLimitations = true
            notes += "NNAPI hardware acceleration unavailable on Android < 8.1; CPU fallback applies."
        }

        val status = if (hasLimitations) {
            CompatibilityStatus.SUPPORTED_WITH_LIMITATIONS
        } else {
            CompatibilityStatus.SUPPORTED
        }
        if (notes.isEmpty()) {
            notes += "Device meets full hardware specifications (64-bit ABI, adequate RAM, NNAPI acceleration)."
        }
        return status to notes
    }

    private fun checkNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun hasSufficientMemoryForModel(minRamMb: Long): Boolean {
        val status = inspectHardware()
        return status.totalRamMb >= minRamMb
    }
}
