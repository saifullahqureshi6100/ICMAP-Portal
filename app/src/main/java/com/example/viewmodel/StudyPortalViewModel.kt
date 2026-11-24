package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiTutorRepository
import com.example.data.Flashcard
import com.example.data.FreeExternalApiService
import com.example.data.FreeReferenceResult
import com.example.data.FullScreenMode
import com.example.data.MaterialProgress
import com.example.data.MaterialType
import com.example.data.McqBank
import com.example.data.McqQuestion
import com.example.data.OmMcqBanks
import com.example.data.PomodoroPhase
import com.example.data.PomodoroState
import com.example.data.PortalTab
import com.example.data.PreloadedFlashcards
import com.example.data.PreloadedMaterials
import com.example.data.QuizAttempt
import com.example.data.QuizSessionState
import com.example.data.ReaderAiAction
import com.example.data.ReaderAiSessionState
import com.example.data.StudyMaterial
import com.example.data.StudyPlanItem
import com.example.data.StudyRepository
import com.example.data.TutorEngineMode
import com.example.data.TutorMessage
import com.example.data.UserProfile
import com.google.firebase.ai.type.content
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface StreamUiState<out T> {
    data object Loading : StreamUiState<Nothing>
    data class Success<T>(val data: T) : StreamUiState<T>
    data class Error(val message: String) : StreamUiState<Nothing>
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data object Loading : AiTaskState
    data class Error(val message: String) : AiTaskState
}

