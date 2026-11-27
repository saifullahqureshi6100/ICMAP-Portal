package com.example.domain

import com.example.data.TypingQuestion
import com.example.data.local.DescriptiveSubmissionEntity
import com.example.data.local.FlashcardEntity
import com.example.data.local.QuestionAttemptEntity
import com.example.data.local.SpacedRepRating
import com.example.data.local.StudySessionEntity
import com.example.data.local.TestAttemptEntity
import com.example.data.local.TopicMasterStatus
import com.example.data.local.TopicProgressEntity
import com.example.data.local.VideoProgressEntity
import com.example.data.syllabus.SyllabusDatabaseBundle
import com.example.data.syllabus.SyllabusPartInfo
import com.example.data.syllabus.SyllabusTopic
import com.example.data.syllabus.TopicMappedMcq
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PartWeightedProgress(
    val part: SyllabusPartInfo,
    val completionPercent: Int,
    val masteredTopics: Int,
    val totalTopics: Int,
    val mcqAccuracyPercent: Int?,
    val videosCompleted: Int,
    val totalVideos: Int
)

data class RevisionPriorityItem(
    val topic: SyllabusTopic,
    val priorityScore: Int,
    val reason: String,
    val masteryPercent: Int
)

data class ReadinessBreakdown(
    val readinessScore: Int,
    val explanation: String,
    val syllabusPct: Int,
    val videoPct: Int,
    val mcqAccuracyPct: Int,
    val descriptivePct: Int,
    val mockPct: Int,
    val flashcardPct: Int,
    val weakCoveragePct: Int,
    val consistencyPct: Int,
    val meritStatusLabel: String
)

data class AnalyticsSnapshot(
    val daysRemaining: Int,
    val hoursRemaining: Int,
    val todayMinutes: Int,
    val weeklyMinutes: Int,
    val streakDays: Int,
    val topicsCompleted: Int,
    val topicsRemaining: Int,
    val videosCompleted: Int,
    val availableVideosCount: Int,
    val overallMcqAccuracy: Int,
    val descriptiveAveragePct: Int,
    val flashcardMasteryPct: Int,
    val mockAveragePct: Int?,
    val partProgress: List<PartWeightedProgress>,
    val readiness: ReadinessBreakdown,
    val revisionNowTop5: List<RevisionPriorityItem>,
    val weakest5: List<RevisionPriorityItem>,
    val strongest5: List<RevisionPriorityItem>,
    val neglectedTopics: List<SyllabusTopic>,
    val repeatedMistakeTopics: List<String>,
    val dailySeriesLast7Days: List<Pair<String, Int>>
)

object JarvisReadinessEngine {

