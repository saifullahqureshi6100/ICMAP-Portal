package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TopicMasterStatus(val label: String, val stepOrder: Int) {
    NOT_STARTED("Not Started", 0),
    READING_STARTED("Reading Started", 1),
    READING_COMPLETE("Reading Complete", 2),
    VIDEO_STARTED("Video Started", 3),
    VIDEO_COMPLETE("Video Complete", 4),
    NOTES_CREATED("Notes Created", 5),
    MCQS_ATTEMPTED("MCQs Attempted", 6),
    MCQS_PASSED("MCQs Passed", 7),
    FLASHCARDS_REVIEWED("Flashcards Reviewed", 8),
    DESCRIPTIVE_PRACTICED("Descriptive Practiced", 9),
    MASTERED("MASTERED", 10)
}

enum class DownloadStatus(val label: String) {
    NOT_DOWNLOADED("Not Downloaded"),
    DOWNLOADING("Downloading"),
    PAUSED("Paused"),
    DOWNLOADED("Offline Ready"),
    FAILED("Failed")
}

enum class SpacedRepRating {
    AGAIN, HARD, GOOD, EASY
}

@Entity(tableName = "topic_progress")
data class TopicProgressEntity(
    @PrimaryKey val topicId: String,
    val partCode: String,
    val unitNumber: Int,
    val readingStarted: Boolean = false,
    val readingCompleted: Boolean = false,
    val readingProgressPercent: Int = 0,
    val lastReadParagraph: Int = 0,
    val videoStarted: Boolean = false,
    val videoCompleted: Boolean = false,
    val notesCreated: Boolean = false,
    val personalNotes: String = "",
    val highlightedSnippets: String = "",
    val bookmarked: Boolean = false,
    val flaggedImportant: Boolean = false,
    val flaggedRevision: Boolean = false,
    val mcqAttemptedCount: Int = 0,
    val bestMcqAccuracy: Int = 0,
    val mcqPassed: Boolean = false,
    val flashcardsReviewed: Int = 0,
    val descriptivePracticed: Int = 0,
    val bestDescriptivePercent: Int = 0,
    val lastStudiedAt: Long = 0L
) {
    fun computeMasterStatus(
        requiredMcqAccuracy: Int = 80,
        requiredFlashcards: Int = 1
    ): TopicMasterStatus {
        val isMastered = readingCompleted &&
            (videoCompleted || bestMcqAccuracy >= requiredMcqAccuracy) &&
            bestMcqAccuracy >= requiredMcqAccuracy &&
            flashcardsReviewed >= requiredFlashcards &&
            descriptivePracticed >= 1

        if (isMastered) return TopicMasterStatus.MASTERED
        return when {
            descriptivePracticed >= 1 -> TopicMasterStatus.DESCRIPTIVE_PRACTICED
            flashcardsReviewed >= requiredFlashcards -> TopicMasterStatus.FLASHCARDS_REVIEWED
            bestMcqAccuracy >= requiredMcqAccuracy || mcqPassed -> TopicMasterStatus.MCQS_PASSED
            mcqAttemptedCount > 0 -> TopicMasterStatus.MCQS_ATTEMPTED
            notesCreated && personalNotes.isNotBlank() -> TopicMasterStatus.NOTES_CREATED
            videoCompleted -> TopicMasterStatus.VIDEO_COMPLETE
            videoStarted -> TopicMasterStatus.VIDEO_STARTED
            readingCompleted -> TopicMasterStatus.READING_COMPLETE
            readingStarted -> TopicMasterStatus.READING_STARTED
            else -> TopicMasterStatus.NOT_STARTED
        }
    }

    fun masteryProgressFraction(requiredMcqAccuracy: Int = 80): Float {
        var points = 0f
        if (readingStarted) points += 0.5f
        if (readingCompleted) points += 1.5f
        if (videoStarted) points += 0.5f
        if (videoCompleted) points += 1.5f
        if (notesCreated && personalNotes.isNotBlank()) points += 1f
        if (mcqAttemptedCount > 0) points += 1f
        if (bestMcqAccuracy >= requiredMcqAccuracy) points += 1.5f
        if (flashcardsReviewed > 0) points += 1f
        if (descriptivePracticed > 0) points += 1.5f
        return (points / 10f).coerceIn(0f, 1f)
    }
}

@Entity(tableName = "video_progress")
data class VideoProgressEntity(
    @PrimaryKey val videoId: String,
    val filename: String,
    val title: String,
    val topicId: String,
    val partCode: String,
    val unitNumber: Int,
    val remoteUrl: String,
    val localFilePath: String = "",
    val downloadStatus: String = DownloadStatus.NOT_DOWNLOADED.name,
    val downloadProgress: Int = 0,
    val fileSizeBytes: Long = 0L,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val watchedPercent: Int = 0,
    val completed: Boolean = false,
    val catalogAvailable: Boolean = false,
    val catalogCheckedAt: Long = 0L,
    val lastError: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun resolvedDownloadStatus(): DownloadStatus =
        DownloadStatus.entries.firstOrNull { it.name == downloadStatus } ?: DownloadStatus.NOT_DOWNLOADED

    val hasLocalFile: Boolean
        get() = localFilePath.isNotBlank() && resolvedDownloadStatus() == DownloadStatus.DOWNLOADED
}

@Entity(tableName = "flashcards_sr")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val partCode: String,
    val deckTitle: String,
    val front: String,
    val back: String,
    val hint: String = "",
    val difficulty: String = "MEDIUM",
    val isCustom: Boolean = false,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastRating: String = "",
    val nextReviewAt: Long = 0L,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0L,
    val mastered: Boolean = false
)

@Entity(tableName = "question_attempts")
data class QuestionAttemptEntity(
    @PrimaryKey val questionId: String,
    val topicId: String,
    val partCode: String,
    val attemptsCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastWrongAt: Long = 0L,
    val lastCorrectAt: Long = 0L,
    val confidenceScore: Float = 0.5f
)

@Entity(tableName = "test_attempts")
data class TestAttemptEntity(
    @PrimaryKey val id: String,
    val modeTitle: String,
    val partCode: String,
    val topicId: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val accuracyPercent: Int,
    val durationSeconds: Int,
    val weakTopicsCsv: String = "",
    val strongTopicsCsv: String = "",
    val isMockExam: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_exam_state")
data class MockExamStateEntity(
    @PrimaryKey val id: String = "active_full_mock",
    val title: String,
    val questionIdsCsv: String,
    val selectedAnswersJson: String,
    val flaggedIndicesCsv: String,
    val descriptiveQuestionIdsCsv: String,
    val descriptiveAnswersJson: String,
    val remainingSeconds: Int,
    val currentIndex: Int,
    val isActive: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "descriptive_submissions")
data class DescriptiveSubmissionEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val topicId: String,
    val prompt: String,
    val answerText: String,
    val estimatedScore: Float,
    val maxMarks: Int,
    val matchedKeywordsCsv: String,
    val missingKeywordsCsv: String,
    val feedbackText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val durationMinutes: Int,
    val activityType: String,
    val topicId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "schedule_slots")
data class ScheduleSlotEntity(
    @PrimaryKey val id: String,
    val startTime: String,
    val endTime: String,
    val title: String,
    val description: String,
    val slotType: String,
    val topicId: String = "",
    val durationMinutes: Int = 30,
    val completedDateKey: String = "",
    val sortOrder: Int = 0
)
