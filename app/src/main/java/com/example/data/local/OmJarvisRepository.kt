package com.example.data.local

import android.content.Context
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.UserPreferencesDataStore
import com.example.data.syllabus.OmSyllabusCatalog
import com.example.data.syllabus.SyllabusDatabaseBundle
import com.example.domain.DescriptiveEvaluationResult
import com.example.domain.JarvisReadinessEngine
import com.example.domain.SmartSpacedRepetition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max

class OmJarvisRepository(
    context: Context,
    private val db: OmJarvisDatabase = OmJarvisDatabase.getInstance(context),
    val prefs: UserPreferencesDataStore = UserPreferencesDataStore(context)
) {
    private val appContext = context.applicationContext

    fun observeTopicProgress(): Flow<List<TopicProgressEntity>> = db.topicProgressDao().observeAll()
    fun observeVideos(): Flow<List<VideoProgressEntity>> = db.videoProgressDao().observeAll()
    fun observeFlashcards(): Flow<List<FlashcardEntity>> = db.flashcardDao().observeAll()
    fun observeQuestionAttempts(): Flow<List<QuestionAttemptEntity>> = db.questionAttemptDao().observeAll()
    fun observeTestAttempts(): Flow<List<TestAttemptEntity>> = db.testAttemptDao().observeAll()
    fun observeActiveMock(): Flow<MockExamStateEntity?> = db.mockExamStateDao().observeActive()
    fun observeDescriptive(): Flow<List<DescriptiveSubmissionEntity>> = db.descriptiveSubmissionDao().observeAll()
    fun observeStudySessions(): Flow<List<StudySessionEntity>> = db.studySessionDao().observeAll()
    fun observeScheduleSlots(): Flow<List<ScheduleSlotEntity>> = db.scheduleSlotDao().observeAll()

    suspend fun initializeOfflineDatabase(bundle: SyllabusDatabaseBundle) = withContext(Dispatchers.IO) {
        val initialTopicRows = bundle.topics.map { t ->
            TopicProgressEntity(
                topicId = t.id,
                partCode = t.partCode,
                unitNumber = t.unit
            )
        }
        db.topicProgressDao().insertAllIgnore(initialTopicRows)
        db.flashcardDao().insertAllIgnore(bundle.starterFlashcards)

        if (db.scheduleSlotDao().getAllOnce().isEmpty()) {
            db.scheduleSlotDao().insertAllIgnore(defaultRealisticSchedule())
        }
    }

    private fun defaultRealisticSchedule(): List<ScheduleSlotEntity> = listOf(
        ScheduleSlotEntity("slot_0800", "08:00", "08:30", "Morning Mission & Topic Launch", "Read core unit notes + watch lecture clip before office.", "MORNING", "om_u14_bsc", 30, "", 1),
        ScheduleSlotEntity("slot_0900", "09:00", "12:30", "Office Focus Hours (Light Recall)", "Primary office work; keep 2 mental formula hooks in mind.", "OFFICE", "", 0, "", 2),
        ScheduleSlotEntity("slot_1245", "12:45", "13:15", "Lunch Break 1: Office Quick Test", "Solve 10 Adaptive MCQs targeting your weakest area.", "LUNCH_TEST", "om_u02_mintzberg", 30, "", 3),
        ScheduleSlotEntity("slot_1315", "13:15", "17:00", "Afternoon Office Block + Micro-Drill", "At 15:30 tea break scan Past Paper headings.", "OFFICE", "", 15, "", 4),
        ScheduleSlotEntity("slot_1730", "17:30", "19:00", "Home Transition & Tired Flashcards", "Flip due SM-2 Flashcards gently without heavy screen strain.", "FLASHCARDS", "om_u05_mrp_erp", 25, "", 5),
        ScheduleSlotEntity("slot_1900", "19:00", "20:00", "Evening Deep Study + Video Lesson", "Fresh-mind study block with Media3 video & personal notes.", "EVENING_STUDY", "om_u04_oee", 60, "", 6),
        ScheduleSlotEntity("slot_2015", "20:15", "21:30", "Night Test (60% MCQ / 40% Typing)", "Timed 20-MCQ test + 1 Past Paper 5-mark typed answer.", "NIGHT_TEST", "om_u14_bsc", 50, "", 7),
        ScheduleSlotEntity("slot_2130", "21:30", "22:00", "Quick Revision & Call Jarvis Check-In", "Oral viva check-in with OM Jarvis before rest.", "REVISION", "om_u07_coq", 20, "", 8)
    )

    suspend fun recordReadingProgress(
        topicId: String,
        partCode: String,
        unitNumber: Int,
        percent: Int,
        paragraphIndex: Int,
        markCompleted: Boolean? = null
    ) {
        val current = db.topicProgressDao().getById(topicId)
            ?: TopicProgressEntity(topicId = topicId, partCode = partCode, unitNumber = unitNumber)
        val completed = markCompleted ?: (current.readingCompleted || percent >= 90)
        db.topicProgressDao().upsert(
            current.copy(
                readingStarted = true,
                readingCompleted = completed,
                readingProgressPercent = max(current.readingProgressPercent, percent).coerceIn(0, 100),
                lastReadParagraph = paragraphIndex,
                lastStudiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveTopicNotesAndFlags(
        topicId: String,
        partCode: String,
        unitNumber: Int,
        notes: String? = null,
        highlightSnippet: String? = null,
        bookmarked: Boolean? = null,
        important: Boolean? = null,
        revision: Boolean? = null
    ) {
        val current = db.topicProgressDao().getById(topicId)
            ?: TopicProgressEntity(topicId = topicId, partCode = partCode, unitNumber = unitNumber)
        val nextNotes = notes ?: current.personalNotes
        val nextHighlights = if (highlightSnippet != null) {
            listOf(current.highlightedSnippets, highlightSnippet).filter { it.isNotBlank() }.joinToString("\n• ")
        } else {
            current.highlightedSnippets
        }
        db.topicProgressDao().upsert(
            current.copy(
                personalNotes = nextNotes.take(6000),
                notesCreated = nextNotes.isNotBlank() || current.notesCreated,
                highlightedSnippets = nextHighlights.take(3000),
                bookmarked = bookmarked ?: current.bookmarked,
                flaggedImportant = important ?: current.flaggedImportant,
                flaggedRevision = revision ?: current.flaggedRevision,
                lastStudiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateVideoPlaybackProgress(
        videoId: String,
        positionMs: Long,
        durationMs: Long,
        autoCompleteThresholdPct: Int,
        explicitComplete: Boolean? = null
    ) {
        val vid = db.videoProgressDao().getById(videoId) ?: return
        val pct = if (durationMs > 0L) ((positionMs * 100L) / durationMs).toInt().coerceIn(0, 100) else vid.watchedPercent
        val isDone = explicitComplete ?: (vid.completed || pct >= autoCompleteThresholdPct)
        val finalPct = if (isDone) 100 else max(vid.watchedPercent, pct)

        db.videoProgressDao().upsert(
            vid.copy(
                lastPositionMs = positionMs,
                durationMs = max(vid.durationMs, durationMs),
                watchedPercent = finalPct,
                completed = isDone,
                updatedAt = System.currentTimeMillis()
            )
        )

        val tp = db.topicProgressDao().getById(vid.topicId)
            ?: TopicProgressEntity(vid.topicId, vid.partCode, vid.unitNumber)
        db.topicProgressDao().upsert(
            tp.copy(
                videoStarted = true,
                videoCompleted = tp.videoCompleted || isDone,
                lastStudiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun reviewFlashcard(card: FlashcardEntity, rating: SpacedRepRating) {
        val updated = SmartSpacedRepetition.scheduleReview(card, rating)
        db.flashcardDao().upsert(updated)

        val tp = db.topicProgressDao().getById(card.topicId)
        if (tp != null) {
            db.topicProgressDao().upsert(
                tp.copy(
                    flashcardsReviewed = tp.flashcardsReviewed + 1,
                    lastStudiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addCustomFlashcard(
        topicId: String,
        partCode: String,
        deckTitle: String,
        front: String,
        back: String,
        hint: String
    ) {
        db.flashcardDao().upsert(
            FlashcardEntity(
                id = "custom_fc_${UUID.randomUUID().toString().take(10)}",
                topicId = topicId,
                partCode = partCode,
                deckTitle = deckTitle,
                front = front.trim(),
                back = back.trim(),
                hint = hint.trim(),
                isCustom = true
            )
        )
    }

    suspend fun recordTestAttempt(
        modeTitle: String,
        partCode: String,
        topicId: String,
        perQuestionResults: List<Triple<String, String, Boolean>>,
        durationSeconds: Int,
        isMockExam: Boolean,
        topicsLookup: Map<String, String>
    ): TestAttemptEntity {
        val now = System.currentTimeMillis()
        for ((qId, qTopicId, isRight) in perQuestionResults) {
            val prev = db.questionAttemptDao().getById(qId)
                ?: QuestionAttemptEntity(questionId = qId, topicId = qTopicId, partCode = partCode)
            val attempts = prev.attemptsCount + 1
            val right = prev.correctCount + if (isRight) 1 else 0
            val wrong = prev.wrongCount + if (!isRight) 1 else 0
            val conf = (right.toFloat() / attempts.coerceAtLeast(1)).coerceIn(0f, 1f)
            db.questionAttemptDao().upsert(
                prev.copy(
                    attemptsCount = attempts,
                    correctCount = right,
                    wrongCount = wrong,
                    lastCorrectAt = if (isRight) now else prev.lastCorrectAt,
                    lastWrongAt = if (!isRight) now else prev.lastWrongAt,
                    confidenceScore = conf
                )
            )
        }

        // Update topic-level best MCQ accuracy
        val groupedByTopic = perQuestionResults.groupBy { it.second }
        val weakNames = mutableListOf<String>()
        val strongNames = mutableListOf<String>()
        for ((tId, qList) in groupedByTopic) {
            val right = qList.count { it.third }
            val acc = if (qList.isNotEmpty()) ((right * 100f) / qList.size).toInt() else 0
            val tp = db.topicProgressDao().getById(tId)
            if (tp != null) {
                val best = max(tp.bestMcqAccuracy, acc)
                db.topicProgressDao().upsert(
                    tp.copy(
                        mcqAttemptedCount = tp.mcqAttemptedCount + qList.size,
                        bestMcqAccuracy = best,
                        mcqPassed = tp.mcqPassed || acc >= 80,
                        lastStudiedAt = now
                    )
                )
            }
            val tName = topicsLookup[tId] ?: tId
            if (acc < 70) weakNames += tName else if (acc >= 80) strongNames += tName
        }

        val total = perQuestionResults.size.coerceAtLeast(1)
        val totalCorrect = perQuestionResults.count { it.third }
        val overallAcc = ((totalCorrect * 100f) / total).toInt()
        val attempt = TestAttemptEntity(
            id = "test_${UUID.randomUUID().toString().take(10)}",
            modeTitle = modeTitle,
            partCode = partCode,
            topicId = topicId,
            totalQuestions = total,
            correctCount = totalCorrect,
            accuracyPercent = overallAcc,
            durationSeconds = durationSeconds,
            weakTopicsCsv = weakNames.joinToString(", "),
            strongTopicsCsv = strongNames.joinToString(", "),
            isMockExam = isMockExam,
            createdAt = now
        )
        db.testAttemptDao().insert(attempt)
        logStudyMinutes(max(3, durationSeconds / 60), if (isMockExam) "MOCK_EXAM" else "MCQ_TEST", topicId)
        return attempt
    }

    suspend fun saveDescriptiveResult(
        questionId: String,
        topicId: String,
        prompt: String,
        answerText: String,
        result: DescriptiveEvaluationResult
    ) {
        val now = System.currentTimeMillis()
        db.descriptiveSubmissionDao().insert(
            DescriptiveSubmissionEntity(
                id = "desc_${UUID.randomUUID().toString().take(10)}",
                questionId = questionId,
                topicId = topicId,
                prompt = prompt,
                answerText = answerText,
                estimatedScore = result.estimatedScore,
                maxMarks = result.maxMarks,
                matchedKeywordsCsv = result.matchedKeywords.joinToString(", "),
                missingKeywordsCsv = result.missingKeywords.joinToString(", "),
                feedbackText = result.feedbackText,
                createdAt = now
            )
        )
        val tp = db.topicProgressDao().getById(topicId)
        if (tp != null) {
            val pct = if (result.maxMarks > 0) ((result.estimatedScore / result.maxMarks) * 100f).toInt() else 0
            db.topicProgressDao().upsert(
                tp.copy(
                    descriptivePracticed = tp.descriptivePracticed + 1,
                    bestDescriptivePercent = max(tp.bestDescriptivePercent, pct),
                    lastStudiedAt = now
                )
            )
        }
        logStudyMinutes(6, "DESCRIPTIVE", topicId)
    }

    suspend fun logStudyMinutes(minutes: Int, activityType: String, topicId: String = "") {
        db.studySessionDao().insert(
            StudySessionEntity(
                id = "sess_${UUID.randomUUID().toString().take(10)}",
                dateKey = JarvisReadinessEngine.todayDateKey(),
                durationMinutes = minutes.coerceIn(1, 360),
                activityType = activityType,
                topicId = topicId
            )
        )
    }

    suspend fun saveMockExamState(state: MockExamStateEntity) = db.mockExamStateDao().save(state)
    suspend fun clearActiveMockState() = db.mockExamStateDao().clear()

    suspend fun updateScheduleSlot(slot: ScheduleSlotEntity) = db.scheduleSlotDao().upsert(slot)
    suspend fun toggleScheduleSlotDone(slot: ScheduleSlotEntity) {
        val today = JarvisReadinessEngine.todayDateKey()
        val next = if (slot.completedDateKey == today) "" else today
        db.scheduleSlotDao().upsert(slot.copy(completedDateKey = next))
        if (next == today && slot.durationMinutes > 0) {
            logStudyMinutes(slot.durationMinutes, "ROUTINE_SLOT", slot.topicId)
        }
    }

    suspend fun exportBackupJson(settings: OmJarvisSettings): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("studentName", settings.studentName)
        root.put("dailyTargetMinutes", settings.dailyTargetMinutes)
        root.put("meritMode", settings.meritModeEnabled)

        val topicsArr = JSONArray()
        db.topicProgressDao().getAllOnce().forEach { tp ->
            topicsArr.put(JSONObject().apply {
                put("topicId", tp.topicId)
                put("partCode", tp.partCode)
                put("unitNumber", tp.unitNumber)
                put("readingCompleted", tp.readingCompleted)
                put("videoCompleted", tp.videoCompleted)
                put("personalNotes", tp.personalNotes)
                put("bookmarked", tp.bookmarked)
                put("bestMcqAccuracy", tp.bestMcqAccuracy)
                put("mcqAttemptedCount", tp.mcqAttemptedCount)
                put("flashcardsReviewed", tp.flashcardsReviewed)
                put("descriptivePracticed", tp.descriptivePracticed)
            })
        }
        root.put("topicProgress", topicsArr)

        val testsArr = JSONArray()
        db.testAttemptDao().getAllOnce().forEach { ta ->
            testsArr.put(JSONObject().apply {
                put("id", ta.id)
                put("modeTitle", ta.modeTitle)
                put("partCode", ta.partCode)
                put("topicId", ta.topicId)
                put("totalQuestions", ta.totalQuestions)
                put("correctCount", ta.correctCount)
                put("accuracyPercent", ta.accuracyPercent)
                put("durationSeconds", ta.durationSeconds)
                put("isMockExam", ta.isMockExam)
                put("createdAt", ta.createdAt)
            })
        }
        root.put("testAttempts", testsArr)

        val sessionsArr = JSONArray()
        db.studySessionDao().getAllOnce().forEach { ss ->
            sessionsArr.put(JSONObject().apply {
                put("id", ss.id)
                put("dateKey", ss.dateKey)
                put("durationMinutes", ss.durationMinutes)
                put("activityType", ss.activityType)
                put("topicId", ss.topicId)
                put("createdAt", ss.createdAt)
            })
        }
        root.put("studySessions", sessionsArr)
        root.toString(2)
    }

    suspend fun importBackupJson(rawJson: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(rawJson)
            var restoredCount = 0

            val topicsArr = root.optJSONArray("topicProgress")
            if (topicsArr != null) {
                for (i in 0 until topicsArr.length()) {
                    val o = topicsArr.getJSONObject(i)
                    val id = o.getString("topicId")
                    val existing = db.topicProgressDao().getById(id)
                        ?: TopicProgressEntity(id, o.optString("partCode", "PART_A"), o.optInt("unitNumber", 1))
                    db.topicProgressDao().upsert(
                        existing.copy(
                            readingCompleted = existing.readingCompleted || o.optBoolean("readingCompleted"),
                            videoCompleted = existing.videoCompleted || o.optBoolean("videoCompleted"),
                            personalNotes = o.optString("personalNotes").ifBlank { existing.personalNotes },
                            bookmarked = existing.bookmarked || o.optBoolean("bookmarked"),
                            bestMcqAccuracy = maxOf(existing.bestMcqAccuracy, o.optInt("bestMcqAccuracy")),
                            mcqAttemptedCount = maxOf(existing.mcqAttemptedCount, o.optInt("mcqAttemptedCount")),
                            flashcardsReviewed = maxOf(existing.flashcardsReviewed, o.optInt("flashcardsReviewed")),
                            descriptivePracticed = maxOf(existing.descriptivePracticed, o.optInt("descriptivePracticed"))
                        )
                    )
                    restoredCount++
                }
            }

            val testsArr = root.optJSONArray("testAttempts")
            if (testsArr != null) {
                val restoredTests = buildList {
                    for (i in 0 until testsArr.length()) {
                        val o = testsArr.getJSONObject(i)
                        add(
                            TestAttemptEntity(
                                id = o.getString("id"),
                                modeTitle = o.optString("modeTitle", "Imported Test"),
                                partCode = o.optString("partCode", "ALL"),
                                topicId = o.optString("topicId", ""),
                                totalQuestions = o.optInt("totalQuestions", 10),
                                correctCount = o.optInt("correctCount", 0),
                                accuracyPercent = o.optInt("accuracyPercent", 0),
                                durationSeconds = o.optInt("durationSeconds", 60),
                                isMockExam = o.optBoolean("isMockExam", false),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
                db.testAttemptDao().insertAll(restoredTests)
            }

            val sessArr = root.optJSONArray("studySessions")
            if (sessArr != null) {
                val restoredSessions = buildList {
                    for (i in 0 until sessArr.length()) {
                        val o = sessArr.getJSONObject(i)
                        add(
                            StudySessionEntity(
                                id = o.getString("id"),
                                dateKey = o.optString("dateKey", JarvisReadinessEngine.todayDateKey()),
                                durationMinutes = o.optInt("durationMinutes", 15),
                                activityType = o.optString("activityType", "IMPORTED"),
                                topicId = o.optString("topicId", ""),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
                db.studySessionDao().insertAll(restoredSessions)
            }

            Result.success("Restored $restoredCount topic records & history safely without deleting current progress.")
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid OM Jarvis backup JSON: ${e.message}"))
        }
    }

    suspend fun resetAllProgress(bundle: SyllabusDatabaseBundle) = withContext(Dispatchers.IO) {
        db.topicProgressDao().clearAll()
        db.flashcardDao().clearAll()
        db.questionAttemptDao().clearAll()
        db.testAttemptDao().clearAll()
        db.mockExamStateDao().clear()
        db.descriptiveSubmissionDao().clearAll()
        db.studySessionDao().clearAll()
        db.scheduleSlotDao().clearAll()
        initializeOfflineDatabase(bundle)
    }
}
