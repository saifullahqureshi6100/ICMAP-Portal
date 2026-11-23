package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiTutorRepository
import com.example.data.FullScreenMode
import com.example.data.MaterialProgress
import com.example.data.MaterialType
import com.example.data.McqBank
import com.example.data.McqQuestion
import com.example.data.OmMcqBanks
import com.example.data.PomodoroPhase
import com.example.data.PomodoroState
import com.example.data.PortalTab
import com.example.data.PreloadedMaterials
import com.example.data.QuizAttempt
import com.example.data.StudyMaterial
import com.example.data.StudyPlanItem
import com.example.data.StudyRepository
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
    initialDisplayName: String
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

    private var tutorJob: Job? = null

    // MCQ Generator state
    private val _mcqGenState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val mcqGenState: StateFlow<AiTaskState> = _mcqGenState.asStateFlow()

    private val _generatedMcqs = MutableStateFlow<Pair<String, List<McqQuestion>>?>(
        OmMcqBanks.banks.first().let { it.title to it.questions }
    )
    val generatedMcqs: StateFlow<Pair<String, List<McqQuestion>>?> = _generatedMcqs.asStateFlow()

    private val _planGenState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val planGenState: StateFlow<AiTaskState> = _planGenState.asStateFlow()

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
        _fullScreenMode.value = null
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun openMaterialInFullScreen(material: StudyMaterial, asVideo: Boolean = false) {
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

        val priorHistory = _tutorMessages.value
            .drop(1) // Skip static greeting
            .takeLast(10)
            .map { msg ->
                content(if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }

        _tutorMessages.value = _tutorMessages.value + userMsg
        _tutorState.value = AiTaskState.Loading
        _tutorStreamingText.value = ""

        tutorJob = viewModelScope.launch {
            var latestText = ""
            try {
                aiRepo.streamTutorResponse(effectivePrompt, priorHistory, attachedImage).collect { accumulated ->
                    latestText = accumulated
                    _tutorStreamingText.value = accumulated
                }
                _tutorMessages.value = _tutorMessages.value + TutorMessage(
                    id = "m_${UUID.randomUUID().toString().take(8)}",
                    isUser = false,
                    text = latestText
                )
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
                _generatedMcqs.value = topic to questions
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

    fun selectMcqBank(bank: McqBank, launchFullScreen: Boolean) {
        _generatedMcqs.value = bank.title to bank.questions
        if (launchFullScreen) {
            _fullScreenMode.value = FullScreenMode.McqExam(topic = bank.title, questions = bank.questions)
        } else {
            _feedbackMessage.value = "Loaded: ${bank.title}"
        }
    }

    fun startCurrentMcqFullScreen() {
        val current = _generatedMcqs.value ?: return
        _fullScreenMode.value = FullScreenMode.McqExam(topic = current.first, questions = current.second)
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
