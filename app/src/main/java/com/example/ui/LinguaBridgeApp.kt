package com.example.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.ModelDownloadConfirmationDialog
import com.example.ui.components.OfflineReadinessPill
import com.example.ui.components.RecordingConsentDialog
import com.example.ui.screens.AiSummaryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveInterpreterScreen
import com.example.ui.screens.ModelManagerScreen
import com.example.ui.screens.RecordingLibraryScreen
import com.example.ui.screens.TranscriptionScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.TextSecondary

enum class MainNavDestination(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    INTERPRETER("live_interpreter", R.string.nav_interpreter, Icons.Filled.Mic, Icons.Outlined.Mic),
    TRANSCRIPTION("transcription", R.string.nav_transcription, Icons.Filled.Description, Icons.Outlined.Description),
    SUMMARY("ai_summary", R.string.nav_summary, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    LIBRARY("recording_library", R.string.nav_library, Icons.Filled.Folder, Icons.Outlined.Folder)
}

private const val ROUTE_MODEL_MANAGER = "model_manager"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinguaBridgeApp(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MainNavDestination.HOME.route

    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val hardwareStatus by viewModel.hardwareStatus.collectAsStateWithLifecycle()
    val managedModels by viewModel.managedModels.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val selectedRecording by viewModel.selectedRecording.collectAsStateWithLifecycle()
    val librarySearchQuery by viewModel.librarySearchQuery.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val readiness by viewModel.readinessState.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val onlineTranslationState by viewModel.onlineTranslationState.collectAsStateWithLifecycle()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsStateWithLifecycle()
    val showConsentDialog by viewModel.showConsentDialog.collectAsStateWithLifecycle()
    val pendingDownloadModel by viewModel.pendingDownloadModel.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbarMessage()
    }

    if (showConsentDialog) {
        RecordingConsentDialog(
            onAccept = {
                viewModel.acceptRecordingConsent(hasMicPermission = true) {}
            },
            onDismiss = viewModel::dismissConsentDialog
        )
    }

    pendingDownloadModel?.let { modelItem ->
        ModelDownloadConfirmationDialog(
            modelItem = modelItem,
            hardwareStatus = hardwareStatus,
            onConfirm = viewModel::confirmModelDownload,
            onDismiss = viewModel::dismissDownloadConfirmation
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLayout = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = NavyVoid,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavySurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_linguabridge_foreground),
                                contentDescription = stringResource(id = R.string.cd_app_logo),
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        OfflineReadinessPill(
                            readiness = readiness.summary,
                            onClick = {
                                if (currentRoute != ROUTE_MODEL_MANAGER) {
                                    navController.navigate(ROUTE_MODEL_MANAGER) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )
            },
            bottomBar = {
                if (!isTabletLayout && currentRoute != ROUTE_MODEL_MANAGER) {
                    NavigationBar(
                        containerColor = NavySurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        MainNavDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(id = dest.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(text = label, maxLines = 1)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("bottom_nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTabletLayout && currentRoute != ROUTE_MODEL_MANAGER) {
                    NavigationRail(
                        containerColor = NavySurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.size(8.dp))
                        MainNavDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(id = dest.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("rail_nav_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = MainNavDestination.HOME.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 960.dp)
                    ) {
                        composable(MainNavDestination.HOME.route) {
                            HomeScreen(
                                readiness = readiness,
                                hardwareStatus = hardwareStatus,
                                recentRecordings = recordings,
                                stages = viewModel.developmentStages,
                                isTabletLayout = isTabletLayout,
                                isOnlineAiEnabled = userPreferences.isOnlineAiEnabled,
                                onToggleOnlineAi = viewModel::setOnlineAiEnabled,
                                onLaunchDirection = { direction ->
                                    viewModel.selectLanguageDirection(direction)
                                    navigateToTab(MainNavDestination.INTERPRETER.route)
                                },
                                onOpenLiveInterpreter = {
                                    navigateToTab(MainNavDestination.INTERPRETER.route)
                                },
                                onOpenTranscription = {
                                    navigateToTab(MainNavDestination.TRANSCRIPTION.route)
                                },
                                onOpenAiSummary = {
                                    navigateToTab(MainNavDestination.SUMMARY.route)
                                },
                                onOpenModelManager = {
                                    navController.navigate(ROUTE_MODEL_MANAGER) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenRecording = { id ->
                                    viewModel.selectRecording(id)
                                    navigateToTab(MainNavDestination.TRANSCRIPTION.route)
                                }
                            )
                        }

                        composable(MainNavDestination.INTERPRETER.route) {
                            LiveInterpreterScreen(
                                liveState = liveState,
                                preferences = userPreferences,
                                readiness = readiness,
                                onSelectSourceLanguage = viewModel::setSourceLanguage,
                                onSelectTargetLanguage = viewModel::setTargetLanguage,
                                onSwapLanguages = viewModel::swapLanguages,
                                onToggleOnlineAi = viewModel::setOnlineAiEnabled,
                                onSendLiveSpeech = viewModel::sendLiveSpeechForInterpretation,
                                onRequestStartRecording = viewModel::requestRecordingStart,
                                onPauseRecording = viewModel::pauseRecording,
                                onResumeRecording = viewModel::resumeRecording,
                                onStopAndSaveRecording = viewModel::stopAndSaveRecording,
                                onOpenModelManager = {
                                    navController.navigate(ROUTE_MODEL_MANAGER) {
                                        launchSingleTop = true
                                    }
                                },
                                onPermissionDeniedMessage = viewModel::postMessage
                            )
                        }

                        composable(MainNavDestination.TRANSCRIPTION.route) {
                            TranscriptionScreen(
                                recordings = recordings,
                                selectedRecording = selectedRecording,
                                playbackState = playbackState,
                                onlineTranslationState = onlineTranslationState,
                                onSelectRecording = viewModel::selectRecording,
                                onTogglePlayback = viewModel::toggleAudioPlayback,
                                onExecuteOnlineTranslation = viewModel::executeOnlineTranslation,
                                onUpdateOnlineInputText = viewModel::updateOnlineInputText,
                                onAppendSegment = viewModel::appendSegmentToRecording,
                                onGenerateYuanbaoSummary = viewModel::generateYuanbaoDeepSummary,
                                onSaveEdits = viewModel::saveTranscriptEdits,
                                onExportTxtStream = viewModel::exportTxtToStream,
                                onExportPdfStream = viewModel::exportPdfToStream,
                                onShareTxt = { rec ->
                                    val intent: Intent = viewModel.buildShareTxtIntent(rec)
                                    context.startActivity(intent)
                                },
                                onSharePdf = { rec ->
                                    val intent: Intent = viewModel.buildSharePdfIntent(rec)
                                    context.startActivity(intent)
                                },
                                onOpenAiSummary = {
                                    navigateToTab(MainNavDestination.SUMMARY.route)
                                },
                                onBuildPlainTextForCopy = viewModel::buildPlainTextForCopy,
                                onShowMessage = viewModel::postMessage
                            )
                        }

                        composable(MainNavDestination.SUMMARY.route) {
                            AiSummaryScreen(
                                recordings = recordings,
                                selectedRecording = selectedRecording,
                                isOnlineAiEnabled = userPreferences.isOnlineAiEnabled,
                                isGeneratingSummary = isGeneratingSummary,
                                onSelectRecording = viewModel::selectRecording,
                                onGenerateYuanbaoSummary = viewModel::generateYuanbaoDeepSummary,
                                onGenerateOfflineSummary = viewModel::generateOfflineSummary
                            )
                        }

                        composable(MainNavDestination.LIBRARY.route) {
                            RecordingLibraryScreen(
                                recordings = recordings,
                                searchQuery = librarySearchQuery,
                                playbackState = playbackState,
                                onSearchQueryChange = viewModel::updateLibrarySearchQuery,
                                onTogglePlayback = viewModel::toggleAudioPlayback,
                                onOpenTranscription = { id ->
                                    viewModel.selectRecording(id)
                                    navigateToTab(MainNavDestination.TRANSCRIPTION.route)
                                },
                                onOpenSummary = { id ->
                                    viewModel.selectRecording(id)
                                    navigateToTab(MainNavDestination.SUMMARY.route)
                                },
                                onDeleteRecording = viewModel::deleteRecording,
                                onStartNewRecording = {
                                    navigateToTab(MainNavDestination.INTERPRETER.route)
                                }
                            )
                        }

                        composable(ROUTE_MODEL_MANAGER) {
                            ModelManagerScreen(
                                models = managedModels,
                                hardwareStatus = hardwareStatus,
                                readiness = readiness,
                                onBack = { navController.popBackStack() },
                                onRefresh = viewModel::refreshModelManager,
                                onRequestDownload = viewModel::requestModelDownload,
                                onCancelDownload = viewModel::cancelModelDownload,
                                onDeleteModel = viewModel::deleteModel
                            )
                        }
                    }
                }
            }
        }
    }
}
