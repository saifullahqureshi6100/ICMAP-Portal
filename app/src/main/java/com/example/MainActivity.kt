package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.configureAppCheck
import com.example.data.security.TutorStyleRequest
import com.example.ui.jarvis.AnalyticsScreen
import com.example.ui.jarvis.CallJarvisScreen
import com.example.ui.jarvis.ExamLabTab
import com.example.ui.jarvis.GlobalSearchScreen
import com.example.ui.jarvis.JarvisDashboardScreen
import com.example.ui.jarvis.JarvisNavDest
import com.example.ui.jarvis.JarvisSidebarPanel
import com.example.ui.jarvis.JarvisSpaceBackdrop
import com.example.ui.jarvis.JarvisTeacherScreen
import com.example.ui.jarvis.JarvisTopHudHeader
import com.example.ui.jarvis.OmJarvisOnboardingScreen
import com.example.ui.jarvis.SettingsBackupScreen
import com.example.ui.jarvis.SmartFlashcardsScreen
import com.example.ui.jarvis.StudyPlannerScreen
import com.example.ui.jarvis.SyllabusWorkspaceScreen
import com.example.ui.jarvis.TestingAndMockScreen
import com.example.ui.jarvis.VideoLibraryScreen
import com.example.ui.jarvis.WorkspaceMode
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisSidebarSurface
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.StudyPortalTheme
import com.example.viewmodel.OmJarvisViewModel
import com.example.work.ReminderDeepLink
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var deepLinkRouteState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        deepLinkRouteState.value = intent?.getStringExtra(ReminderDeepLink.EXTRA_TARGET_ROUTE)

        enableEdgeToEdge()
        setContent {
            val vm: OmJarvisViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()

            StudyPortalTheme(darkTheme = settings.darkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OmJarvisAppRoot(
                        vm = vm,
                        pendingDeepLinkRoute = deepLinkRouteState.value,
                        onConsumeDeepLink = { deepLinkRouteState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRouteState.value = intent.getStringExtra(ReminderDeepLink.EXTRA_TARGET_ROUTE)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmJarvisAppRoot(
    vm: OmJarvisViewModel,
    pendingDeepLinkRoute: String?,
    onConsumeDeepLink: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val bundle by vm.bundle.collectAsStateWithLifecycle()
    val isOnline by vm.isOnline.collectAsStateWithLifecycle()
    val analytics by vm.analytics.collectAsStateWithLifecycle()
    val topicProgressMap by vm.topicProgressMap.collectAsStateWithLifecycle()
    val videos by vm.videos.collectAsStateWithLifecycle()
    val catalogAudit by vm.catalogAudit.collectAsStateWithLifecycle()
    val isAuditingCatalog by vm.isAuditingCatalog.collectAsStateWithLifecycle()
    val flashcards by vm.flashcards.collectAsStateWithLifecycle()
    val testAttempts by vm.testAttempts.collectAsStateWithLifecycle()
    val scheduleSlots by vm.scheduleSlots.collectAsStateWithLifecycle()
    val activeMockState by vm.activeMockState.collectAsStateWithLifecycle()
    val selectedTopicId by vm.selectedTopicId.collectAsStateWithLifecycle()
    val selectedPartFilter by vm.selectedPartFilter.collectAsStateWithLifecycle()
    val mcqRunnerState by vm.mcqRunnerState.collectAsStateWithLifecycle()
    val lastDescriptiveEval by vm.lastDescriptiveEval.collectAsStateWithLifecycle()
    val tutorMessages by vm.tutorMessages.collectAsStateWithLifecycle()
    val tutorLoading by vm.tutorLoading.collectAsStateWithLifecycle()
    val feedbackBanner by vm.feedbackBanner.collectAsStateWithLifecycle()

    if (!settings.onboardingCompleted) {
        OmJarvisOnboardingScreen(
            settings = settings,
            daysRemaining = analytics.daysRemaining,
            catalogAudit = catalogAudit,
            isOnline = isOnline,
            onRefreshVideoCatalog = { vm.refreshVideoCatalog() },
            onCompleteOnboarding = { merit, dark, mins, notifs ->
                vm.completeOnboarding(merit, dark, mins, notifs)
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: JarvisNavDest.DASHBOARD.route

    var syllabusInitialMode by remember { mutableStateOf(WorkspaceMode.READ) }
    var syllabusOpenWorkspace by remember { mutableStateOf(false) }
    var showReadinessDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackBanner) {
        feedbackBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearFeedback()
        }
    }

    val navigateToRoute: (String) -> Unit = { route ->
        if (route == JarvisNavDest.SYLLABUS.route) {
            syllabusOpenWorkspace = false
        } else if (route == JarvisNavDest.TOPICS.route) {
            syllabusOpenWorkspace = true
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Handle WorkManager notification deep links cleanly
    LaunchedEffect(pendingDeepLinkRoute) {
        when (pendingDeepLinkRoute) {
            ReminderDeepLink.ROUTE_CALL_JARVIS -> {
                navController.navigate(JarvisNavDest.CALL_JARVIS.route) { launchSingleTop = true }
                onConsumeDeepLink()
            }
            ReminderDeepLink.ROUTE_TEST -> {
                navigateToRoute(JarvisNavDest.MCQ_TESTS.route)
                onConsumeDeepLink()
            }
            ReminderDeepLink.ROUTE_MOCK -> {
                navigateToRoute(JarvisNavDest.GRAND_TESTS.route)
                onConsumeDeepLink()
            }
            ReminderDeepLink.ROUTE_FLASHCARDS -> {
                navigateToRoute(JarvisNavDest.FLASHCARDS.route)
                onConsumeDeepLink()
            }
            ReminderDeepLink.ROUTE_SYLLABUS -> {
                navigateToRoute(JarvisNavDest.SYLLABUS.route)
                onConsumeDeepLink()
            }
            ReminderDeepLink.ROUTE_BRIEF -> {
                navigateToRoute(JarvisNavDest.DASHBOARD.route)
                onConsumeDeepLink()
            }
        }
    }

    val compactBottomTabs = remember { JarvisNavDest.entries.filter { it.isCompactBottomTab } }
    val weakIds = remember(analytics) { analytics.weakest5.map { it.topic.id }.toSet() }
    val activeTopic = remember(bundle, selectedTopicId) {
        bundle.topics.firstOrNull { it.id == selectedTopicId } ?: bundle.topics.firstOrNull()
    }
    val activeVideo = remember(videos, activeTopic) {
        videos.firstOrNull { it.topicId == activeTopic?.id } ?: videos.firstOrNull()
    }
    val activeProgress = remember(topicProgressMap, activeTopic) {
        activeTopic?.let { topicProgressMap[it.id] }
    }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val dueFlashcardsCount = remember(flashcards) {
        val now = System.currentTimeMillis()
        flashcards.count { it.nextReviewAt <= now }
    }
    val pendingAlertsCount = remember(scheduleSlots, analytics, dueFlashcardsCount, todayKey) {
        val pendingSlots = scheduleSlots.count { it.completedDateKey != todayKey }
        val dueCards = if (dueFlashcardsCount > 0) 1 else 0
        (pendingSlots.coerceAtMost(2) + dueCards).coerceAtLeast(if (analytics.weakest5.isNotEmpty()) 1 else 0)
    }

    val isCallScreen = currentRoute == JarvisNavDest.CALL_JARVIS.route

    val contentBody: @Composable (Boolean) -> Unit = { isCompactPhone ->
        NavHost(
            navController = navController,
            startDestination = JarvisNavDest.DASHBOARD.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(JarvisNavDest.DASHBOARD.route) {
                JarvisDashboardScreen(
                    settings = settings,
                    analytics = analytics,
                    activeTopic = activeTopic,
                    activeVideo = activeVideo,
                    activeProgress = activeProgress,
                    showReadinessDialog = showReadinessDialog,
                    onDismissReadinessDialog = { showReadinessDialog = false },
                    onContinueStudy = {
                        vm.selectTopic(settings.lastStudiedTopicId)
                        syllabusInitialMode = WorkspaceMode.READ
                        syllabusOpenWorkspace = true
                        navigateToRoute(JarvisNavDest.TOPICS.route)
                    },
                    onOpenTopic = { topicId ->
                        vm.selectTopic(topicId)
                        syllabusInitialMode = WorkspaceMode.READ
                        syllabusOpenWorkspace = true
                        navigateToRoute(JarvisNavDest.TOPICS.route)
                    },
                    onFilterPart = { partCode ->
                        vm.selectPartFilter(partCode)
                        syllabusOpenWorkspace = false
                        navigateToRoute(JarvisNavDest.SYLLABUS.route)
                    },
                    onOpenVideoWorkspace = {
                        activeTopic?.let { vm.selectTopic(it.id) }
                        navigateToRoute(JarvisNavDest.VIDEO_LIBRARY.route)
                    },
                    onOpenReadingWorkspace = {
                        activeTopic?.let { vm.selectTopic(it.id) }
                        syllabusInitialMode = WorkspaceMode.READ
                        syllabusOpenWorkspace = true
                        navigateToRoute(JarvisNavDest.TOPICS.route)
                    },
                    onSaveQuickNote = { topic, note ->
                        vm.saveTopicNotesAndFlags(topic, notes = note)
                    },
                    onStartQuickTest = {
                        vm.startAdaptiveMcqTest("Today's 10-MCQ Quick Drill", 10, timedExamMode = true)
                        navigateToRoute(JarvisNavDest.MCQ_TESTS.route)
                    },
                    onStartWeakTest = {
                        vm.startAdaptiveMcqTest("Weak-Topic Recovery Drill", 15, weakOnly = true, timedExamMode = true)
                        navigateToRoute(JarvisNavDest.MCQ_TESTS.route)
                    },
                    onOpenFlashcards = { navigateToRoute(JarvisNavDest.FLASHCARDS.route) },
                    onOpenVideos = { navigateToRoute(JarvisNavDest.VIDEO_LIBRARY.route) },
                    onAskJarvis = { navigateToRoute(JarvisNavDest.JARVIS.route) },
                    onCallJarvis = { navController.navigate(JarvisNavDest.CALL_JARVIS.route) { launchSingleTop = true } },
                    onOpenMockExam = { navigateToRoute(JarvisNavDest.GRAND_TESTS.route) },
                    onOpenAnalytics = { navigateToRoute(JarvisNavDest.ANALYTICS.route) }
                )
            }

            composable(JarvisNavDest.SYLLABUS.route) {
                SyllabusWorkspaceScreen(
                    bundle = bundle,
                    settings = settings,
                    isOnline = isOnline,
                    selectedPartFilter = selectedPartFilter,
                    selectedTopicId = selectedTopicId,
                    topicProgressMap = topicProgressMap,
                    videos = videos,
                    catalogAudit = catalogAudit,
                    isAuditingCatalog = isAuditingCatalog,
                    initialMode = syllabusInitialMode,
                    initialWorkspaceOpen = false,
                    onSelectPartFilter = { vm.selectPartFilter(it) },
                    onSelectTopic = { vm.selectTopic(it) },
                    onMoveRelativeTopic = { delta -> vm.moveRelativeTopic(delta) },
                    onRecordReading = { t, pct, p, done -> vm.recordReadingProgress(t, pct, p, done) },
                    onSaveNotesAndFlags = { t, n, h, bm, imp, rev ->
                        vm.saveTopicNotesAndFlags(t, n, h, bm, imp, rev)
                    },
                    onVideoTick = { vidId, pos, dur, done -> vm.onVideoPlaybackTick(vidId, pos, dur, done) },
                    onStartDownload = { vm.startOrResumeDownload(it) },
                    onPauseDownload = { vm.pauseDownload(it) },
                    onCancelDownload = { vm.cancelDownload(it) },
                    onDeleteCachedVideo = { vm.deleteCachedVideo(it) },
                    onRefreshVideoCatalog = { vm.refreshVideoCatalog() },
                    onStartTopicMcq = { t ->
                        vm.startAdaptiveMcqTest("Topic Drill: ${t.name}", 10, topicFilterId = t.id, timedExamMode = true)
                        navigateToRoute(JarvisNavDest.MCQ_TESTS.route)
                    },
                    onAskJarvisForTopic = { t ->
                        vm.selectTopic(t.id)
                        vm.askJarvis("Explain ${t.name} clearly with Roman Urdu & Pakistani example.", TutorStyleRequest.DEFAULT_BILINGUAL, t)
                        navigateToRoute(JarvisNavDest.JARVIS.route)
                    },
                    onUpdateSettings = { vm.updateSettings(it) }
                )
            }

            composable(JarvisNavDest.TOPICS.route) {
                SyllabusWorkspaceScreen(
                    bundle = bundle,
                    settings = settings,
                    isOnline = isOnline,
                    selectedPartFilter = selectedPartFilter,
                    selectedTopicId = selectedTopicId,
                    topicProgressMap = topicProgressMap,
                    videos = videos,
                    catalogAudit = catalogAudit,
                    isAuditingCatalog = isAuditingCatalog,
                    initialMode = syllabusInitialMode,
                    initialWorkspaceOpen = true,
                    onSelectPartFilter = { vm.selectPartFilter(it) },
                    onSelectTopic = { vm.selectTopic(it) },
                    onMoveRelativeTopic = { delta -> vm.moveRelativeTopic(delta) },
                    onRecordReading = { t, pct, p, done -> vm.recordReadingProgress(t, pct, p, done) },
                    onSaveNotesAndFlags = { t, n, h, bm, imp, rev ->
                        vm.saveTopicNotesAndFlags(t, n, h, bm, imp, rev)
                    },
                    onVideoTick = { vidId, pos, dur, done -> vm.onVideoPlaybackTick(vidId, pos, dur, done) },
                    onStartDownload = { vm.startOrResumeDownload(it) },
                    onPauseDownload = { vm.pauseDownload(it) },
                    onCancelDownload = { vm.cancelDownload(it) },
                    onDeleteCachedVideo = { vm.deleteCachedVideo(it) },
                    onRefreshVideoCatalog = { vm.refreshVideoCatalog() },
                    onStartTopicMcq = { t ->
                        vm.startAdaptiveMcqTest("Topic Drill: ${t.name}", 10, topicFilterId = t.id, timedExamMode = true)
                        navigateToRoute(JarvisNavDest.MCQ_TESTS.route)
                    },
                    onAskJarvisForTopic = { t ->
                        vm.selectTopic(t.id)
                        vm.askJarvis("Explain ${t.name} clearly with Roman Urdu & Pakistani example.", TutorStyleRequest.DEFAULT_BILINGUAL, t)
                        navigateToRoute(JarvisNavDest.JARVIS.route)
                    },
                    onUpdateSettings = { vm.updateSettings(it) }
                )
            }

            composable(JarvisNavDest.STUDY_PLANNER.route) {
                StudyPlannerScreen(
                    settings = settings,
                    scheduleSlots = scheduleSlots,
                    todayMinutesLogged = analytics.todayMinutes,
                    onToggleSlotDone = { vm.toggleScheduleSlotDone(it) },
                    onSaveSlot = { vm.saveScheduleSlot(it) },
                    onLogMinutes = { mins -> vm.logQuickMinutes(mins) },
                    onOpenTopic = { topicId ->
                        vm.selectTopic(topicId)
                        syllabusInitialMode = WorkspaceMode.READ
                        navigateToRoute(JarvisNavDest.TOPICS.route)
                    },
                    onCallJarvis = { navController.navigate(JarvisNavDest.CALL_JARVIS.route) { launchSingleTop = true } }
                )
            }

            composable(JarvisNavDest.VIDEO_LIBRARY.route) {
                VideoLibraryScreen(
                    topics = bundle.topics,
                    videos = videos,
                    selectedTopicId = selectedTopicId,
                    catalogAudit = catalogAudit,
                    isAuditingCatalog = isAuditingCatalog,
                    isOnline = isOnline,
                    videoCacheSizeMb = vm.getVideoCacheSizeMb(),
                    onSelectVideoTopic = { vm.selectTopic(it) },
                    onMoveRelativeTopic = { delta -> vm.moveRelativeTopic(delta) },
                    onVideoTick = { vidId, pos, dur, done -> vm.onVideoPlaybackTick(vidId, pos, dur, done) },
                    onStartDownload = { vm.startOrResumeDownload(it) },
                    onPauseDownload = { vm.pauseDownload(it) },
                    onCancelDownload = { vm.cancelDownload(it) },
                    onDeleteCachedVideo = { vm.deleteCachedVideo(it) },
                    onRefreshVideoCatalog = { vm.refreshVideoCatalog() },
                    onSaveQuickNote = { topic, note -> vm.saveTopicNotesAndFlags(topic, notes = note) },
                    onAskJarvisForTopic = { t ->
                        vm.selectTopic(t.id)
                        vm.askJarvis("Explain ${t.name} video lecture key concepts in Roman Urdu.", TutorStyleRequest.DEFAULT_BILINGUAL, t)
                        navigateToRoute(JarvisNavDest.JARVIS.route)
                    }
                )
            }

            composable(JarvisNavDest.FLASHCARDS.route) {
                SmartFlashcardsScreen(
                    flashcards = flashcards,
                    topics = bundle.topics,
                    weakTopicIds = weakIds,
                    settings = settings,
                    onReviewCard = { card, rating -> vm.reviewFlashcard(card, rating) },
                    onAddCustomCard = { topic, f, b, h -> vm.addCustomFlashcard(topic, f, b, h) }
                )
            }

            composable(JarvisNavDest.MCQ_TESTS.route) {
                TestingAndMockScreen(
                    bundle = bundle,
                    runnerState = mcqRunnerState,
                    activeMockState = activeMockState,
                    testAttempts = testAttempts,
                    lastDescriptiveEval = lastDescriptiveEval,
                    initialTab = ExamLabTab.MCQS,
                    onStartAdaptiveMcq = { title, cnt, tid, part, weak, timed ->
                        vm.startAdaptiveMcqTest(title, cnt, tid, part, weak, timed)
                    },
                    onStartFullMock = { vm.startFullBookMockExam() },
                    onResumeSavedMock = { vm.resumeSavedMockExam() },
                    onSelectMcqOption = { qIdx, oIdx -> vm.selectMcqOption(qIdx, oIdx) },
                    onJumpToQuestion = { vm.jumpToMcqIndex(it) },
                    onToggleFlag = { vm.toggleFlagQuestion(it) },
                    onToggleUnansweredFilter = { vm.toggleShowUnansweredOnly() },
                    onSubmitMcqRunner = { vm.submitCurrentMcqRunner() },
                    onExitMcqRunner = { vm.exitMcqRunner() },
                    onEvaluateDescriptive = { tid, q, ans -> vm.evaluateDescriptiveAnswer(tid, q, ans) },
                    onAskJarvisForQuestion = { topic, prompt ->
                        if (topic != null) vm.selectTopic(topic.id)
                        vm.askJarvis(prompt, TutorStyleRequest.EXAM_ANSWER, topic)
                        navigateToRoute(JarvisNavDest.JARVIS.route)
                    }
                )
            }

            composable(JarvisNavDest.TYPING_PRACTICE.route) {
                TestingAndMockScreen(
                    bundle = bundle,
                    runnerState = mcqRunnerState,
                    activeMockState = activeMockState,
                    testAttempts = testAttempts,
                    lastDescriptiveEval = lastDescriptiveEval,
                    initialTab = ExamLabTab.TYPING,
                    onStartAdaptiveMcq = { title, cnt, tid, part, weak, timed ->
                        vm.startAdaptiveMcqTest(title, cnt, tid, part, weak, timed)
                    },
                    onStartFullMock = { vm.startFullBookMockExam() },
                    onResumeSavedMock = { vm.resumeSavedMockExam() },
                    onSelectMcqOption = { qIdx, oIdx -> vm.selectMcqOption(qIdx, oIdx) },
                    onJumpToQuestion = { vm.jumpToMcqIndex(it) },
                    onToggleFlag = { vm.toggleFlagQuestion(it) },
                    onToggleUnansweredFilter = { vm.toggleShowUnansweredOnly() },
                    onSubmitMcqRunner = { vm.submitCurrentMcqRunner() },
                    onExitMcqRunner = { vm.exitMcqRunner() },
                    onEvaluateDescriptive = { tid, q, ans -> vm.evaluateDescriptiveAnswer(tid, q, ans) },
                    onAskJarvisForQuestion = { topic, prompt ->
                        if (topic != null) vm.selectTopic(topic.id)
                        vm.askJarvis(prompt, TutorStyleRequest.EXAM_ANSWER, topic)
                        navigateToRoute(JarvisNavDest.JARVIS.route)
                    }
                )
            }

            composable(JarvisNavDest.GRAND_TESTS.route) {
                TestingAndMockScreen(
                    bundle = bundle,
                    runnerState = mcqRunnerState,
                    activeMockState = activeMockState,
                    testAttempts = testAttempts,
                    lastDescriptiveEval = lastDescriptiveEval,
                    initialTab = ExamLabTab.MOCK_EXAM,
                    onStartAdaptiveMcq = { title, cnt, tid, part, weak, timed ->
                        vm.startAdaptiveMcqTest(title, cnt, tid, part, weak, timed)
                    },
                    onStartFullMock = { vm.startFullBookMockExam() },
                    onResumeSavedMock = { vm.resumeSavedMockExam() },
                    onSelectMcqOption = { qIdx, oIdx -> vm.selectMcqOption(qIdx, oIdx) },
                    onJumpToQuestion = { vm.jumpToMcqIndex(it) },
                    onToggleFlag = { vm.toggleFlagQuestion(it) },
                    onToggleUnansweredFilter = { vm.toggleShowUnansweredOnly() },
                    onSubmitMcqRunner = { vm.submitCurrentMcqRunner() },
                    onExitMcqRunner = { vm.exitMcqRunner() },
                    onEvaluateDescriptive = { tid, q, ans -> vm.evaluateDescriptiveAnswer(tid, q, ans) },
                    onAskJarvisForQuestion = { topic, prompt ->
                        if (topic != null) vm.selectTopic(topic.id)
                        vm.askJarvis(prompt, TutorStyleRequest.EXAM_ANSWER, topic)
                        navigateToRoute(JarvisNavDest.JARVIS.route)
                    }
                )
            }

            composable(JarvisNavDest.PAST_PAPERS.route) {
                TestingAndMockScreen(
                    bundle = bundle,
                    runnerState = mcqRunnerState,
                    activeMockState = activeMockState,
                    testAttempts = testAttempts,
                    lastDescriptiveEval = lastDescriptiveEval,
                    initialTab = ExamLabTab.PAST_PAPERS,
                    onStartAdaptiveMcq = { title, cnt, tid, part, weak, timed ->
                        vm.startAdaptiveMcqTest(title, cnt, tid, part, weak, timed)
                    },
                    onStartFullMock = { vm.startFullBookMockExam() },
                    onResumeSavedMock = { vm.resumeSavedMockExam() },
                    onSelectMcqOption = { qIdx, oIdx -> vm.selectMcqOption(qIdx, oIdx) },
                    onJumpToQuestion = { vm.jumpToMcqIndex(it) },
                    onToggleFlag = { vm.toggleFlagQuestion(it) },
                    onToggleUnansweredFilter = { vm.toggleShowUnansweredOnly() },
                    onSubmitMcqRunner = { vm.submitCurrentMcqRunner() },
                    onExitMcqRunner = { vm.exitMcqRunner() },
                    onEvaluateDescriptive = { tid, q, ans -> vm.evaluateDescriptiveAnswer(tid, q, ans) },
                    onAskJarvisForQuestion = { topic, prompt ->
                        if (topic != null) vm.selectTopic(topic.id)
                        vm.askJarvis(prompt, TutorStyleRequest.EXAM_ANSWER, topic)
                        navigateToRoute(JarvisNavDest.JARVIS.route)
                    }
                )
            }

            composable(JarvisNavDest.JARVIS.route) {
                JarvisTeacherScreen(
                    settings = settings,
                    isOnline = isOnline,
                    activeTopic = activeTopic,
                    messages = tutorMessages,
                    isLoading = tutorLoading,
                    onAskJarvis = { q, style -> vm.askJarvis(q, style) },
                    onClearChat = { vm.clearTutorChat() },
                    onLaunchCallJarvis = { navController.navigate(JarvisNavDest.CALL_JARVIS.route) { launchSingleTop = true } },
                    onShowFeedback = { vm.showFeedback(it) }
                )
            }

            composable(JarvisNavDest.CALL_JARVIS.route) {
                CallJarvisScreen(
                    settings = settings,
                    analytics = analytics,
                    topics = bundle.topics,
                    onExitCall = { navController.popBackStack() },
                    onLogSessionMinutes = { mins -> vm.logQuickMinutes(mins, "CALL_JARVIS") },
                    onShowFeedback = { vm.showFeedback(it) }
                )
            }

            composable(JarvisNavDest.ANALYTICS.route) {
                AnalyticsScreen(
                    analytics = analytics,
                    onBack = { navController.popBackStack() },
                    onOpenTopic = { topicId ->
                        vm.selectTopic(topicId)
                        navigateToRoute(JarvisNavDest.TOPICS.route)
                    }
                )
            }

            composable(JarvisNavDest.SEARCH.route) {
                GlobalSearchScreen(
                    onSearch = { q -> vm.searchAll(q) },
                    onOpenTopic = { topicId ->
                        vm.selectTopic(topicId)
                        navigateToRoute(JarvisNavDest.TOPICS.route)
                    },
                    onOpenFlashcards = { navigateToRoute(JarvisNavDest.FLASHCARDS.route) },
                    onOpenPastPapers = { navigateToRoute(JarvisNavDest.PAST_PAPERS.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(JarvisNavDest.SETTINGS.route) {
                SettingsBackupScreen(
                    settings = settings,
                    catalogAudit = catalogAudit,
                    videoCacheMb = vm.getVideoCacheSizeMb(),
                    getMaskedKey = { prov -> vm.getMaskedApiKey(prov) },
                    onUpdateSettings = { vm.updateSettings(it) },
                    onSaveApiKey = { p, k, m, e -> vm.saveEncryptedApiKey(p, k, m, e) },
                    onClearApiKey = { p -> vm.clearEncryptedApiKey(p) },
                    onRefreshVideoCatalog = { vm.refreshVideoCatalog() },
                    onClearVideoCache = { vm.clearAllCachedVideos() },
                    onTriggerTestReminder = { type -> vm.triggerTestReminder(type) },
                    onExportJson = { vm.exportBackupJson() },
                    onImportJson = { raw -> vm.importBackupJson(raw) },
                    onResetProgress = { vm.resetAllProgressConfirmed() },
                    onShowFeedback = { vm.showFeedback(it) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    JarvisSpaceBackdrop(darkMode = settings.darkThemeEnabled) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideHud = maxWidth >= 840.dp

            if (isWideHud) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (!isCallScreen) {
                            JarvisSidebarPanel(
                                currentRoute = currentRoute,
                                meritModeEnabled = settings.meritModeEnabled,
                                onSelectRoute = { route -> navigateToRoute(route) },
                                onToggleMeritMode = { enabled ->
                                    vm.updateSettings { it.copy(meritModeEnabled = enabled) }
                                },
                                modifier = Modifier.width(216.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (!isCallScreen) {
                                JarvisTopHudHeader(
                                    isCompactPhone = false,
                                    settings = settings,
                                    analytics = analytics,
                                    unreadAlertsCount = pendingAlertsCount,
                                    onOpenDrawer = {},
                                    onClickReadiness = { showReadinessDialog = true },
                                    onClickNotifications = { showNotificationsDialog = true },
                                    onOpenSearch = { navController.navigate(JarvisNavDest.SEARCH.route) { launchSingleTop = true } },
                                    onOpenSettings = { navigateToRoute(JarvisNavDest.SETTINGS.route) },
                                    onToggleTheme = { vm.toggleTheme() }
                                )
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                contentBody(false)
                            }
                        }
                    }
                }
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !isCallScreen,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = JarvisSidebarSurface,
                            modifier = Modifier.width(268.dp)
                        ) {
                            JarvisSidebarPanel(
                                currentRoute = currentRoute,
                                meritModeEnabled = settings.meritModeEnabled,
                                onSelectRoute = { route ->
                                    scope.launch { drawerState.close() }
                                    navigateToRoute(route)
                                },
                                onToggleMeritMode = { enabled ->
                                    vm.updateSettings { it.copy(meritModeEnabled = enabled) }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            if (!isCallScreen) {
                                JarvisTopHudHeader(
                                    isCompactPhone = true,
                                    settings = settings,
                                    analytics = analytics,
                                    unreadAlertsCount = pendingAlertsCount,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onClickReadiness = { showReadinessDialog = true },
                                    onClickNotifications = { showNotificationsDialog = true },
                                    onOpenSearch = { navController.navigate(JarvisNavDest.SEARCH.route) { launchSingleTop = true } },
                                    onOpenSettings = { navigateToRoute(JarvisNavDest.SETTINGS.route) },
                                    onToggleTheme = { vm.toggleTheme() }
                                )
                            }
                        },
                        bottomBar = {
                            if (!isCallScreen) {
                                NavigationBar(
                                    containerColor = JarvisSidebarSurface.copy(alpha = 0.96f),
                                    contentColor = JarvisTextPrimary
                                ) {
                                    compactBottomTabs.forEach { dest ->
                                        val shortLabel = when (dest) {
                                            JarvisNavDest.VIDEO_LIBRARY -> "Videos"
                                            JarvisNavDest.MCQ_TESTS -> "Tests"
                                            JarvisNavDest.JARVIS -> "Jarvis"
                                            else -> dest.label
                                        }
                                        NavigationBarItem(
                                            selected = currentRoute == dest.route,
                                            onClick = { navigateToRoute(dest.route) },
                                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                                            label = {
                                                Text(
                                                    text = shortLabel,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = JarvisCyanGlow,
                                                selectedTextColor = JarvisCyanGlow,
                                                indicatorColor = JarvisElectricBlue.copy(alpha = 0.35f),
                                                unselectedIconColor = JarvisTextSecondary,
                                                unselectedTextColor = JarvisTextSecondary
                                            ),
                                            modifier = Modifier.testTag("nav_${dest.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            contentBody(true)
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsDialog) {
        val pendingSlots = scheduleSlots.filter { it.completedDateKey != todayKey }.take(4)
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("OM Jarvis Study Alerts", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exam Countdown: ${analytics.daysRemaining} days left • Merit Target: ${if (settings.meritModeEnabled) "80%+" else "70%+"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = JarvisGold
                    )
                    if (dueFlashcardsCount > 0) {
                        Text("• $dueFlashcardsCount spaced-repetition flashcards due today")
                    }
                    if (analytics.weakest5.isNotEmpty()) {
                        Text("• Weakest Area Alert: ${analytics.weakest5.first().topic.name} (${analytics.weakest5.first().masteryPercent}%)")
                    }
                    pendingSlots.forEach { slot ->
                        Text("• ${slot.startTime} — ${slot.title}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationsDialog = false
                        navigateToRoute(JarvisNavDest.STUDY_PLANNER.route)
                    }
                ) { Text("Open Planner") }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Dismiss") }
            }
        )
    }
}
