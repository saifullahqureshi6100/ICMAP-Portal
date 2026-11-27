package com.example

import com.example.data.OmTypingQuestions
import com.example.data.local.DescriptiveSubmissionEntity
import com.example.data.local.QuestionAttemptEntity
import com.example.data.local.SpacedRepRating
import com.example.data.local.StudySessionEntity
import com.example.data.local.TestAttemptEntity
import com.example.data.local.TopicMasterStatus
import com.example.data.local.TopicProgressEntity
import com.example.data.preferences.OmJarvisSettings
import com.example.data.syllabus.OmSyllabusCatalog
import com.example.data.syllabus.SyllabusDatabaseBundle
import com.example.data.syllabus.SyllabusPartInfo
import com.example.data.syllabus.SyllabusTopic
import com.example.domain.AdaptiveMcqEngine
import com.example.domain.DescriptiveEvaluator
import com.example.domain.JarvisReadinessEngine
import com.example.domain.SmartSpacedRepetition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OmJarvisEnginesTest {

    private fun testBundle(): SyllabusDatabaseBundle {
        // Leverage fallback defaults for MCQs, Past Papers, Flashcards + synthetic weighted topics across A-D
        val fallback = OmSyllabusCatalog.parseFromJson("")
        val topics = listOf(
            SyllabusTopic("om_u02_mintzberg", "PART_A", "Operations Management", 35, 2, "Mintzberg 5 Parts", "", OmSyllabusCatalog.DEFAULT_VIDEO_BASE_URL, 19, "CRITICAL", "MEDIUM", "5 Parts", "Mintzberg structure", listOf("Operating Core", "Strategic Apex"), "Roman Urdu", "PTCL"),
            SyllabusTopic("om_u08_herzberg", "PART_B", "Human Resource Management", 20, 8, "Herzberg Two-Factor", "u08_02_herzberg.mp4", OmSyllabusCatalog.DEFAULT_VIDEO_BASE_URL, 118, "HIGH", "MEDIUM", "Hygiene vs Motivators", "Motivation", listOf("Hygiene", "Motivators"), "Roman Urdu", "Engro"),
            SyllabusTopic("om_u10_orientations", "PART_C", "Marketing Management", 25, 10, "Marketing Concept & STP", "u10_01_marketing_concepts.mp4", OmSyllabusCatalog.DEFAULT_VIDEO_BASE_URL, 150, "CRITICAL", "MEDIUM", "STP", "Marketing", listOf("Target Market"), "Roman Urdu", "Tapal"),
            SyllabusTopic("om_u14_bsc", "PART_D", "Performance Management", 20, 14, "Balanced Scorecard (Q.4)", "u14_01_balanced_scorecard.mp4", OmSyllabusCatalog.DEFAULT_VIDEO_BASE_URL, 230, "CRITICAL", "MEDIUM", "4 Perspectives", "BSC", listOf("Financial", "Customer"), "Roman Urdu", "HBL")
        )
        return fallback.copy(
            parts = listOf(
                SyllabusPartInfo("PART_A", "Operations Management", 35, "Units 1–7"),
                SyllabusPartInfo("PART_B", "Human Resource Management", 20, "Units 8–9"),
                SyllabusPartInfo("PART_C", "Marketing Management", 25, "Units 10–12"),
                SyllabusPartInfo("PART_D", "Performance Management", 20, "Units 13–14")
            ),
            topics = topics
        )
    }

    @Test
    fun `bundled syllabus json asset contains all 4 weighted parts and key topics`() {
        val candidates = listOf(
            File("app/src/main/assets/om_syllabus_config.json"),
            File("src/main/assets/om_syllabus_config.json")
        )
        val raw = candidates.first { it.exists() }.readText()
        assertTrue(raw.contains("\"code\": \"PART_A\""))
        assertTrue(raw.contains("\"weightPercent\": 35"))
        assertTrue(raw.contains("\"code\": \"PART_B\""))
        assertTrue(raw.contains("\"weightPercent\": 20"))
        assertTrue(raw.contains("\"code\": \"PART_C\""))
        assertTrue(raw.contains("\"weightPercent\": 25"))
        assertTrue(raw.contains("\"code\": \"PART_D\""))
        assertTrue(raw.contains("\"om_u14_bsc\""))
    }

    @Test
    fun `master syllabus fallback builds parts totaling 100 percent and official vs jarvis past papers`() {
        val bundle = testBundle()
        assertEquals(4, bundle.parts.size)
        assertEquals(100, bundle.parts.sumOf { it.weightPercent })
        assertTrue(bundle.mcqs.isNotEmpty())
        assertTrue(bundle.pastPapers.any { it.isOfficialPastPaper })
        assertTrue(bundle.pastPapers.any { !it.isOfficialPastPaper })
    }

    @Test
    fun `topic master status requires all configurable merit checks for MASTERED`() {
        val partial = TopicProgressEntity(
            topicId = "om_u14_bsc",
            partCode = "PART_D",
            unitNumber = 14,
            readingCompleted = true,
            videoCompleted = true,
            bestMcqAccuracy = 85,
            flashcardsReviewed = 0,
            descriptivePracticed = 0
        )
        assertEquals(TopicMasterStatus.MCQS_PASSED, partial.computeMasterStatus(80))

        val complete = partial.copy(
            flashcardsReviewed = 2,
            descriptivePracticed = 1
        )
        assertEquals(TopicMasterStatus.MASTERED, complete.computeMasterStatus(80))
    }

    @Test
    fun `jarvis readiness starts at zero and increases with real stored user activity`() {
        val bundle = testBundle()

        val emptySnap = JarvisReadinessEngine.computeAnalytics(
            bundle = bundle,
            examDateMillis = OmJarvisSettings.defaultDec7ExamMillis(),
            meritMode = true,
            dailyTargetMinutes = 180,
            topicProgressMap = emptyMap(),
            videos = emptyList(),
            flashcards = bundle.starterFlashcards,
            testAttempts = emptyList(),
            descriptiveSubmissions = emptyList(),
            questionAttempts = emptyMap(),
            studySessions = emptyList()
        )
        assertEquals(0, emptySnap.readiness.readinessScore)
        assertEquals(0, emptySnap.topicsCompleted)

        val t = bundle.topics.first()
        val progress = mapOf(
            t.id to TopicProgressEntity(
                topicId = t.id,
                partCode = t.partCode,
                unitNumber = t.unit,
                readingStarted = true,
                readingCompleted = true,
                readingProgressPercent = 100,
                mcqAttemptedCount = 2,
                bestMcqAccuracy = 100,
                mcqPassed = true,
                flashcardsReviewed = 2,
                descriptivePracticed = 1,
                lastStudiedAt = System.currentTimeMillis()
            )
        )
        val tests = listOf(
            TestAttemptEntity("t1", "Unit Drill", t.partCode, t.id, 2, 2, 100, 90)
        )
        val desc = listOf(
            DescriptiveSubmissionEntity(
                id = "d1",
                questionId = "typ_q4_bsc",
                topicId = t.id,
                prompt = OmTypingQuestions.items.first().question,
                answerText = "Sample",
                estimatedScore = 4.5f,
                maxMarks = 5,
                matchedKeywordsCsv = "qualitative, overload",
                missingKeywordsCsv = "",
                feedbackText = "Good"
            )
        )
        val sessions = listOf(
            StudySessionEntity("s1", JarvisReadinessEngine.todayDateKey(), 45, "READING", t.id)
        )

        val updatedSnap = JarvisReadinessEngine.computeAnalytics(
            bundle = bundle,
            examDateMillis = OmJarvisSettings.defaultDec7ExamMillis(),
            meritMode = true,
            dailyTargetMinutes = 180,
            topicProgressMap = progress,
            videos = emptyList(),
            flashcards = bundle.starterFlashcards,
            testAttempts = tests,
            descriptiveSubmissions = desc,
            questionAttempts = emptyMap(),
            studySessions = sessions
        )
        assertTrue(updatedSnap.readiness.readinessScore > 0)
        assertEquals(100, updatedSnap.overallMcqAccuracy)
        assertEquals(45, updatedSnap.todayMinutes)
        assertEquals(1, updatedSnap.topicsCompleted)
    }

    @Test
    fun `sm2 spaced repetition prioritizes missed cards and advances intervals`() {
        val bundle = testBundle()
        val c1 = bundle.starterFlashcards.first()
        val againCard = SmartSpacedRepetition.scheduleReview(c1, SpacedRepRating.AGAIN, now = 1_000_000L)
        assertEquals(0, againCard.repetitions)
        assertEquals(1, againCard.lapses)
        assertFalse(againCard.mastered)

        val good1 = SmartSpacedRepetition.scheduleReview(c1, SpacedRepRating.GOOD, now = 1_000_000L)
        val good2 = SmartSpacedRepetition.scheduleReview(good1, SpacedRepRating.GOOD, now = 2_000_000L)
        assertTrue(good2.mastered)
        assertTrue(good2.intervalDays >= 3)
    }

    @Test
    fun `adaptive mcq engine prioritizes missed questions and weak topics`() {
        val bundle = testBundle()
        val qWeak = bundle.mcqs.last()
        val attempts = mapOf(
            qWeak.mcq.id to QuestionAttemptEntity(qWeak.mcq.id, qWeak.topicId, qWeak.partCode, attemptsCount = 3, correctCount = 0, wrongCount = 3)
        )
        val selected = AdaptiveMcqEngine.buildAdaptiveSelection(
            allMcqs = bundle.mcqs,
            count = 5,
            attemptsMap = attempts,
            weakTopicIds = setOf(qWeak.topicId)
        )
        assertEquals(5, selected.size)
        assertEquals(qWeak.mcq.id, selected.first().mcq.id)
    }

    @Test
    fun `descriptive evaluator awards balanced score and detects missing concepts`() {
        val q = OmTypingQuestions.items.first()
        val weakResult = DescriptiveEvaluator.evaluate(q, "Too short.")
        assertTrue(weakResult.estimatedScore < 2.0f)
        assertTrue(weakResult.missingKeywords.isNotEmpty())

        val strongText = "1) Qualitative measurement difficulty.\n2) Information overload.\n3) Conflict across KPIs.\n4) Misaligned strategy.\n5) Senior management buy-in. " + "analysis ".repeat(40)
        val strongResult = DescriptiveEvaluator.evaluate(q, strongText)
        assertTrue(strongResult.estimatedScore >= 4.0f)
        assertTrue(strongResult.matchedKeywords.size >= 4)
    }
}