    fun todayDateKey(timeMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeMillis))

    fun computeAnalytics(
        bundle: SyllabusDatabaseBundle,
        examDateMillis: Long,
        meritMode: Boolean,
        dailyTargetMinutes: Int,
        topicProgressMap: Map<String, TopicProgressEntity>,
        videos: List<VideoProgressEntity>,
        flashcards: List<FlashcardEntity>,
        testAttempts: List<TestAttemptEntity>,
        descriptiveSubmissions: List<DescriptiveSubmissionEntity>,
        questionAttempts: Map<String, QuestionAttemptEntity>,
        studySessions: List<StudySessionEntity>
    ): AnalyticsSnapshot {
        val now = System.currentTimeMillis()
        val diffMs = max(0L, examDateMillis - now)
        val daysRemaining = (diffMs / (24L * 3600L * 1000L)).toInt()
        val hoursRemaining = (diffMs / (3600L * 1000L)).toInt()

        val topics = bundle.topics
        val videoByTopic = videos.associateBy { it.topicId }
        val masteryThreshold = if (meritMode) 80 else 70

        // Per-topic progress & revision ranking
        val topicRevisions = topics.map { topic ->
            val prog = topicProgressMap[topic.id]
            val vid = videoByTopic[topic.id]
            val status = prog?.computeMasterStatus(masteryThreshold) ?: TopicMasterStatus.NOT_STARTED
            val pct = ((prog?.masteryProgressFraction(masteryThreshold) ?: 0f) * 100f).roundToInt()

            val failedQuestionsInTopic = questionAttempts.values.count {
                it.topicId == topic.id && it.wrongCount > it.correctCount
            }
            var priority = 0
            val reasons = mutableListOf<String>()
            if (prog != null && prog.mcqAttemptedCount > 0 && prog.bestMcqAccuracy < masteryThreshold) {
                priority += (masteryThreshold - prog.bestMcqAccuracy) * 2
                reasons += "MCQ ${prog.bestMcqAccuracy}%"
            }
            if (failedQuestionsInTopic > 0) {
                priority += failedQuestionsInTopic * 18
                reasons += "$failedQuestionsInTopic repeated misses"
            }
            if (prog == null || prog.lastStudiedAt == 0L) {
                priority += 30
                reasons += "Not reviewed"
            } else {
                val daysSince = ((now - prog.lastStudiedAt) / (24L * 3600L * 1000L)).toInt()
                if (daysSince >= 2) {
                    priority += min(30, daysSince * 8)
                    reasons += "Due (${daysSince}d ago)"
                }
            }
            if (topic.importance == "CRITICAL") {
                priority += 25
                reasons += "High-yield Q/Unit"
            }
            if (vid != null && vid.catalogAvailable && !vid.completed) {
                priority += 10
            }
            if (status == TopicMasterStatus.MASTERED) {
                priority = max(0, priority - 60)
            }

            RevisionPriorityItem(
                topic = topic,
                priorityScore = priority,
                reason = reasons.take(2).joinToString(" • ").ifBlank { "Keep sharp" },
                masteryPercent = pct
            )
        }

        val topicsCompleted = topics.count { t ->
            val p = topicProgressMap[t.id]
            val s = p?.computeMasterStatus(masteryThreshold)
            s == TopicMasterStatus.MASTERED || (p?.readingCompleted == true && p.bestMcqAccuracy >= masteryThreshold)
        }

        // Part-level weighted progress
        val partProgress = bundle.parts.map { part ->
            val partTopics = topics.filter { it.partCode == part.code }
            val avgTopicPct = if (partTopics.isNotEmpty()) {
                partTopics.map { t ->
                    (topicProgressMap[t.id]?.masteryProgressFraction(masteryThreshold) ?: 0f) * 100f
                }.average().roundToInt()
            } else 0
            val masteredCount = partTopics.count { t ->
                topicProgressMap[t.id]?.computeMasterStatus(masteryThreshold) == TopicMasterStatus.MASTERED
            }
            val partVids = videos.filter { it.partCode == part.code }
            val partVidsDone = partVids.count { it.completed }
            val partTests = testAttempts.filter { it.partCode == part.code || it.partCode == "ALL" }
            val partMcqAvg = if (partTests.isNotEmpty()) {
                partTests.map { it.accuracyPercent }.average().roundToInt()
            } else null

            PartWeightedProgress(
                part = part,
                completionPercent = avgTopicPct,
                masteredTopics = masteredCount,
                totalTopics = partTopics.size,
                mcqAccuracyPercent = partMcqAvg,
                videosCompleted = partVidsDone,
                totalVideos = partVids.size
            )
        }

        val syllabusWeightedPct = if (partProgress.sumOf { it.part.weightPercent } > 0) {
            val totalWeight = partProgress.sumOf { it.part.weightPercent }.toFloat()
            (partProgress.sumOf { it.completionPercent * it.part.weightPercent } / totalWeight).roundToInt()
        } else 0

        val availableVideos = videos.filter { it.catalogAvailable || it.hasLocalFile }
        val videosCompleted = videos.count { it.completed }
        val videoDenominator = if (availableVideos.isNotEmpty()) availableVideos.size else max(1, videos.size)
        val videoPct = ((videosCompleted * 100f) / videoDenominator).roundToInt().coerceIn(0, 100)

        val totalMcqQs = testAttempts.sumOf { it.totalQuestions }
        val totalMcqCorrect = testAttempts.sumOf { it.correctCount }
        val overallMcqAccuracy = if (totalMcqQs > 0) ((totalMcqCorrect * 100f) / totalMcqQs).roundToInt() else 0

        val descriptiveAvg = if (descriptiveSubmissions.isNotEmpty()) {
            descriptiveSubmissions.map {
                if (it.maxMarks > 0) ((it.estimatedScore / it.maxMarks) * 100f) else 0f
            }.average().roundToInt().coerceIn(0, 100)
        } else 0

        val mockAttempts = testAttempts.filter { it.isMockExam }
        val mockAverage = if (mockAttempts.isNotEmpty()) {
            mockAttempts.map { it.accuracyPercent }.average().roundToInt()
        } else null

        val flashcardMasteryPct = if (flashcards.isNotEmpty()) {
            val scoreSum = flashcards.sumOf {
                when {
                    it.mastered -> 1.0
                    it.reviewCount > 0 -> 0.5
                    else -> 0.0
                }
            }
            ((scoreSum / flashcards.size) * 100.0).roundToInt().coerceIn(0, 100)
        } else 0

        // Study consistency & streak from real StudySessionEntity
        val minutesByDay = studySessions.groupBy { it.dateKey }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
        val todayKey = todayDateKey(now)
        val todayMinutes = minutesByDay[todayKey] ?: 0
        val last7Days = buildLast7Days(minutesByDay, now)
        val weeklyMinutes = last7Days.sumOf { it.second }
        val activeDays = last7Days.count { it.second >= 15 }
        val consistencyPct = ((activeDays / 7f) * 100f).roundToInt().coerceIn(0, 100)
        val streakDays = computeStreak(minutesByDay, now)

        val weakTopicsList = topicRevisions.filter { it.masteryPercent < masteryThreshold }
        val weakCoveragePct = if (topics.isNotEmpty()) {
            val touchedWeak = weakTopicsList.count { (topicProgressMap[it.topic.id]?.mcqAttemptedCount ?: 0) > 0 }
            if (weakTopicsList.isEmpty()) 100 else ((touchedWeak * 100f) / weakTopicsList.size).roundToInt()
        } else 0

        // Transparent weighted formula for OM Jarvis Readiness Score (never fake)
        val readinessScore = (
            syllabusWeightedPct * 0.22f +
                videoPct * 0.12f +
                overallMcqAccuracy * 0.20f +
                descriptiveAvg * 0.14f +
                (mockAverage ?: overallMcqAccuracy) * 0.12f +
                flashcardMasteryPct * 0.10f +
                weakCoveragePct * 0.05f +
                consistencyPct * 0.05f
            ).roundToInt().coerceIn(0, 100)

        val meritStatusLabel = when {
            readinessScore >= 85 -> "Certificate of Merit Zone (Practice Indicator)"
            readinessScore >= 70 -> "Strong Pass Trajectory"
            readinessScore >= 40 -> "Building Core Coverage"
            else -> "Launch Phase — Follow Daily Plan"
        }

        val explanation = "Calculated from real stored activity: Weighted Syllabus $syllabusWeightedPct% (22%) + " +
            "MCQ Accuracy $overallMcqAccuracy% (20%) + Descriptive $descriptiveAvg% (14%) + " +
            "Videos $videoPct% (12%) + Mocks ${mockAverage ?: overallMcqAccuracy}% (12%) + " +
            "Flashcards $flashcardMasteryPct% (10%) + Weak Coverage $weakCoveragePct% (5%) + " +
            "Consistency $consistencyPct% (5%). Practice metric only, not an official ICMA prediction."

        val revisionNowTop5 = topicRevisions.sortedByDescending { it.priorityScore }.take(5)
        val weakest5 = topicRevisions.sortedWith(compareBy<RevisionPriorityItem> { it.masteryPercent }.thenByDescending { it.priorityScore }).take(5)
        val strongest5 = topicRevisions.filter { it.masteryPercent > 0 }.sortedByDescending { it.masteryPercent }.take(5)
        val neglectedTopics = topics.filter { (topicProgressMap[it.id]?.lastStudiedAt ?: 0L) == 0L }.take(5)
        val repeatedMistakes = questionAttempts.values
            .filter { it.wrongCount >= 2 }
            .sortedByDescending { it.wrongCount }
            .mapNotNull { qa -> topics.firstOrNull { it.id == qa.topicId }?.name }
            .distinct()
            .take(5)

        return AnalyticsSnapshot(
            daysRemaining = daysRemaining,
            hoursRemaining = hoursRemaining,
            todayMinutes = todayMinutes,
            weeklyMinutes = weeklyMinutes,
            streakDays = streakDays,
            topicsCompleted = topicsCompleted,
            topicsRemaining = max(0, topics.size - topicsCompleted),
            videosCompleted = videosCompleted,
            availableVideosCount = availableVideos.size,
            overallMcqAccuracy = overallMcqAccuracy,
            descriptiveAveragePct = descriptiveAvg,
            flashcardMasteryPct = flashcardMasteryPct,
            mockAveragePct = mockAverage,
            partProgress = partProgress,
            readiness = ReadinessBreakdown(
                readinessScore = readinessScore,
                explanation = explanation,
                syllabusPct = syllabusWeightedPct,
                videoPct = videoPct,
                mcqAccuracyPct = overallMcqAccuracy,
                descriptivePct = descriptiveAvg,
                mockPct = mockAverage ?: 0,
                flashcardPct = flashcardMasteryPct,
                weakCoveragePct = weakCoveragePct,
                consistencyPct = consistencyPct,
                meritStatusLabel = meritStatusLabel
            ),
            revisionNowTop5 = revisionNowTop5,
            weakest5 = weakest5,
            strongest5 = strongest5,
            neglectedTopics = neglectedTopics,
            repeatedMistakeTopics = repeatedMistakes,
            dailySeriesLast7Days = last7Days
        )
    }

    private fun buildLast7Days(minutesByDay: Map<String, Int>, now: Long): List<Pair<String, Int>> {
        val labelFmt = SimpleDateFormat("EEE", Locale.US)
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.add(Calendar.DAY_OF_YEAR, -6)
        return (0 until 7).map {
            val d = cal.time
            val label = labelFmt.format(d)
            val key = keyFmt.format(d)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            label to (minutesByDay[key] ?: 0)
        }
    }

    private fun computeStreak(minutesByDay: Map<String, Int>, now: Long): Int {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        var streak = 0
        for (i in 0 until 30) {
            val key = keyFmt.format(cal.time)
            val mins = minutesByDay[key] ?: 0
            if (mins > 0) {
                streak++
            } else if (i > 0) {
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}

object SmartSpacedRepetition {

    fun scheduleReview(card: FlashcardEntity, rating: SpacedRepRating, now: Long = System.currentTimeMillis()): FlashcardEntity {
        val dayMs = 24L * 3600L * 1000L
        var ease = card.easeFactor
        var reps = card.repetitions
        var interval = card.intervalDays
        var lapses = card.lapses

        when (rating) {
            SpacedRepRating.AGAIN -> {
                reps = 0
                interval = 0
                lapses += 1
                ease = (ease - 0.20f).coerceAtLeast(1.30f)
            }
            SpacedRepRating.HARD -> {
                reps = max(1, reps)
                interval = if (interval <= 1) 1 else (interval * 1.2f).roundToInt()
                ease = (ease - 0.14f).coerceAtLeast(1.30f)
            }
            SpacedRepRating.GOOD -> {
                interval = when (reps) {
                    0 -> 1
                    1 -> 3
                    else -> (interval * ease).roundToInt().coerceAtLeast(reps + 1)
                }
                reps += 1
            }
            SpacedRepRating.EASY -> {
                interval = when (reps) {
                    0 -> 3
                    1 -> 6
                    else -> (interval * ease * 1.3f).roundToInt().coerceAtLeast(4)
                }
                reps += 1
                ease = (ease + 0.15f).coerceAtMost(2.80f)
            }
        }

        val nextDueAt = when (rating) {
            SpacedRepRating.AGAIN -> now + 10L * 60L * 1000L // 10 minutes
            else -> now + interval * dayMs
        }

        return card.copy(
            easeFactor = ease,
            intervalDays = interval,
            repetitions = reps,
            lapses = lapses,
            lastRating = rating.name,
            nextReviewAt = nextDueAt,
            reviewCount = card.reviewCount + 1,
            lastReviewedAt = now,
            mastered = reps >= 2 && rating != SpacedRepRating.AGAIN
        )
    }

    fun prioritizeDeck(
        cards: List<FlashcardEntity>,
        weakTopicIds: Set<String>,
        now: Long = System.currentTimeMillis()
    ): List<FlashcardEntity> = cards.sortedWith(
        compareByDescending<FlashcardEntity> { it.lastRating == SpacedRepRating.AGAIN.name }
            .thenByDescending { it.topicId in weakTopicIds }
            .thenByDescending { it.lapses }
            .thenBy { it.nextReviewAt <= now }
            .thenBy { it.reviewCount > 0 }
            .thenBy { it.nextReviewAt }
    )
}

object AdaptiveMcqEngine {

    fun buildAdaptiveSelection(
        allMcqs: List<TopicMappedMcq>,
        count: Int,
        topicFilterId: String? = null,
        partFilterCode: String? = null,
        weakOnly: Boolean = false,
        attemptsMap: Map<String, QuestionAttemptEntity> = emptyMap(),
        topicProgressMap: Map<String, TopicProgressEntity> = emptyMap(),
        weakTopicIds: Set<String> = emptySet()
    ): List<TopicMappedMcq> {
        val base = allMcqs.filter { q ->
            (topicFilterId == null || q.topicId == topicFilterId) &&
                (partFilterCode == null || q.partCode == partFilterCode) &&
                (!weakOnly || q.topicId in weakTopicIds || (attemptsMap[q.mcq.id]?.wrongCount ?: 0) > 0)
        }.ifEmpty { allMcqs }

        val scored = base.sortedByDescending { q ->
            val qa = attemptsMap[q.mcq.id]
            val tp = topicProgressMap[q.topicId]
            var weight = 50
            if (q.topicId in weakTopicIds) weight += 40
            if (qa != null && qa.wrongCount > qa.correctCount) weight += 55
            if (qa == null || qa.attemptsCount == 0) weight += 30
            if (tp != null && tp.bestMcqAccuracy >= 85 && (qa?.correctCount ?: 0) >= 2) weight -= 35
            weight
        }
        return scored.take(count.coerceAtMost(scored.size))
    }
}

data class DescriptiveEvaluationResult(
    val estimatedScore: Float,
    val maxMarks: Int,
    val matchedKeywords: List<String>,
    val missingKeywords: List<String>,
    val structureFeedback: String,
    val feedbackText: String
)

object DescriptiveEvaluator {

    fun evaluate(question: TypingQuestion, userAnswer: String): DescriptiveEvaluationResult {
        val clean = userAnswer.trim()
        val lower = clean.lowercase()
        val matched = question.keywords.filter { kw -> lower.contains(kw.lowercase()) }
        val missing = question.keywords.filterNot { kw -> lower.contains(kw.lowercase()) }

        val keywordRatio = if (question.keywords.isNotEmpty()) {
            matched.size.toFloat() / question.keywords.size
        } else 0.5f
        val lineCount = clean.lines().count { it.isNotBlank() }
        val wordCount = clean.split(Regex("\\s+")).count { it.isNotBlank() }
        val structureScore = when {
            lineCount >= 4 && wordCount >= 45 -> 1.0f
            lineCount >= 2 && wordCount >= 25 -> 0.7f
            wordCount >= 10 -> 0.4f
            else -> 0.1f
        }

        val rawMarks = ((keywordRatio * 0.75f + structureScore * 0.25f) * question.marks)
            .coerceIn(0f, question.marks.toFloat())
        val roundedMarks = (rawMarks * 2f).roundToInt() / 2f

        val structureNote = if (lineCount >= 4) {
            "Good structured headings/points ($lineCount sections, $wordCount words)."
        } else {
            "Use 4–5 numbered points/headings (currently $lineCount lines, $wordCount words) for faster ICMA marking."
        }

        val feedback = buildString {
            appendLine("Estimated Practice Score (Heuristic Rubric): $roundedMarks / ${question.marks}")
            appendLine("(Practice feedback only — not an official ICMA examiner grade.)")
            appendLine()
            appendLine("Structure: $structureNote")
            appendLine("Matched Key Concepts (${matched.size}/${question.keywords.size}): ${matched.joinToString(", ").ifBlank { "None yet" }}")
            if (missing.isNotEmpty()) {
                appendLine("Missing Key Points to Add: ${missing.joinToString(", ")}")
            }
        }.trim()

        return DescriptiveEvaluationResult(
            estimatedScore = roundedMarks,
            maxMarks = question.marks,
            matchedKeywords = matched,
            missingKeywords = missing,
            structureFeedback = structureNote,
            feedbackText = feedback
        )
    }
}
