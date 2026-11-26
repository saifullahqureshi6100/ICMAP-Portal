package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.signOutUser
import com.example.data.AiTutorRepository
import com.example.data.FullScreenMode
import com.example.data.PortalTab
import com.example.data.PreloadedMaterials
import com.example.data.StudyRepository
import com.example.data.configureAppCheck
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.FullScreenFlashcardScreen
import com.example.ui.screens.FullScreenMcqExam
import com.example.ui.screens.FullScreenPomodoroScreen
import com.example.ui.screens.FullScreenReadingViewer
import com.example.ui.screens.FullScreenRoadmapScreen
import com.example.ui.screens.FullScreenTutorStudio
import com.example.ui.screens.MaterialsScreen
import com.example.ui.screens.McqScreen
import com.example.ui.screens.TutorScreen
import com.example.ui.theme.StudyPortalTheme
import com.example.viewmodel.StreamUiState
import com.example.viewmodel.StudyPortalViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            StudyPortalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            StudyPortalShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: user.email ?: "Student"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPortalShell(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudyPortalViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudyPortalViewModel(
                    studyRepo = StudyRepository(db),
                    aiRepo = AiTutorRepository(),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val fullScreenMode by viewModel.fullScreenMode.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val materialsState by viewModel.materialsState.collectAsStateWithLifecycle()
    val progressMapState by viewModel.progressMapState.collectAsStateWithLifecycle()
    val studyPlansState by viewModel.studyPlansState.collectAsStateWithLifecycle()
    val quizAttemptsState by viewModel.quizAttemptsState.collectAsStateWithLifecycle()
    val planGenState by viewModel.planGenState.collectAsStateWithLifecycle()
    val mcqGenState by viewModel.mcqGenState.collectAsStateWithLifecycle()
    val quizSession by viewModel.quizSession.collectAsStateWithLifecycle()
    val tutorMessages by viewModel.tutorMessages.collectAsStateWithLifecycle()
    val tutorStreamingText by viewModel.tutorStreamingText.collectAsStateWithLifecycle()
    val tutorState by viewModel.tutorState.collectAsStateWithLifecycle()
    val tutorEngineMode by viewModel.tutorEngineMode.collectAsStateWithLifecycle()
    val freeRefState by viewModel.freeRefState.collectAsStateWithLifecycle()
    val freeRefResult by viewModel.freeRefResult.collectAsStateWithLifecycle()
    val readerAiState by viewModel.readerAiState.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcardsState.collectAsStateWithLifecycle()
    val selectedDeckFilter by viewModel.selectedDeckFilter.collectAsStateWithLifecycle()
    val flashcardGenState by viewModel.flashcardGenState.collectAsStateWithLifecycle()
    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val dailyBriefPlan by viewModel.dailyBriefPlan.collectAsStateWithLifecycle()
    val aiBriefText by viewModel.aiBriefText.collectAsStateWithLifecycle()
    val briefState by viewModel.briefState.collectAsStateWithLifecycle()
    val completedRoutineIds by viewModel.completedRoutineIds.collectAsStateWithLifecycle()
    val weakTopics by viewModel.weakTopics.collectAsStateWithLifecycle()
    val typingFeedbackMap by viewModel.typingFeedbackMap.collectAsStateWithLifecycle()
    val typingGradingId by viewModel.typingGradingId.collectAsStateWithLifecycle()
    val readerQuickReply by viewModel.readerQuickReply.collectAsStateWithLifecycle()
    val readerQuickState by viewModel.readerQuickState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val isQuizVisible = (activeTab == PortalTab.MCQS && fullScreenMode == null) ||
        fullScreenMode is FullScreenMode.McqExam
    LaunchedEffect(isQuizVisible, quizSession.isSubmitted, quizSession.questions.size) {
        while (isQuizVisible && !quizSession.isSubmitted && quizSession.questions.isNotEmpty()) {
            delay(1000L)
            viewModel.tickQuizElapsed()
        }
    }

    val activeFullScreen = fullScreenMode
    if (activeFullScreen != null) {
        when (activeFullScreen) {
            is FullScreenMode.ReadingViewer -> {
                val progressMap = (progressMapState as? StreamUiState.Success)?.data ?: emptyMap()
                FullScreenReadingViewer(
                    material = activeFullScreen.material,
                    openAsVideo = activeFullScreen.openAsVideo,
                    progress = progressMap[activeFullScreen.material.id],
                    readerAiState = readerAiState,
                    onRunReaderAiAction = { action, selectedText, pageLabel, pageBitmap ->
                        viewModel.runReaderAiAction(
                            material = activeFullScreen.material,
                            action = action,
                            selectedText = selectedText,
                            pageLabel = pageLabel,
                            pageBitmap = pageBitmap
                        )
                    },
                    onSwitchReaderAiAction = { action -> viewModel.switchReaderAiAction(action) },
                    onRetryReaderAi = { viewModel.retryReaderAiAction() },
                    onSaveReaderAiToNotes = { viewModel.saveReaderAiToNotes() },
                    onContinueReaderAiInTutor = { viewModel.continueReaderAiInTutor() },
                    onDismissReaderAi = { viewModel.dismissReaderAiSheet() },
                    onToggleComplete = { completed, mins ->
                        viewModel.toggleMaterialCompleted(activeFullScreen.material.id, completed, mins)
                    },
                    onSaveNotes = { notes ->
                        viewModel.saveMaterialNotes(activeFullScreen.material.id, notes)
                    },
                    onAskTutor = { mat ->
                        viewModel.launchTutorForMaterial(mat)
                    },
                    onGenerateFlashcards = { mat ->
                        viewModel.generateFlashcardsFromMaterial(
                            material = mat,
                            cardCount = 6,
                            launchFullScreen = true
                        )
                    },
                    isGeneratingFlashcards = flashcardGenState is com.example.viewmodel.AiTaskState.Loading,
                    readerQuickReply = readerQuickReply,
                    readerQuickState = readerQuickState,
                    onAskTeacherInReader = { mat, q, inUrdu ->
                        viewModel.askTeacherFromReader(mat, q, inUrdu)
                    },
                    onClearReaderQuickReply = { viewModel.clearReaderQuickReply() },
                    onExit = { viewModel.exitFullScreen() }
                )
            }
            is FullScreenMode.McqExam -> {
                FullScreenMcqExam(
                    session = quizSession,
                    onSelectOption = { qIdx, optIdx -> viewModel.selectQuizOption(qIdx, optIdx) },
                    onSelectQuestionIndex = { idx -> viewModel.setQuizQuestionIndex(idx) },
                    onResetQuiz = { viewModel.resetCurrentQuiz() },
                    onSubmitQuiz = { viewModel.submitCurrentQuiz(fullScreenUsed = true) },
                    onExit = { viewModel.exitFullScreen() }
                )
            }
            is FullScreenMode.TutorStudio -> {
                FullScreenTutorStudio(
                    messages = tutorMessages,
                    streamingText = tutorStreamingText,
                    tutorState = tutorState,
                    engineMode = tutorEngineMode,
                    freeRefState = freeRefState,
                    freeRefResult = freeRefResult,
                    onSelectEngine = { mode -> viewModel.selectTutorEngine(mode) },
                    onLookupWiki = { query -> viewModel.lookupWikipediaConcept(query) },
                    onLookupDictionary = { word -> viewModel.lookupDictionaryTerm(word) },
                    onDismissReference = { viewModel.dismissFreeReference() },
                    onAskTutorAboutReference = { ref -> viewModel.askTutorAboutReference(ref) },
                    onSendMessage = { text, bmp -> viewModel.sendTutorMessage(text, bmp) },
                    onExit = { viewModel.exitFullScreen() }
                )
            }
            is FullScreenMode.PomodoroFocus -> {
                FullScreenPomodoroScreen(
                    state = pomodoroState,
                    onToggleRunPause = { viewModel.togglePomodoroRunPause() },
                    onReset = { viewModel.resetPomodoro() },
                    onSkipPhase = { viewModel.skipPomodoroPhase() },
                    onSelectPhase = { phase -> viewModel.selectPomodoroPhase(phase) },
                    onUpdateConfig = { work, shortBrk, longBrk, cycles, auto ->
                        viewModel.updatePomodoroIntervals(work, shortBrk, longBrk, cycles, auto)
                    },
                    onExit = { viewModel.exitFullScreen() }
                )
            }
            is FullScreenMode.RoadmapFocus -> {
                val materials = (materialsState as? StreamUiState.Success)?.data ?: PreloadedMaterials.items
                val progressMap = (progressMapState as? StreamUiState.Success)?.data ?: emptyMap()
                val plansList = (studyPlansState as? StreamUiState.Success)?.data ?: emptyList()
                val attemptsList = (quizAttemptsState as? StreamUiState.Success)?.data ?: emptyList()
                FullScreenRoadmapScreen(
                    materials = materials,
                    progressMap = progressMap,
                    plans = plansList,
                    attempts = attemptsList,
                    onOpenMaterial = { mat, asVideo -> viewModel.openMaterialInFullScreen(mat, asVideo) },
                    onToggleMaterialComplete = { id, done, mins -> viewModel.toggleMaterialCompleted(id, done, mins) },
                    onLaunchMcqBank = { bank -> viewModel.selectMcqBank(bank, launchFullScreen = true) },
                    onTogglePlanItem = { id, done -> viewModel.toggleStudyPlanItem(id, done) },
                    onExit = { viewModel.exitFullScreen() }
                )
            }
            is FullScreenMode.FlashcardDeck -> {
                FullScreenFlashcardScreen(
                    flashcards = flashcards,
                    selectedDeckFilter = selectedDeckFilter,
                    onSelectDeckFilter = { filter -> viewModel.selectFlashcardDeck(filter) },
                    onRecordReview = { card, mastered -> viewModel.recordFlashcardReview(card, mastered) },
                    onAddCustomCard = { deck, matId, front, back, hint ->
                        viewModel.addCustomFlashcard(deck, matId, front, back, hint)
                    },
                    onExit = { viewModel.exitFullScreen() }
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "OM Merit Portal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "5 Dec Exam (${dailyBriefPlan.daysToExam}d) • Hi, ${displayName.substringBefore(' ')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleFullScreenForActiveTab() },
                            modifier = Modifier.testTag("topbar_fullscreen_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Toggle Full-Screen Mode for ${activeTab.label}"
                            )
                        }
                        IconButton(
                            onClick = {
                                signOutUser(credentialManager, scope)
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Sign out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        PortalTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(tabIcon(tab), contentDescription = tab.label)
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
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
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            PortalTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = activeTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Crossfade(targetState = activeTab, label = "tab_crossfade") { tab ->
                        when (tab) {
                            PortalTab.DASHBOARD -> DashboardScreen(
                                summary = summary,
                                pomodoroState = pomodoroState,
                                materialsState = materialsState,
                                progressMapState = progressMapState,
                                plansState = studyPlansState,
                                attemptsState = quizAttemptsState,
                                planGenState = planGenState,
                                dailyBriefPlan = dailyBriefPlan,
                                aiBriefText = aiBriefText,
                                briefState = briefState,
                                completedRoutineIds = completedRoutineIds,
                                weakTopics = weakTopics,
                                onSelectEnergyMode = { mode -> viewModel.selectEnergyMode(mode) },
                                onToggleRoutineSlot = { slotId -> viewModel.toggleRoutineSlot(slotId) },
                                onGenerateAiBrief = { viewModel.generateAiBrief() },
                                onLaunchRoutineAction = { type, focus -> viewModel.launchRoutineAction(type, focus) },
                                onTogglePomodoro = { viewModel.togglePomodoroRunPause() },
                                onResetPomodoro = { viewModel.resetPomodoro() },
                                onSkipPomodoro = { viewModel.skipPomodoroPhase() },
                                onSelectPomodoroPhase = { phase -> viewModel.selectPomodoroPhase(phase) },
                                onUpdatePomodoroConfig = { work, shortBrk, longBrk, cycles, auto ->
                                    viewModel.updatePomodoroIntervals(work, shortBrk, longBrk, cycles, auto)
                                },
                                onLaunchFullScreenPomodoro = {
                                    viewModel.enterFullScreen(FullScreenMode.PomodoroFocus)
                                },
                                onUpdateGoal = { goal, mins -> viewModel.updateStudyGoal(goal, mins) },
                                onLogMinutes = { mins -> viewModel.logQuickStudySession(mins) },
                                onAddPlanItem = { t, topic, day, mins ->
                                    viewModel.addStudyPlanItem(t, topic, day, mins)
                                },
                                onTogglePlanItem = { id, done -> viewModel.toggleStudyPlanItem(id, done) },
                                onDeletePlanItem = { id -> viewModel.deleteStudyPlanItem(id) },
                                onGenerateAiPlan = { goal -> viewModel.generateAiStudyPlan(goal) },
                                onLaunchFullScreenMaterial = { mat, asVideo ->
                                    viewModel.openMaterialInFullScreen(mat, asVideo)
                                },
                                onToggleMaterialComplete = { id, done, mins ->
                                    viewModel.toggleMaterialCompleted(id, done, mins)
                                },
                                onLaunchMcqBank = { bank ->
                                    viewModel.selectMcqBank(bank, launchFullScreen = true)
                                },
                                onLaunchFullScreenRoadmap = {
                                    viewModel.enterFullScreen(FullScreenMode.RoadmapFocus)
                                },
                                onLaunchFullScreenMcq = { viewModel.startCurrentMcqFullScreen() },
                                onLaunchFullScreenFlashcards = { viewModel.openFlashcardsFullScreen() },
                                onLaunchFullScreenTutor = {
                                    viewModel.enterFullScreen(FullScreenMode.TutorStudio())
                                }
                            )
                            PortalTab.MATERIALS -> MaterialsScreen(
                                materialsState = materialsState,
                                progressMapState = progressMapState,
                                onOpenFullScreen = { mat, asVideo ->
                                    viewModel.openMaterialInFullScreen(mat, asVideo)
                                },
                                onAskTutor = { mat -> viewModel.launchTutorForMaterial(mat) },
                                onGenerateFlashcards = { mat ->
                                    viewModel.selectTab(PortalTab.FLASHCARDS)
                                    viewModel.generateFlashcardsFromMaterial(
                                        material = mat,
                                        cardCount = 6,
                                        launchFullScreen = false
                                    )
                                },
                                onToggleComplete = { id, done, mins ->
                                    viewModel.toggleMaterialCompleted(id, done, mins)
                                },
                                onAddCustomMaterial = { title, desc, type, url, notes, cat, mins ->
                                    viewModel.addCustomMaterial(title, desc, type, url, notes, cat, mins)
                                },
                                onDeleteCustomMaterial = { id -> viewModel.deleteCustomMaterial(id) }
                            )
                            PortalTab.FLASHCARDS -> FlashcardsScreen(
                                flashcards = flashcards,
                                selectedDeckFilter = selectedDeckFilter,
                                materialsState = materialsState,
                                flashcardGenState = flashcardGenState,
                                onSelectDeckFilter = { filter -> viewModel.selectFlashcardDeck(filter) },
                                onRecordReview = { card, mastered -> viewModel.recordFlashcardReview(card, mastered) },
                                onDeleteCard = { card -> viewModel.deleteFlashcard(card) },
                                onGenerateFromMaterial = { mat, excerpt, count, fs ->
                                    viewModel.generateFlashcardsFromMaterial(mat, excerpt, count, fs)
                                },
                                onAddCustomCard = { deck, matId, front, back, hint ->
                                    viewModel.addCustomFlashcard(deck, matId, front, back, hint)
                                },
                                onOpenFullScreen = { viewModel.openFlashcardsFullScreen() }
                            )
                            PortalTab.MCQS -> McqScreen(
                                mcqGenState = mcqGenState,
                                quizSession = quizSession,
                                attemptsState = quizAttemptsState,
                                typingFeedbackMap = typingFeedbackMap,
                                typingGradingId = typingGradingId,
                                onGradeTypingAnswer = { q, ans -> viewModel.gradeTypingAnswer(q, ans) },
                                onSelectMcqBank = { bank, fs ->
                                    viewModel.selectMcqBank(bank, fs)
                                },
                                onGenerateAndStartFullScreen = { topic, notes, count, fs ->
                                    viewModel.generateAndStartMcqQuiz(topic, notes, count, fs)
                                },
                                onStartCurrentFullScreen = { viewModel.startCurrentMcqFullScreen() },
                                onSelectOption = { qIdx, optIdx -> viewModel.selectQuizOption(qIdx, optIdx) },
                                onSelectQuestionIndex = { idx -> viewModel.setQuizQuestionIndex(idx) },
                                onResetQuiz = { viewModel.resetCurrentQuiz() },
                                onSubmitQuiz = { viewModel.submitCurrentQuiz(fullScreenUsed = false) }
                            )
                            PortalTab.TUTOR -> TutorScreen(
                                messages = tutorMessages,
                                streamingText = tutorStreamingText,
                                tutorState = tutorState,
                                engineMode = tutorEngineMode,
                                freeRefState = freeRefState,
                                freeRefResult = freeRefResult,
                                isFullScreen = false,
                                onToggleFullScreen = {
                                    viewModel.enterFullScreen(FullScreenMode.TutorStudio())
                                },
                                onSelectEngine = { mode -> viewModel.selectTutorEngine(mode) },
                                onLookupWiki = { query -> viewModel.lookupWikipediaConcept(query) },
                                onLookupDictionary = { word -> viewModel.lookupDictionaryTerm(word) },
                                onDismissReference = { viewModel.dismissFreeReference() },
                                onAskTutorAboutReference = { ref -> viewModel.askTutorAboutReference(ref) },
                                onSendMessage = { text, bmp -> viewModel.sendTutorMessage(text, bmp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: PortalTab) = when (tab) {
    PortalTab.DASHBOARD -> Icons.Rounded.Dashboard
    PortalTab.MATERIALS -> Icons.Rounded.FolderCopy
    PortalTab.FLASHCARDS -> Icons.Rounded.Style
    PortalTab.MCQS -> Icons.Rounded.Quiz
    PortalTab.TUTOR -> Icons.Rounded.AutoAwesome
}