class StudyPortalViewModel(
    private val studyRepo: StudyRepository,
    private val aiRepo: AiTutorRepository,
    private val currentUserId: String,
    initialDisplayName: String,
    private val freeApiService: FreeExternalApiService = FreeExternalApiService()
) : ViewModel() {

    private fun <T> Flow<T>.toSafeUiState(fallbackError: String): StateFlow<StreamUiState<T>> =
        map<T, StreamUiState<T>> { StreamUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(StreamUiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamUiState.Loading
            )

    val userProfileState: StateFlow<StreamUiState<UserProfile?>> =
        studyRepo.observeUserProfile(currentUserId)
            .toSafeUiState("Failed to load profile")

    val materialsState: StateFlow<StreamUiState<List<StudyMaterial>>> =
        studyRepo.observeCustomMaterials(currentUserId)
            .map { custom -> PreloadedMaterials.items + custom }
            .toSafeUiState("Failed to load study materials")

    val progressMapState: StateFlow<StreamUiState<Map<String, MaterialProgress>>> =
        studyRepo.observeMaterialProgress(currentUserId)
            .map { list -> list.associateBy { it.materialId } }
            .toSafeUiState("Failed to load progress")

    val studyPlansState: StateFlow<StreamUiState<List<StudyPlanItem>>> =
        studyRepo.observeStudyPlans(currentUserId)
            .toSafeUiState("Failed to load study plan")

    val quizAttemptsState: StateFlow<StreamUiState<List<QuizAttempt>>> =
        studyRepo.observeQuizAttempts(currentUserId)
            .toSafeUiState("Failed to load quiz history")

    private val savedFlashcardsState: StateFlow<StreamUiState<List<Flashcard>>> =
        studyRepo.observeFlashcards(currentUserId)
            .toSafeUiState("Failed to load flashcards")

    private val _starterMasteryMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _starterReviewMap = MutableStateFlow<Map<String, Long>>(emptyMap())

    val flashcardsState: StateFlow<List<Flashcard>> = combine(
        savedFlashcardsState,
        _starterMasteryMap,
        _starterReviewMap
    ) { savedRes, starterMastery, starterReviews ->
        val saved = (savedRes as? StreamUiState.Success)?.data ?: emptyList()
        val starters = PreloadedFlashcards.cards.map { card ->
            card.copy(
                mastered = starterMastery[card.id] ?: card.mastered,
                reviewCount = starterReviews[card.id] ?: card.reviewCount
            )
        }
        saved + starters
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PreloadedFlashcards.cards
    )

    private val _activeTab = MutableStateFlow(PortalTab.DASHBOARD)
    val activeTab: StateFlow<PortalTab> = _activeTab.asStateFlow()

    private val _fullScreenMode = MutableStateFlow<FullScreenMode?>(null)
    val fullScreenMode: StateFlow<FullScreenMode?> = _fullScreenMode.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // AI Tutor state
    private val _tutorMessages = MutableStateFlow(
        listOf(
            TutorMessage(
                id = "welcome_msg",
                isUser = false,
                text = "Welcome to your Study Portal AI Tutor! Ask me to explain any concept from your Google Drive modules, create flashcards, walk through practice problems, or analyze a photo of your notes."
            )
        )
    )
    val tutorMessages: StateFlow<List<TutorMessage>> = _tutorMessages.asStateFlow()

    private val _tutorStreamingText = MutableStateFlow<String?>(null)
    val tutorStreamingText: StateFlow<String?> = _tutorStreamingText.asStateFlow()

    private val _tutorState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val tutorState: StateFlow<AiTaskState> = _tutorState.asStateFlow()

    private val _tutorEngineMode = MutableStateFlow(TutorEngineMode.GEMINI_BUILTIN)
    val tutorEngineMode: StateFlow<TutorEngineMode> = _tutorEngineMode.asStateFlow()

    private val _freeRefState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val freeRefState: StateFlow<AiTaskState> = _freeRefState.asStateFlow()

    private val _freeRefResult = MutableStateFlow<FreeReferenceResult?>(null)
    val freeRefResult: StateFlow<FreeReferenceResult?> = _freeRefResult.asStateFlow()

    private var tutorJob: Job? = null
    private var freeRefJob: Job? = null

    private val _readerAiState = MutableStateFlow(ReaderAiSessionState())
    val readerAiState: StateFlow<ReaderAiSessionState> = _readerAiState.asStateFlow()
    private var readerAiJob: Job? = null
    private var lastReaderMaterial: StudyMaterial? = null
    private var lastReaderSnippet: String = ""
    private var lastReaderPageLabel: String? = null
    private var lastReaderBitmap: Bitmap? = null

    private var lastViewedMaterial: StudyMaterial = PreloadedMaterials.items.first()

    // MCQ Generator & Live Quiz Session state
    private val _mcqGenState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val mcqGenState: StateFlow<AiTaskState> = _mcqGenState.asStateFlow()

    private val _quizSession = MutableStateFlow(
        OmMcqBanks.banks.first().let { bank ->
            QuizSessionState(topic = bank.title, questions = bank.questions)
        }
    )
    val quizSession: StateFlow<QuizSessionState> = _quizSession.asStateFlow()

    private val _planGenState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val planGenState: StateFlow<AiTaskState> = _planGenState.asStateFlow()

    private val _flashcardGenState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val flashcardGenState: StateFlow<AiTaskState> = _flashcardGenState.asStateFlow()

    private val _selectedDeckFilter = MutableStateFlow<String?>(null)
    val selectedDeckFilter: StateFlow<String?> = _selectedDeckFilter.asStateFlow()

    // Pomodoro Focus Timer state
    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()
    private var pomodoroTickerJob: Job? = null

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        userProfileState,
        materialsState,
        progressMapState,
        studyPlansState,
        quizAttemptsState
    ) { profileRes, materialsRes, progressRes, plansRes, attemptsRes ->
        val profile = (profileRes as? StreamUiState.Success)?.data
        val materials = (materialsRes as? StreamUiState.Success)?.data ?: PreloadedMaterials.items
        val progress = (progressRes as? StreamUiState.Success)?.data ?: emptyMap()
        val plans = (plansRes as? StreamUiState.Success)?.data ?: emptyList()
        val attempts = (attemptsRes as? StreamUiState.Success)?.data ?: emptyList()

        val completedMaterialsCount = materials.count { progress[it.id]?.completed == true }
        val completedPlansCount = plans.count { it.completed }
        val avgScore = if (attempts.isNotEmpty()) {
            attempts.map { it.accuracyPercent }.average().toInt()
        } else 0

        DashboardSummary(
            profile = profile,
            totalMaterials = materials.size,
            completedMaterials = completedMaterialsCount,
            totalPlans = plans.size,
            completedPlans = completedPlansCount,
            quizCount = attempts.size,
            averageQuizAccuracy = avgScore
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardSummary()
    )

    init {
        viewModelScope.launch {
            studyRepo.ensureUserProfile(currentUserId, initialDisplayName)
        }
    }

    fun selectTab(tab: PortalTab) {
        _activeTab.value = tab
    }

    fun enterFullScreen(mode: FullScreenMode) {
        _fullScreenMode.value = mode
    }

    fun exitFullScreen() {
        dismissReaderAiSheet()
        _fullScreenMode.value = null
    }

    fun toggleFullScreenForActiveTab() {
        if (_fullScreenMode.value != null) {
            exitFullScreen()
            return
        }
        when (_activeTab.value) {
            PortalTab.DASHBOARD -> {
                _fullScreenMode.value = FullScreenMode.RoadmapFocus
            }
            PortalTab.MATERIALS -> {
                val materials = (materialsState.value as? StreamUiState.Success)?.data ?: PreloadedMaterials.items
                val target = materials.firstOrNull { it.id == lastViewedMaterial.id } ?: materials.firstOrNull()
                if (target != null) {
                    openMaterialInFullScreen(target, target.resolvedType() == MaterialType.VIDEO_CLIP)
                }
            }
            PortalTab.MCQS -> {
                startCurrentMcqFullScreen()
            }
            PortalTab.FLASHCARDS -> {
                _fullScreenMode.value = FullScreenMode.FlashcardDeck(_selectedDeckFilter.value)
            }
            PortalTab.TUTOR -> {
                _fullScreenMode.value = FullScreenMode.TutorStudio()
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun openMaterialInFullScreen(material: StudyMaterial, asVideo: Boolean = false) {
        if (lastReaderMaterial?.id != material.id) {
            dismissReaderAiSheet()
        }
        lastViewedMaterial = material
        _fullScreenMode.value = FullScreenMode.ReadingViewer(material = material, openAsVideo = asVideo)
    }

    fun launchTutorForMaterial(material: StudyMaterial) {
        val prompt = if (material.isBundledPdf) {
            "I am studying \"${material.title}\" (${material.category}). Give me an ICMA ML-1 exam-focused breakdown: 4 high-yield concepts likely to be tested, common pitfalls, and 1 5-mark past-paper practice question."
        } else {
            "Help me study \"${material.title}\" (${material.category}). Give me a crisp study guide with 3 key takeaways, common pitfalls, and 1 check-yourself question."
        }
        _activeTab.value = PortalTab.TUTOR
        _fullScreenMode.value = FullScreenMode.TutorStudio(initialPrompt = prompt)
        sendTutorMessage(prompt, null)
    }

    fun toggleMaterialCompleted(materialId: String, completed: Boolean, minutesLogged: Long = 0L) {
        val existing = (progressMapState.value as? StreamUiState.Success)?.data?.get(materialId)
        viewModelScope.launch {
            val res = studyRepo.toggleMaterialCompletion(
                userId = currentUserId,
                materialId = materialId,
                completed = completed,
                existingProgress = existing,
                additionalMinutes = minutesLogged
            )
            if (completed && res.isSuccess) {
                if (minutesLogged > 0L) {
                    studyRepo.logStudySession(currentUserId, minutesLogged)
                }
                _feedbackMessage.value = "Material marked completed!"
            } else if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update progress."
            }
        }
    }

    fun saveMaterialNotes(materialId: String, notes: String) {
        val existing = (progressMapState.value as? StreamUiState.Success)?.data?.get(materialId)
        viewModelScope.launch {
            val res = studyRepo.saveMaterialNotes(currentUserId, materialId, notes, existing)
            _feedbackMessage.value = if (res.isSuccess) {
                "Notes saved to your study profile."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not save notes."
            }
        }
    }

    fun addCustomMaterial(
        title: String,
        description: String,
        type: MaterialType,
        sourceUrl: String,
        contentMarkdown: String,
        category: String,
        estimatedMinutes: Long
    ) {
        viewModelScope.launch {
            val res = studyRepo.addCustomMaterial(
                userId = currentUserId,
                title = title,
                description = description,
                materialType = type,
                sourceUrl = sourceUrl,
                contentMarkdown = contentMarkdown,
                category = category,
                estimatedMinutes = estimatedMinutes
            )
            _feedbackMessage.value = if (res.isSuccess) {
                "Study material added!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to add material."
            }
        }
    }

    fun deleteCustomMaterial(materialId: String) {
        viewModelScope.launch {
            val res = studyRepo.deleteCustomMaterial(currentUserId, materialId)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not delete material."
            }
        }
    }

    fun updateStudyGoal(goal: String, dailyTargetMinutes: Long) {
        viewModelScope.launch {
            val res = studyRepo.updateStudyGoal(currentUserId, goal, dailyTargetMinutes)
            _feedbackMessage.value = if (res.isSuccess) {
                "Study goal updated!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update goal."
            }
        }
    }

    fun logQuickStudySession(minutes: Long) {
        viewModelScope.launch {
            val res = studyRepo.logStudySession(currentUserId, minutes)
            _feedbackMessage.value = if (res.isSuccess) {
                "Logged $minutes study minutes!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to log minutes."
            }
        }
    }

    fun addStudyPlanItem(title: String, topic: String, dayLabel: String, targetMinutes: Long) {
        viewModelScope.launch {
            val res = studyRepo.addStudyPlanItem(currentUserId, title, topic, dayLabel, targetMinutes)
            _feedbackMessage.value = if (res.isSuccess) {
                "Added to study plan."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to add task."
            }
        }
    }

    fun toggleStudyPlanItem(planId: String, completed: Boolean) {
        viewModelScope.launch {
            val res = studyRepo.toggleStudyPlanItem(currentUserId, planId, completed)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update task."
            }
        }
    }

    fun deleteStudyPlanItem(planId: String) {
        viewModelScope.launch {
            val res = studyRepo.deleteStudyPlanItem(currentUserId, planId)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete task."
            }
        }
    }

    fun generateAiStudyPlan(goal: String) {
        if (_planGenState.value is AiTaskState.Loading) return
        val materials = (materialsState.value as? StreamUiState.Success)?.data ?: PreloadedMaterials.items
        val summary = materials.joinToString("\n") { "- ${it.title} (${it.resolvedType().label}, ${it.estimatedMinutes}m)" }
        viewModelScope.launch {
            _planGenState.value = AiTaskState.Loading
            try {
                val generated = aiRepo.generateStudyPlan(goal, summary)
                generated.forEach { task ->
                    studyRepo.addStudyPlanItem(
                        userId = currentUserId,
                        title = task.title,
                        topic = task.topic,
                        dayLabel = task.dayLabel,
                        targetMinutes = task.targetMinutes
                    )
                }
                _planGenState.value = AiTaskState.Idle
                _feedbackMessage.value = "AI generated ${generated.size} study plan tasks!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _planGenState.value = AiTaskState.Error(e.localizedMessage ?: "Failed to generate plan.")
            }
        }
    }

    fun selectTutorEngine(mode: TutorEngineMode) {
        _tutorEngineMode.value = mode
        _tutorState.value = AiTaskState.Idle
        _feedbackMessage.value = "AI Tutor switched to ${mode.label} (${mode.subtitle})"
    }

    fun lookupWikipediaConcept(query: String) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) {
            _feedbackMessage.value = "Enter a topic (e.g. Balanced Scorecard, Kaizen, MRP) to search."
            return
        }
        freeRefJob?.cancel()
        _freeRefState.value = AiTaskState.Loading
        freeRefJob = viewModelScope.launch {
            try {
                val res = freeApiService.fetchWikipediaSummary(cleaned)
                _freeRefResult.value = res
                _freeRefState.value = AiTaskState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _freeRefState.value = AiTaskState.Error(e.localizedMessage ?: "Wikipedia lookup failed.")
            }
        }
    }

    fun lookupDictionaryTerm(word: String) {
        val cleaned = word.trim()
        if (cleaned.isEmpty()) {
            _feedbackMessage.value = "Enter a word (e.g. synergy, ergonomics, strategy) to define."
            return
        }
        freeRefJob?.cancel()
        _freeRefState.value = AiTaskState.Loading
        freeRefJob = viewModelScope.launch {
            try {
                val res = freeApiService.fetchDictionaryDefinition(cleaned)
                _freeRefResult.value = res
                _freeRefState.value = AiTaskState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _freeRefState.value = AiTaskState.Error(e.localizedMessage ?: "Dictionary lookup failed.")
            }
        }
    }

    fun dismissFreeReference() {
        freeRefJob?.cancel()
        _freeRefState.value = AiTaskState.Idle
        _freeRefResult.value = null
    }

    fun askTutorAboutReference(ref: FreeReferenceResult) {
        val prompt = buildString {
            appendLine("Break down this concept from ${ref.sourceName} for exam revision:")
            appendLine("Topic: ${ref.title}")
            if (ref.subtitle.isNotBlank()) appendLine("Overview: ${ref.subtitle}")
            appendLine()
            appendLine(ref.summary.take(1200))
            appendLine()
            append("Explain key takeaways, a practical example, and give me 1 quick practice question.")
        }
        _freeRefResult.value = null
        sendTutorMessage(prompt, null)
    }

    fun sendTutorMessage(userPrompt: String, attachedImage: Bitmap?) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty() && attachedImage == null) return
        val effectivePrompt = if (trimmed.isEmpty()) "Analyze this study image and explain step-by-step." else trimmed

        tutorJob?.cancel()
        val userMsg = TutorMessage(
            id = "u_${UUID.randomUUID().toString().take(8)}",
            isUser = true,
            text = effectivePrompt,
            hasAttachment = attachedImage != null
        )

        val recentMessages = _tutorMessages.value
            .drop(1) // Skip static greeting
            .takeLast(10)

        val priorHistory = recentMessages.map { msg ->
            content(if (msg.isUser) "user" else "model") {
                text(msg.text)
            }
        }

        val engine = _tutorEngineMode.value
        if (engine == TutorEngineMode.POLLINATIONS_FREE && attachedImage != null) {
            _feedbackMessage.value = "Using Built-in Gemini Vision for attached image."
        }

        _tutorMessages.value = _tutorMessages.value + userMsg
        _tutorState.value = AiTaskState.Loading
        _tutorStreamingText.value = ""

        tutorJob = viewModelScope.launch {
            var latestText = ""
            try {
                if (engine == TutorEngineMode.POLLINATIONS_FREE && attachedImage == null) {
                    _tutorStreamingText.value = "Querying Pollinations Open AI ($0 No-Key endpoint)..."
                    val reply = freeApiService.askPollinationsChat(effectivePrompt, recentMessages)
                    _tutorMessages.value = _tutorMessages.value + TutorMessage(
                        id = "m_${UUID.randomUUID().toString().take(8)}",
                        isUser = false,
                        text = reply
                    )
                } else {
                    aiRepo.streamTutorResponse(effectivePrompt, priorHistory, attachedImage).collect { accumulated ->
                        latestText = accumulated
                        _tutorStreamingText.value = accumulated
                    }
                    _tutorMessages.value = _tutorMessages.value + TutorMessage(
                        id = "m_${UUID.randomUUID().toString().take(8)}",
                        isUser = false,
                        text = latestText
                    )
                }
                _tutorStreamingText.value = null
                _tutorState.value = AiTaskState.Idle
            } catch (e: CancellationException) {
                _tutorStreamingText.value = null
                throw e
            } catch (e: Exception) {
                _tutorStreamingText.value = null
                _tutorState.value = AiTaskState.Error(e.localizedMessage ?: "AI Tutor request failed.")
            }
        }
    }

    fun runReaderAiAction(
        material: StudyMaterial,
        action: ReaderAiAction,
        selectedText: String,
        pageLabel: String? = null,
        pageBitmap: Bitmap? = null
    ) {
        val cleaned = selectedText.trim()
        if (cleaned.isEmpty() && pageBitmap == null) {
            _feedbackMessage.value = "Select text or capture a page first."
            return
        }

        lastReaderMaterial = material
        lastReaderSnippet = cleaned
        lastReaderPageLabel = pageLabel
        lastReaderBitmap = pageBitmap

        readerAiJob?.cancel()
        _readerAiState.value = ReaderAiSessionState(
            isVisible = true,
            action = action,
            materialId = material.id,
            materialTitle = material.title,
            materialCategory = material.category,
            selectedSnippet = cleaned,
            pageLabel = pageLabel,
            hasPageImage = pageBitmap != null,
            responseText = "",
            isLoading = true,
            errorMessage = null,
            savedToNotes = false
        )

        readerAiJob = viewModelScope.launch {
            try {
                aiRepo.streamSelectionTutorAction(
                    action = action,
                    materialTitle = material.title,
                    materialCategory = material.category,
                    selectedText = cleaned,
                    pageLabel = pageLabel,
                    pageBitmap = pageBitmap
                ).collect { accumulated ->
                    _readerAiState.value = _readerAiState.value.copy(
                        responseText = accumulated,
                        isLoading = true,
                        errorMessage = null
                    )
                }
                _readerAiState.value = _readerAiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _readerAiState.value = _readerAiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "AI Tutor failed to analyze selection."
                )
            }
        }
    }

    fun switchReaderAiAction(action: ReaderAiAction) {
        val mat = lastReaderMaterial ?: return
        runReaderAiAction(
            material = mat,
            action = action,
            selectedText = lastReaderSnippet,
            pageLabel = lastReaderPageLabel,
            pageBitmap = lastReaderBitmap
        )
    }

    fun retryReaderAiAction() {
        switchReaderAiAction(_readerAiState.value.action)
    }

    fun dismissReaderAiSheet() {
        readerAiJob?.cancel()
        readerAiJob = null
        _readerAiState.value = ReaderAiSessionState()
    }

    fun saveReaderAiToNotes() {
        val state = _readerAiState.value
        if (state.responseText.isBlank() || state.materialId.isBlank()) return
        val existingProgress = (progressMapState.value as? StreamUiState.Success)?.data?.get(state.materialId)
        val existingNotes = existingProgress?.personalNotes.orEmpty().trim()
        val header = buildString {
            append("--- AI ${state.action.shortLabel}")
            if (!state.pageLabel.isNullOrBlank()) append(" (${state.pageLabel})")
            append(" ---")
        }
        val snippetQuote = if (state.selectedSnippet.isNotBlank()) {
            "\nSource: \"${state.selectedSnippet.take(140)}${if (state.selectedSnippet.length > 140) "..." else ""}\""
        } else ""
        val combined = buildString {
            if (existingNotes.isNotEmpty()) {
                append(existingNotes)
                append("\n\n")
            }
            append(header)
            append(snippetQuote)
            append("\n")
            append(state.responseText.trim())
        }

        viewModelScope.launch {
            val res = studyRepo.saveMaterialNotes(currentUserId, state.materialId, combined, existingProgress)
            if (res.isSuccess) {
                _readerAiState.value = _readerAiState.value.copy(savedToNotes = true)
                _feedbackMessage.value = "AI notes appended to Personal Study Notes!"
            } else {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not save AI notes."
            }
        }
    }

    fun continueReaderAiInTutor() {
        val state = _readerAiState.value
        if (state.responseText.isBlank() && state.selectedSnippet.isBlank()) return
        val contextDesc = buildString {
            append("[${state.action.label} • ${state.materialTitle}")
            if (!state.pageLabel.isNullOrBlank()) append(" • ${state.pageLabel}")
            append("]\n")
            if (state.selectedSnippet.isNotBlank()) {
                append("\"${state.selectedSnippet.take(500)}\"")
            } else {
                append("Analyze ${state.pageLabel ?: "this reading page"}")
            }
        }
        val extraMsgs = buildList {
            add(
                TutorMessage(
                    id = "u_${UUID.randomUUID().toString().take(8)}",
                    isUser = true,
                    text = contextDesc,
                    hasAttachment = state.hasPageImage
                )
            )
            if (state.responseText.isNotBlank()) {
                add(
                    TutorMessage(
                        id = "m_${UUID.randomUUID().toString().take(8)}",
                        isUser = false,
                        text = state.responseText
                    )
                )
            }
        }
        _tutorMessages.value = _tutorMessages.value + extraMsgs
        dismissReaderAiSheet()
        _activeTab.value = PortalTab.TUTOR
        _fullScreenMode.value = FullScreenMode.TutorStudio()
    }

    fun generateAndStartMcqQuiz(
        topic: String,
        notesContext: String,
        count: Int,
        launchFullScreen: Boolean
    ) {
        if (_mcqGenState.value is AiTaskState.Loading) return
        viewModelScope.launch {
            _mcqGenState.value = AiTaskState.Loading
            try {
                val questions = aiRepo.generateMcqs(topic, notesContext, count)
                _quizSession.value = QuizSessionState(topic = topic, questions = questions)
                _mcqGenState.value = AiTaskState.Idle
                if (launchFullScreen) {
                    _fullScreenMode.value = FullScreenMode.McqExam(topic = topic, questions = questions)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mcqGenState.value = AiTaskState.Error(e.localizedMessage ?: "Failed to generate MCQs.")
            }
        }
    }

    fun selectFlashcardDeck(deckTitle: String?) {
        _selectedDeckFilter.value = deckTitle
    }

    fun openFlashcardsFullScreen(deckFilter: String? = _selectedDeckFilter.value) {
        _selectedDeckFilter.value = deckFilter
        _fullScreenMode.value = FullScreenMode.FlashcardDeck(deckFilter)
    }

    fun generateFlashcardsFromMaterial(
        material: StudyMaterial,
        customExcerpt: String = "",
        cardCount: Int = 6,
        launchFullScreen: Boolean = false
    ) {
        if (_flashcardGenState.value is AiTaskState.Loading) return
        val contextNotes = buildString {
            if (customExcerpt.isNotBlank()) {
                appendLine(customExcerpt.trim())
                appendLine()
            }
            if (material.description.isNotBlank()) {
                appendLine("Overview: ${material.description}")
            }
            if (material.contentMarkdown.isNotBlank()) {
                appendLine(material.contentMarkdown)
            }
        }.ifBlank {
            "Generate high-yield study flashcards on ${material.title} (${material.category})."
        }

        viewModelScope.launch {
            _flashcardGenState.value = AiTaskState.Loading
            try {
                val generated = aiRepo.generateFlashcards(
                    deckTitle = material.title,
                    sourceContent = contextNotes,
                    cardCount = cardCount
                )
                val saveRes = studyRepo.saveGeneratedFlashcards(
                    userId = currentUserId,
                    materialId = material.id,
                    deckTitle = material.title,
                    cards = generated
                )
                if (saveRes.isSuccess) {
                    val savedCount = saveRes.getOrDefault(generated.size)
                    _flashcardGenState.value = AiTaskState.Idle
                    _selectedDeckFilter.value = material.title.take(140)
                    _feedbackMessage.value = "Generated $savedCount flashcards from ${material.title}!"
                    if (launchFullScreen) {
                        _fullScreenMode.value = FullScreenMode.FlashcardDeck(_selectedDeckFilter.value)
                    }
                } else {
                    _flashcardGenState.value = AiTaskState.Error(
                        saveRes.exceptionOrNull()?.localizedMessage ?: "Failed to save flashcards."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _flashcardGenState.value = AiTaskState.Error(
                    e.localizedMessage ?: "Failed to generate flashcards."
                )
            }
        }
    }

    fun addCustomFlashcard(
        deckTitle: String,
        materialId: String,
        front: String,
        back: String,
        hint: String
    ) {
        viewModelScope.launch {
            val res = studyRepo.addCustomFlashcard(
                userId = currentUserId,
                materialId = materialId,
                deckTitle = deckTitle,
                front = front,
                back = back,
                hint = hint
            )
            _feedbackMessage.value = if (res.isSuccess) {
                _selectedDeckFilter.value = deckTitle.trim().ifBlank { "Custom Flashcards" }.take(140)
                "Flashcard saved to deck!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not save flashcard."
            }
        }
    }

    fun recordFlashcardReview(card: Flashcard, mastered: Boolean) {
        if (card.userId.isBlank()) {
            _starterMasteryMap.value = _starterMasteryMap.value + (card.id to mastered)
            _starterReviewMap.value = _starterReviewMap.value + (card.id to (card.reviewCount + 1L))
            return
        }
        viewModelScope.launch {
            val res = studyRepo.updateFlashcardMastery(
                userId = currentUserId,
                cardId = card.id,
                mastered = mastered,
                currentReviewCount = card.reviewCount
            )
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not update card mastery."
            }
        }
    }

    fun deleteFlashcard(card: Flashcard) {
        if (card.userId.isBlank()) {
            _feedbackMessage.value = "Starter OM cards remain available offline."
            return
        }
        viewModelScope.launch {
            val res = studyRepo.deleteFlashcard(currentUserId, card.id)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete flashcard."
            }
        }
    }

    fun selectMcqBank(bank: McqBank, launchFullScreen: Boolean) {
        _quizSession.value = QuizSessionState(topic = bank.title, questions = bank.questions)
        if (launchFullScreen) {
            _fullScreenMode.value = FullScreenMode.McqExam(topic = bank.title, questions = bank.questions)
        } else {
            _feedbackMessage.value = "Loaded: ${bank.title}"
        }
    }

    fun startCurrentMcqFullScreen() {
        val current = _quizSession.value
        if (current.questions.isEmpty()) return
        _fullScreenMode.value = FullScreenMode.McqExam(topic = current.topic, questions = current.questions)
    }

    fun selectQuizOption(questionIndex: Int, optionIndex: Int) {
        val current = _quizSession.value
        if (current.isSubmitted) return
        _quizSession.value = current.copy(
            selectedAnswers = current.selectedAnswers + (questionIndex to optionIndex)
        )
    }

    fun setQuizQuestionIndex(index: Int) {
        val current = _quizSession.value
        if (current.questions.isEmpty()) return
        _quizSession.value = current.copy(
            currentIndex = index.coerceIn(0, current.questions.lastIndex)
        )
    }

    fun tickQuizElapsed() {
        val current = _quizSession.value
        if (!current.isSubmitted && current.questions.isNotEmpty()) {
            _quizSession.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
        }
    }

    fun resetCurrentQuiz() {
        val current = _quizSession.value
        _quizSession.value = QuizSessionState(topic = current.topic, questions = current.questions)
    }

    fun submitCurrentQuiz(fullScreenUsed: Boolean) {
        val current = _quizSession.value
        if (current.isSubmitted || current.questions.isEmpty()) return
        _quizSession.value = current.copy(isSubmitted = true)
        submitQuizAttempt(
            topic = current.topic,
            totalQuestions = current.questions.size,
            correctCount = current.correctCount,
            durationSeconds = current.elapsedSeconds.toLong(),
            fullScreenUsed = fullScreenUsed
        )
    }

    fun submitQuizAttempt(
        topic: String,
        totalQuestions: Int,
        correctCount: Int,
        durationSeconds: Long,
        fullScreenUsed: Boolean
    ) {
        viewModelScope.launch {
            val res = studyRepo.recordQuizAttempt(
                userId = currentUserId,
                topic = topic,
                totalQuestions = totalQuestions.toLong(),
                correctCount = correctCount.toLong(),
                durationSeconds = durationSeconds,
                fullScreenUsed = fullScreenUsed
            )
            _feedbackMessage.value = if (res.isSuccess) {
                "Saved score: $correctCount/$totalQuestions!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not save quiz score."
            }
        }
    }

    // --- Pomodoro Timer Controls ---

    fun togglePomodoroRunPause() {
        val current = _pomodoroState.value
        if (current.isRunning) {
            pausePomodoro()
        } else {
            startPomodoro()
        }
    }

    private fun startPomodoro() {
        pomodoroTickerJob?.cancel()
        _pomodoroState.value = _pomodoroState.value.copy(isRunning = true)
        pomodoroTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _pomodoroState.value
                if (!state.isRunning) break
                if (state.remainingSeconds > 1) {
                    _pomodoroState.value = state.copy(remainingSeconds = state.remainingSeconds - 1)
                } else {
                    completeCurrentPomodoroPhase(completedNaturally = true)
                    if (!_pomodoroState.value.autoAdvance) {
                        break
                    }
                }
            }
        }
    }

    private fun pausePomodoro() {
        pomodoroTickerJob?.cancel()
        pomodoroTickerJob = null
        _pomodoroState.value = _pomodoroState.value.copy(isRunning = false)
    }

    fun resetPomodoro() {
        pausePomodoro()
        val current = _pomodoroState.value
        val phaseSeconds = phaseDurationSeconds(current, current.phase)
        _pomodoroState.value = current.copy(
            remainingSeconds = phaseSeconds,
            totalPhaseSeconds = phaseSeconds,
            isRunning = false
        )
    }

    fun skipPomodoroPhase() {
        completeCurrentPomodoroPhase(completedNaturally = false)
    }

    fun selectPomodoroPhase(phase: PomodoroPhase) {
        pausePomodoro()
        val current = _pomodoroState.value
        val phaseSeconds = phaseDurationSeconds(current, phase)
        _pomodoroState.value = current.copy(
            phase = phase,
            remainingSeconds = phaseSeconds,
            totalPhaseSeconds = phaseSeconds,
            isRunning = false
        )
    }

    fun updatePomodoroIntervals(
        workMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        cyclesBeforeLongBreak: Int,
        autoAdvance: Boolean
    ) {
        pausePomodoro()
        val safeWork = workMinutes.coerceIn(1, 120)
        val safeShort = shortBreakMinutes.coerceIn(1, 60)
        val safeLong = longBreakMinutes.coerceIn(5, 60)
        val safeCycles = cyclesBeforeLongBreak.coerceIn(2, 8)
        val updated = _pomodoroState.value.copy(
            workMinutes = safeWork,
            shortBreakMinutes = safeShort,
            longBreakMinutes = safeLong,
            cyclesBeforeLongBreak = safeCycles,
            autoAdvance = autoAdvance
        )
        val resetSeconds = phaseDurationSeconds(updated, updated.phase)
        _pomodoroState.value = updated.copy(
            remainingSeconds = resetSeconds,
            totalPhaseSeconds = resetSeconds,
            isRunning = false
        )
        _feedbackMessage.value = "Pomodoro updated: ${safeWork}m focus / ${safeShort}m break"
    }

    private fun completeCurrentPomodoroPhase(completedNaturally: Boolean) {
        val current = _pomodoroState.value
        var newCompletedCount = current.completedWorkSessions
        val nextPhase = when (current.phase) {
            PomodoroPhase.WORK -> {
                if (completedNaturally) {
                    newCompletedCount += 1
                    val workMins = current.workMinutes.toLong()
                    viewModelScope.launch {
                        studyRepo.logStudySession(currentUserId, workMins)
                    }
                    _feedbackMessage.value = "Focus block done! Logged ${current.workMinutes}m. Time for a break."
                }
                if (newCompletedCount > 0 && newCompletedCount % current.cyclesBeforeLongBreak == 0) {
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> {
                if (completedNaturally) {
                    _feedbackMessage.value = "Break finished! Ready for your next focus session."
                }
                PomodoroPhase.WORK
            }
        }
        val nextSeconds = phaseDurationSeconds(current, nextPhase)
        val shouldKeepRunning = completedNaturally && current.autoAdvance
        if (!shouldKeepRunning) {
            pomodoroTickerJob?.cancel()
            pomodoroTickerJob = null
        }
        _pomodoroState.value = current.copy(
            phase = nextPhase,
            completedWorkSessions = newCompletedCount,
            remainingSeconds = nextSeconds,
            totalPhaseSeconds = nextSeconds,
            isRunning = shouldKeepRunning
        )
    }

    private fun phaseDurationSeconds(state: PomodoroState, phase: PomodoroPhase): Int = when (phase) {
        PomodoroPhase.WORK -> state.workMinutes * 60
        PomodoroPhase.SHORT_BREAK -> state.shortBreakMinutes * 60
        PomodoroPhase.LONG_BREAK -> state.longBreakMinutes * 60
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5000L

        fun starterMcqQuestions(): List<McqQuestion> = listOf(
            McqQuestion(
                id = "starter_1",
                question = "Which study technique yields the highest long-term retention compared to passive re-reading?",
                options = listOf(
                    "Active recall with spaced practice testing",
                    "Highlighting entire paragraphs in bright colors",
                    "Re-reading the same chapter three times in one night",
                    "Copying definitions word-for-word without self-testing"
                ),
                correctIndex = 0,
                explanation = "Active recall forces memory retrieval, strengthening neural pathways far more effectively than passive review."
            ),
            McqQuestion(
                id = "starter_2",
                question = "When watching a recorded lecture video clip, what is the best way to avoid passive illusion of competence?",
                options = listOf(
                    "Play at 2x speed without pausing",
                    "Pause after each concept to summarize in your own words and solve an example",
                    "Keep multiple tabs open while listening in the background",
                    "Skip all worked examples and only read the final slide"
                ),
                correctIndex = 1,
                explanation = "Pausing to articulate ideas and test yourself converts passive viewing into active encoding."
            ),
            McqQuestion(
                id = "starter_3",
                question = "How does interleaved practice improve exam performance?",
                options = listOf(
                    "It eliminates the need to review foundational notes",
                    "It mixes related problem types so you practice selecting the right strategy",
                    "It guarantees shorter study sessions with zero effort",
                    "It focuses strictly on memorizing page numbers"
                ),
                correctIndex = 1,
                explanation = "Interleaving trains your brain to discriminate between problem types and choose the appropriate method under exam conditions."
            )
        )
    }
}

data class DashboardSummary(
    val profile: UserProfile? = null,
    val totalMaterials: Int = PreloadedMaterials.items.size,
    val completedMaterials: Int = 0,
    val totalPlans: Int = 0,
    val completedPlans: Int = 0,
    val quizCount: Int = 0,
    val averageQuizAccuracy: Int = 0
)
