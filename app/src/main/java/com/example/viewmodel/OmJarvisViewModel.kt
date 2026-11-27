package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TypingQuestion
import com.example.data.local.DescriptiveSubmissionEntity
import com.example.data.local.FlashcardEntity
import com.example.data.local.MockExamStateEntity
import com.example.data.local.OmJarvisDatabase
import com.example.data.local.OmJarvisRepository
import com.example.data.local.QuestionAttemptEntity
import com.example.data.local.ScheduleSlotEntity
import com.example.data.local.SpacedRepRating
import com.example.data.local.StudySessionEntity
import com.example.data.local.TestAttemptEntity
import com.example.data.local.TopicProgressEntity
import com.example.data.local.VideoProgressEntity
import com.example.data.preferences.AiProviderType
import com.example.data.preferences.MotivationFrequency
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.StudyLanguageMode
import com.example.data.security.SecureAiProviderManager
import com.example.data.security.TutorStyleRequest
import com.example.data.syllabus.OmSyllabusCatalog
import com.example.data.syllabus.PastPaperEntry
import com.example.data.syllabus.SyllabusDatabaseBundle
import com.example.data.syllabus.SyllabusTopic
import com.example.data.syllabus.TopicMappedMcq
import com.example.data.video.VideoCatalogAudit
import com.example.data.video.VideoCatalogManager
import com.example.domain.AdaptiveMcqEngine
import com.example.domain.AnalyticsSnapshot
import com.example.domain.DescriptiveEvaluationResult
import com.example.domain.DescriptiveEvaluator
import com.example.domain.JarvisReadinessEngine
import com.example.work.ReminderScheduler
import com.example.work.StudyReminderType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class McqRunnerState(
    val modeTitle: String = "",
    val partCode: String = "ALL",
    val topicId: String = "",
    val questions: List<TopicMappedMcq> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val flaggedIndices: Set<Int> = emptySet(),
    val showUnansweredOnly: Boolean = false,
    val isTimedMode: Boolean = false,
    val isMockExam: Boolean = false,
    val totalDurationSeconds: Int = 600,
    val remainingSeconds: Int = 600,
    val elapsedSeconds: Int = 0,
    val isSubmitted: Boolean = false,
    val lastAttemptSummary: TestAttemptEntity? = null
) {
    val isActive: Boolean get() = questions.isNotEmpty()
    val correctCount: Int
        get() = questions.indices.count { i -> selectedAnswers[i] == questions[i].mcq.correctIndex }
}

data class JarvisChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val styleLabel: String = "",
    val providerLabel: String = "",
    val isOfflineFallback: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class GlobalSearchResults(
    val topics: List<SyllabusTopic> = emptyList(),
    val videos: List<VideoProgressEntity> = emptyList(),
    val flashcards: List<FlashcardEntity> = emptyList(),
    val mcqs: List<TopicMappedMcq> = emptyList(),
    val notesMatches: List<Pair<SyllabusTopic, TopicProgressEntity>> = emptyList(),
    val pastPapers: List<PastPaperEntry> = emptyList()
) {
    val totalHits: Int
        get() = topics.size + videos.size + flashcards.size + mcqs.size + notesMatches.size + pastPapers.size
}

class OmJarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = OmJarvisDatabase.getInstance(appContext)
    private val repo = OmJarvisRepository(appContext, db)
    private val videoManager = VideoCatalogManager(appContext, db.videoProgressDao())
    private val aiManager = SecureAiProviderManager(appContext)

    private val _bundle = MutableStateFlow(OmSyllabusCatalog.loadBundle(appContext))
    val bundle: StateFlow<SyllabusDatabaseBundle> = _bundle.asStateFlow()

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val settings: StateFlow<OmJarvisSettings> = repo.prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OmJarvisSettings())

    val topicProgressMap: StateFlow<Map<String, TopicProgressEntity>> = repo.observeTopicProgress()
        .map { list -> list.associateBy { it.topicId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val videos: StateFlow<List<VideoProgressEntity>> = repo.observeVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardEntity>> = repo.observeFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionAttemptsMap: StateFlow<Map<String, QuestionAttemptEntity>> = repo.observeQuestionAttempts()
        .map { list -> list.associateBy { it.questionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val testAttempts: StateFlow<List<TestAttemptEntity>> = repo.observeTestAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val descriptiveSubmissions: StateFlow<List<DescriptiveSubmissionEntity>> = repo.observeDescriptive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions: StateFlow<List<StudySessionEntity>> = repo.observeStudySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleSlots: StateFlow<List<ScheduleSlotEntity>> = repo.observeScheduleSlots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMockState: StateFlow<MockExamStateEntity?> = repo.observeActiveMock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _catalogAudit = MutableStateFlow<VideoCatalogAudit?>(null)
    val catalogAudit: StateFlow<VideoCatalogAudit?> = _catalogAudit.asStateFlow()

    private val _isAuditingCatalog = MutableStateFlow(false)
    val isAuditingCatalog: StateFlow<Boolean> = _isAuditingCatalog.asStateFlow()

    private val _selectedTopicId = MutableStateFlow("om_u14_bsc")
    val selectedTopicId: StateFlow<String> = _selectedTopicId.asStateFlow()

    private val _selectedPartFilter = MutableStateFlow<String?>(null)
    val selectedPartFilter: StateFlow<String?> = _selectedPartFilter.asStateFlow()

    private val _mcqRunnerState = MutableStateFlow(McqRunnerState())
    val mcqRunnerState: StateFlow<McqRunnerState> = _mcqRunnerState.asStateFlow()

    private val _lastDescriptiveEval = MutableStateFlow<DescriptiveEvaluationResult?>(null)
    val lastDescriptiveEval: StateFlow<DescriptiveEvaluationResult?> = _lastDescriptiveEval.asStateFlow()

    private val _tutorMessages = MutableStateFlow(
        listOf(
            JarvisChatMessage(
                isUser = false,
                text = "Assalam-o-Alaikum Saifullah! Main OM Jarvis hoon — aapka ICMA M3 Organizational Management mentor. Koi bhi topic, Roman Urdu explanation, Pakistani corporate example, ya 5-mark exam answer poochein.",
                styleLabel = "Mentor Ready",
                providerLabel = "Offline Jarvis Mode",
                isOfflineFallback = true
            )
        )
    )
    val tutorMessages: StateFlow<List<JarvisChatMessage>> = _tutorMessages.asStateFlow()

    private val _tutorLoading = MutableStateFlow(false)
    val tutorLoading: StateFlow<Boolean> = _tutorLoading.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<String?>(null)
    val feedbackBanner: StateFlow<String?> = _feedbackBanner.asStateFlow()

    private var timerJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    val analytics: StateFlow<AnalyticsSnapshot> = combine(
        bundle,
        settings,
        topicProgressMap,
        videos,
        flashcards,
        testAttempts,
        descriptiveSubmissions,
        questionAttemptsMap,
        studySessions
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val b = arr[0] as SyllabusDatabaseBundle
        val s = arr[1] as OmJarvisSettings
        @Suppress("UNCHECKED_CAST")
        val tp = arr[2] as Map<String, TopicProgressEntity>
        @Suppress("UNCHECKED_CAST")
        val vids = arr[3] as List<VideoProgressEntity>
        @Suppress("UNCHECKED_CAST")
        val fcs = arr[4] as List<FlashcardEntity>
        @Suppress("UNCHECKED_CAST")
        val tests = arr[5] as List<TestAttemptEntity>
        @Suppress("UNCHECKED_CAST")
        val desc = arr[6] as List<DescriptiveSubmissionEntity>
        @Suppress("UNCHECKED_CAST")
        val qa = arr[7] as Map<String, QuestionAttemptEntity>
        @Suppress("UNCHECKED_CAST")
        val sess = arr[8] as List<StudySessionEntity>

        JarvisReadinessEngine.computeAnalytics(
            bundle = b,
            examDateMillis = s.examDateMillis,
            meritMode = s.meritModeEnabled,
            dailyTargetMinutes = s.dailyTargetMinutes,
            topicProgressMap = tp,
            videos = vids,
            flashcards = fcs,
            testAttempts = tests,
            descriptiveSubmissions = desc,
            questionAttempts = qa,
            studySessions = sess
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        JarvisReadinessEngine.computeAnalytics(
            bundle = _bundle.value,
            examDateMillis = OmJarvisSettings.defaultDec7ExamMillis(),
            meritMode = true,
            dailyTargetMinutes = 180,
            topicProgressMap = emptyMap(),
            videos = emptyList(),
            flashcards = emptyList(),
            testAttempts = emptyList(),
            descriptiveSubmissions = emptyList(),
            questionAttempts = emptyMap(),
            studySessions = emptyList()
        )
    )

    init {
        registerConnectivityObserver()
        viewModelScope.launch {
            val loadedBundle = _bundle.value
            repo.initializeOfflineDatabase(loadedBundle)
            val saved = repo.prefs.settingsFlow.first()
            if (saved.lastStudiedTopicId.isNotBlank()) {
                _selectedTopicId.value = saved.lastStudiedTopicId
            }
            // Audit local files instantly, then probe GitHub catalog if online
            val localAudit = videoManager.seedAndAuditCatalog(loadedBundle, performRemoteCheck = false)
            _catalogAudit.value = localAudit
            if (_isOnline.value) {
                refreshVideoCatalog(silent = true)
            }
            if (saved.onboardingCompleted && saved.notificationsEnabled) {
                ReminderScheduler.scheduleAll(appContext, saved)
            }
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerConnectivityObserver() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = checkCurrentConnectivity()
            }

            override fun onLost(network: Network) {
                _isOnline.value = checkCurrentConnectivity()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        networkCallback = cb
        try {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), cb)
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback?.let { runCatching { cm?.unregisterNetworkCallback(it) } }
    }

    fun clearFeedback() {
        _feedbackBanner.value = null
    }

    fun showFeedback(message: String) {
        _feedbackBanner.value = message
    }

    fun completeOnboarding(
        meritMode: Boolean,
        darkTheme: Boolean,
        dailyTargetMinutes: Int,
        notificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            repo.prefs.update {
                it.copy(
                    onboardingCompleted = true,
                    meritModeEnabled = meritMode,
                    darkThemeEnabled = darkTheme,
                    dailyTargetMinutes = dailyTargetMinutes,
                    notificationsEnabled = notificationsEnabled
                )
            }
            val updated = repo.prefs.settingsFlow.first()
            ReminderScheduler.scheduleAll(appContext, updated)
            _feedbackBanner.value = "OM Jarvis Command Center ready for Dec 7 Merit Exam!"
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            repo.prefs.update { it.copy(darkThemeEnabled = !it.darkThemeEnabled) }
        }
    }

    fun updateSettings(transform: (OmJarvisSettings) -> OmJarvisSettings) {
        viewModelScope.launch {
            repo.prefs.update(transform)
            val updated = repo.prefs.settingsFlow.first()
            ReminderScheduler.scheduleAll(appContext, updated)
        }
    }

    fun selectTopic(topicId: String) {
        _selectedTopicId.value = topicId
        val topic = _bundle.value.topics.firstOrNull { it.id == topicId } ?: return
        viewModelScope.launch {
            repo.prefs.saveLastStudied(topic.id, "${topic.name} • Unit ${topic.unit}")
        }
    }

    fun selectPartFilter(partCode: String?) {
        _selectedPartFilter.value = partCode
    }

    fun moveRelativeTopic(delta: Int) {
        val list = _bundle.value.topics
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.id == _selectedTopicId.value }.coerceAtLeast(0)
        val next = (idx + delta).coerceIn(0, list.lastIndex)
        selectTopic(list[next].id)
    }

    fun recordReadingProgress(topic: SyllabusTopic, percent: Int, paragraph: Int, markDone: Boolean? = null) {
        viewModelScope.launch {
            repo.recordReadingProgress(topic.id, topic.partCode, topic.unit, percent, paragraph, markDone)
            repo.prefs.saveLastStudied(topic.id, "${topic.name} • Unit ${topic.unit}")
            if (markDone == true) {
                repo.logStudyMinutes(15, "READING", topic.id)
                _feedbackBanner.value = "${topic.name} reading marked complete!"
            }
        }
    }

    fun saveTopicNotesAndFlags(
        topic: SyllabusTopic,
        notes: String? = null,
        highlightSnippet: String? = null,
        bookmarked: Boolean? = null,
        important: Boolean? = null,
        revision: Boolean? = null
    ) {
        viewModelScope.launch {
            repo.saveTopicNotesAndFlags(
                topicId = topic.id,
                partCode = topic.partCode,
                unitNumber = topic.unit,
                notes = notes,
                highlightSnippet = highlightSnippet,
                bookmarked = bookmarked,
                important = important,
                revision = revision
            )
            if (notes != null) _feedbackBanner.value = "Personal note saved offline."
            if (highlightSnippet != null) _feedbackBanner.value = "Key snippet highlighted."
        }
    }

    fun logQuickMinutes(minutes: Int, activityType: String = "DEEP_STUDY", topicId: String = _selectedTopicId.value) {
        viewModelScope.launch {
            repo.logStudyMinutes(minutes, activityType, topicId)
            _feedbackBanner.value = "+$minutes min logged to today's consistency score!"
        }
    }

    // --- Video Catalog & Media3 Offline Controls ---

    fun refreshVideoCatalog(silent: Boolean = false) {
        viewModelScope.launch {
            _isAuditingCatalog.value = true
            val audit = videoManager.seedAndAuditCatalog(_bundle.value, performRemoteCheck = true)
            _catalogAudit.value = audit
            _isAuditingCatalog.value = false
            if (!silent) {
                _feedbackBanner.value =
                    "Catalog audited: ${audit.availableCount}/${audit.configuredCount} videos playable (${audit.downloadedCount} offline)."
            }
        }
    }

    fun onVideoPlaybackTick(videoId: String, positionMs: Long, durationMs: Long, explicitComplete: Boolean? = null) {
        viewModelScope.launch {
            val threshold = settings.value.videoCompletionThresholdPct
            repo.updateVideoPlaybackProgress(videoId, positionMs, durationMs, threshold, explicitComplete)
            if (explicitComplete == true) {
                repo.logStudyMinutes(20, "VIDEO_COMPLETE", videoId.removePrefix("vid_"))
                _feedbackBanner.value = "Video lecture marked complete!"
            }
        }
    }

    fun startOrResumeDownload(video: VideoProgressEntity) {
        videoManager.startOrResumeDownload(video)
        _feedbackBanner.value = "Downloading ${video.filename} for offline study..."
    }

    fun pauseDownload(video: VideoProgressEntity) {
        viewModelScope.launch { videoManager.pauseDownload(video) }
    }

    fun cancelDownload(video: VideoProgressEntity) {
        viewModelScope.launch { videoManager.cancelDownload(video) }
    }

    fun deleteCachedVideo(video: VideoProgressEntity) {
        viewModelScope.launch {
            videoManager.deleteCachedVideo(video)
            _feedbackBanner.value = "Removed offline cache for ${video.filename}"
        }
    }

    fun clearAllCachedVideos() {
        viewModelScope.launch {
            videoManager.clearAllCachedVideos()
            refreshVideoCatalog(silent = true)
            _feedbackBanner.value = "Cleared all offline video cache."
        }
    }

    fun getVideoCacheSizeMb(): String {
        val bytes = videoManager.getTotalStorageUsedBytes()
        return String.format("%.1f MB", bytes / (1024f * 1024f))
    }

    // --- SM-2 Flashcards ---

    fun reviewFlashcard(card: FlashcardEntity, rating: SpacedRepRating) {
        viewModelScope.launch {
            repo.reviewFlashcard(card, rating)
            repo.logStudyMinutes(2, "FLASHCARD", card.topicId)
        }
    }

    fun addCustomFlashcard(topic: SyllabusTopic, front: String, back: String, hint: String) {
        viewModelScope.launch {
            repo.addCustomFlashcard(
                topicId = topic.id,
                partCode = topic.partCode,
                deckTitle = "Unit ${topic.unit} — ${topic.partTitle}",
                front = front,
                back = back,
                hint = hint
            )
            _feedbackBanner.value = "Custom SM-2 flashcard saved."
        }
    }

    // --- Adaptive MCQ & 3-Hour Mock Simulator ---

    fun startAdaptiveMcqTest(
        modeTitle: String,
        count: Int,
        topicFilterId: String? = null,
        partFilterCode: String? = null,
        weakOnly: Boolean = false,
        timedExamMode: Boolean = false,
        durationSeconds: Int = count * 60
    ) {
        val weakIds = analytics.value.weakest5.map { it.topic.id }.toSet()
        val chosen = AdaptiveMcqEngine.buildAdaptiveSelection(
            allMcqs = _bundle.value.mcqs,
            count = count,
            topicFilterId = topicFilterId,
            partFilterCode = partFilterCode,
            weakOnly = weakOnly,
            attemptsMap = questionAttemptsMap.value,
            topicProgressMap = topicProgressMap.value,
            weakTopicIds = weakIds
        )
        _mcqRunnerState.value = McqRunnerState(
            modeTitle = modeTitle,
            partCode = partFilterCode ?: "ALL",
            topicId = topicFilterId.orEmpty(),
            questions = chosen,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            flaggedIndices = emptySet(),
            isTimedMode = timedExamMode,
            isMockExam = false,
            totalDurationSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            elapsedSeconds = 0,
            isSubmitted = false
        )
        startMcqTimerIfNeeded(isTimed = timedExamMode, isMock = false)
    }

    fun startFullBookMockExam() {
        val all = _bundle.value.mcqs
        val weighted = buildList {
            // Mirror syllabus proportions: 35% A, 20% B, 25% C, 20% D
            addAll(all.filter { it.partCode == "PART_A" }.shuffled())
            addAll(all.filter { it.partCode == "PART_C" }.shuffled())
            addAll(all.filter { it.partCode == "PART_B" }.shuffled())
            addAll(all.filter { it.partCode == "PART_D" }.shuffled())
        }.distinctBy { it.mcq.id }

        val duration = 3 * 3600 // 3 hours
        val state = McqRunnerState(
            modeTitle = "Full Book Merit Mock Exam (3-Hour Simulator)",
            partCode = "ALL",
            topicId = "MOCK_FULL",
            questions = weighted,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            flaggedIndices = emptySet(),
            isTimedMode = true,
            isMockExam = true,
            totalDurationSeconds = duration,
            remainingSeconds = duration,
            elapsedSeconds = 0,
            isSubmitted = false
        )
        _mcqRunnerState.value = state
        persistMockSnapshot(state)
        startMcqTimerIfNeeded(isTimed = true, isMock = true)
    }

    fun resumeSavedMockExam() {
        val saved = activeMockState.value ?: return
        val ids = saved.questionIdsCsv.split(",").filter { it.isNotBlank() }
        val mcqLookup = _bundle.value.mcqs.associateBy { it.mcq.id }
        val restoredQuestions = ids.mapNotNull { mcqLookup[it] }.ifEmpty { _bundle.value.mcqs }
        val selected = mutableMapOf<Int, Int>()
        runCatching {
            val json = JSONObject(saved.selectedAnswersJson)
            json.keys().forEach { k ->
                val idx = k.toIntOrNull()
                if (idx != null) selected[idx] = json.getInt(k)
            }
        }
        val flagged = saved.flaggedIndicesCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        val state = McqRunnerState(
            modeTitle = saved.title,
            partCode = "ALL",
            topicId = "MOCK_FULL",
            questions = restoredQuestions,
            currentIndex = saved.currentIndex.coerceIn(0, (restoredQuestions.size - 1).coerceAtLeast(0)),
            selectedAnswers = selected,
            flaggedIndices = flagged,
            isTimedMode = true,
            isMockExam = true,
            totalDurationSeconds = 3 * 3600,
            remainingSeconds = saved.remainingSeconds.coerceAtLeast(60),
            elapsedSeconds = (3 * 3600 - saved.remainingSeconds).coerceAtLeast(0),
            isSubmitted = false
        )
        _mcqRunnerState.value = state
        startMcqTimerIfNeeded(isTimed = true, isMock = true)
    }

    private fun startMcqTimerIfNeeded(isTimed: Boolean, isMock: Boolean) {
        timerJob?.cancel()
        if (!isTimed) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val curr = _mcqRunnerState.value
                if (!curr.isActive || curr.isSubmitted) break
                val nextRem = (curr.remainingSeconds - 1).coerceAtLeast(0)
                val updated = curr.copy(
                    remainingSeconds = nextRem,
                    elapsedSeconds = curr.elapsedSeconds + 1
                )
                _mcqRunnerState.value = updated
                if (isMock && updated.elapsedSeconds % 10 == 0) {
                    persistMockSnapshot(updated)
                }
                if (nextRem == 0) {
                    submitCurrentMcqRunner()
                    break
                }
            }
        }
    }

    fun selectMcqOption(questionIdx: Int, optionIdx: Int) {
        val curr = _mcqRunnerState.value
        if (curr.isSubmitted) return
        val nextMap = curr.selectedAnswers + (questionIdx to optionIdx)
        val updated = curr.copy(selectedAnswers = nextMap)
        _mcqRunnerState.value = updated
        if (updated.isMockExam) persistMockSnapshot(updated)
    }

    fun jumpToMcqIndex(index: Int) {
        val curr = _mcqRunnerState.value
        if (curr.questions.isEmpty()) return
        val updated = curr.copy(currentIndex = index.coerceIn(0, curr.questions.lastIndex))
        _mcqRunnerState.value = updated
        if (updated.isMockExam) persistMockSnapshot(updated)
    }

    fun toggleFlagQuestion(index: Int) {
        val curr = _mcqRunnerState.value
        val nextFlags = if (index in curr.flaggedIndices) curr.flaggedIndices - index else curr.flaggedIndices + index
        val updated = curr.copy(flaggedIndices = nextFlags)
        _mcqRunnerState.value = updated
        if (updated.isMockExam) persistMockSnapshot(updated)
    }

    fun toggleShowUnansweredOnly() {
        val curr = _mcqRunnerState.value
        _mcqRunnerState.value = curr.copy(showUnansweredOnly = !curr.showUnansweredOnly)
    }

    fun submitCurrentMcqRunner() {
        val curr = _mcqRunnerState.value
        if (!curr.isActive || curr.isSubmitted) return
        timerJob?.cancel()

        viewModelScope.launch {
            val results = curr.questions.mapIndexed { idx, q ->
                val isRight = curr.selectedAnswers[idx] == q.mcq.correctIndex
                Triple(q.mcq.id, q.topicId, isRight)
            }
            val topicsLookup = _bundle.value.topics.associate { it.id to it.name }
            val savedAttempt = repo.recordTestAttempt(
                modeTitle = curr.modeTitle,
                partCode = curr.partCode,
                topicId = curr.topicId,
                perQuestionResults = results,
                durationSeconds = curr.elapsedSeconds.coerceAtLeast(15),
                isMockExam = curr.isMockExam,
                topicsLookup = topicsLookup
            )
            if (curr.isMockExam) {
                repo.clearActiveMockState()
            }
            _mcqRunnerState.value = curr.copy(
                isSubmitted = true,
                lastAttemptSummary = savedAttempt
            )
            _feedbackBanner.value =
                "${curr.modeTitle}: Scored ${savedAttempt.correctCount}/${savedAttempt.totalQuestions} (${savedAttempt.accuracyPercent}%)"
        }
    }

    fun exitMcqRunner() {
        timerJob?.cancel()
        _mcqRunnerState.value = McqRunnerState()
    }

    private fun persistMockSnapshot(state: McqRunnerState) {
        viewModelScope.launch {
            val json = JSONObject().apply {
                state.selectedAnswers.forEach { (k, v) -> put(k.toString(), v) }
            }
            repo.saveMockExamState(
                MockExamStateEntity(
                    title = state.modeTitle,
                    questionIdsCsv = state.questions.joinToString(",") { it.mcq.id },
                    selectedAnswersJson = json.toString(),
                    flaggedIndicesCsv = state.flaggedIndices.joinToString(","),
                    descriptiveQuestionIdsCsv = "",
                    descriptiveAnswersJson = "{}",
                    remainingSeconds = state.remainingSeconds,
                    currentIndex = state.currentIndex,
                    isActive = !state.isSubmitted
                )
            )
        }
    }

    // --- Descriptive Typing Practice ---

    fun evaluateDescriptiveAnswer(topicId: String, question: TypingQuestion, answerText: String) {
        viewModelScope.launch {
            val localEval = DescriptiveEvaluator.evaluate(question, answerText)
            repo.saveDescriptiveResult(question.id, topicId, question.question, answerText, localEval)
            _lastDescriptiveEval.value = localEval
            _feedbackBanner.value =
                "Estimated Practice Score: ${localEval.estimatedScore}/${localEval.maxMarks}"
        }
    }

    // --- Jarvis Teacher AI & Offline Mentor ---

    fun askJarvis(
        question: String,
        style: TutorStyleRequest = TutorStyleRequest.DEFAULT_BILINGUAL,
        topicOverride: SyllabusTopic? = null
    ) {
        val clean = question.trim()
        if (clean.isEmpty()) return

        val topic = topicOverride ?: _bundle.value.topics.firstOrNull { it.id == _selectedTopicId.value }
        _tutorMessages.value = _tutorMessages.value + JarvisChatMessage(
            isUser = true,
            text = clean,
            styleLabel = style.label
        )
        _tutorLoading.value = true

        viewModelScope.launch {
            val weakNames = analytics.value.weakest5.map { it.topic.name }
            val resp = aiManager.askJarvis(
                question = clean,
                style = style,
                settings = settings.value,
                currentTopic = topic,
                weakTopics = weakNames,
                readinessScore = analytics.value.readiness.readinessScore,
                isOnline = _isOnline.value
            )
            _tutorMessages.value = _tutorMessages.value + JarvisChatMessage(
                isUser = false,
                text = resp.text,
                styleLabel = style.label,
                providerLabel = resp.providerLabel,
                isOfflineFallback = resp.isOfflineFallback
            )
            _tutorLoading.value = false
        }
    }

    fun clearTutorChat() {
        _tutorMessages.value = listOf(
            JarvisChatMessage(
                isUser = false,
                text = "Fresh mentor session started for Saifullah! Select any topic or tap a coaching chip below.",
                styleLabel = "Mentor Ready",
                providerLabel = "Offline Jarvis Mode",
                isOfflineFallback = true
            )
        )
    }

    // --- Editable Schedule ---

    fun toggleScheduleSlotDone(slot: ScheduleSlotEntity) {
        viewModelScope.launch { repo.toggleScheduleSlotDone(slot) }
    }

    fun saveScheduleSlot(slot: ScheduleSlotEntity) {
        viewModelScope.launch {
            repo.updateScheduleSlot(slot)
            _feedbackBanner.value = "Updated schedule slot ${slot.startTime}–${slot.endTime}."
        }
    }

    // --- Encrypted API Keys, Backup/Restore, Notifications, Reset ---

    fun saveEncryptedApiKey(provider: AiProviderType, rawKey: String, customModel: String, customEndpoint: String) {
        viewModelScope.launch {
            aiManager.saveApiKey(provider, rawKey)
            repo.prefs.update {
                it.copy(
                    aiProviderType = provider,
                    aiModelName = customModel.ifBlank { provider.defaultModel },
                    aiCustomEndpoint = customEndpoint.trim()
                )
            }
            _feedbackBanner.value = "${provider.label} encrypted with Android Keystore AES-256."
        }
    }

    fun clearEncryptedApiKey(provider: AiProviderType) {
        viewModelScope.launch {
            aiManager.clearApiKey(provider)
            if (settings.value.aiProviderType == provider) {
                repo.prefs.update {
                    it.copy(
                        aiProviderType = AiProviderType.OFFLINE_ONLY,
                        aiModelName = AiProviderType.OFFLINE_ONLY.defaultModel
                    )
                }
            }
            _feedbackBanner.value = "Removed API key for ${provider.label}."
        }
    }

    fun getMaskedApiKey(provider: AiProviderType): String = aiManager.getMaskedApiKey(provider)

    suspend fun exportBackupJson(): String = repo.exportBackupJson(settings.value)

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            val res = repo.importBackupJson(rawJson)
            _feedbackBanner.value = res.getOrElse { it.message ?: "Import failed." }
        }
    }

    fun triggerTestReminder(type: StudyReminderType) {
        ReminderScheduler.triggerTestNotificationNow(appContext, type)
        _feedbackBanner.value = "Scheduled test notification (${type.title}) in 2 seconds."
    }

    fun resetAllProgressConfirmed() {
        viewModelScope.launch {
            repo.resetAllProgress(_bundle.value)
            val audit = videoManager.seedAndAuditCatalog(_bundle.value, performRemoteCheck = false)
            _catalogAudit.value = audit
            _feedbackBanner.value = "All OM Jarvis study progress reset cleanly."
        }
    }

    // --- Global Instant Search ---

    fun searchAll(query: String): GlobalSearchResults {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return GlobalSearchResults()
        val b = _bundle.value
        val tp = topicProgressMap.value
        val topicLookup = b.topics.associateBy { it.id }

        val topics = b.topics.filter {
            it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.romanUrduSummary.lowercase().contains(q) ||
                it.formulae.lowercase().contains(q) ||
                "unit ${it.unit}".contains(q)
        }
        val vids = videos.value.filter {
            it.title.lowercase().contains(q) || it.filename.lowercase().contains(q)
        }
        val fcs = flashcards.value.filter {
            it.front.lowercase().contains(q) || it.back.lowercase().contains(q) || it.deckTitle.lowercase().contains(q)
        }
        val mcqs = b.mcqs.filter {
            it.mcq.question.lowercase().contains(q) || it.mcq.explanation.lowercase().contains(q)
        }
        val notes = tp.values.mapNotNull { p ->
            if (p.personalNotes.lowercase().contains(q) || p.highlightedSnippets.lowercase().contains(q)) {
                topicLookup[p.topicId]?.let { t -> t to p }
            } else null
        }
        val pastPapers = b.pastPapers.filter {
            it.typingQuestion.question.lowercase().contains(q) ||
                it.typingQuestion.topicTag.lowercase().contains(q) ||
                it.typingQuestion.modelAnswer.lowercase().contains(q)
        }
        return GlobalSearchResults(
            topics = topics,
            videos = vids,
            flashcards = fcs,
            mcqs = mcqs,
            notesMatches = notes,
            pastPapers = pastPapers
        )
    }

    fun motivationalMessage(): String? {
        val snap = analytics.value
        return when (settings.value.motivationFrequency) {
            MotivationFrequency.DISABLE -> null
            MotivationFrequency.LOW -> "Stay consistent — ${snap.daysRemaining} days to Dec 7 Merit Exam."
            MotivationFrequency.NORMAL ->
                "Assalam-o-Alaikum Saifullah! Readiness ${snap.readiness.readinessScore}/100 • ${snap.streakDays}d streak. Complete ${snap.revisionNowTop5.firstOrNull()?.topic?.name ?: "today's mission"}!"
            MotivationFrequency.HIGH ->
                "Merit Push: ${snap.topicsCompleted}/${snap.topicsCompleted + snap.topicsRemaining} topics mastered • ${snap.overallMcqAccuracy}% MCQ accuracy. Push past 80% in every part!"
        }
    }
}
