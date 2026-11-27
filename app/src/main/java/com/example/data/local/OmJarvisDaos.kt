package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicProgressDao {
    @Query("SELECT * FROM topic_progress ORDER BY unitNumber ASC, topicId ASC")
    fun observeAll(): Flow<List<TopicProgressEntity>>

    @Query("SELECT * FROM topic_progress ORDER BY unitNumber ASC, topicId ASC")
    suspend fun getAllOnce(): List<TopicProgressEntity>

    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId LIMIT 1")
    suspend fun getById(topicId: String): TopicProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TopicProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<TopicProgressEntity>)

    @Query("DELETE FROM topic_progress")
    suspend fun clearAll()
}

@Dao
interface VideoProgressDao {
    @Query("SELECT * FROM video_progress ORDER BY unitNumber ASC, title ASC")
    fun observeAll(): Flow<List<VideoProgressEntity>>

    @Query("SELECT * FROM video_progress ORDER BY unitNumber ASC, title ASC")
    suspend fun getAllOnce(): List<VideoProgressEntity>

    @Query("SELECT * FROM video_progress WHERE videoId = :videoId LIMIT 1")
    suspend fun getById(videoId: String): VideoProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VideoProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VideoProgressEntity>)

    @Query("DELETE FROM video_progress")
    suspend fun clearAll()
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards_sr ORDER BY nextReviewAt ASC, lapses DESC")
    fun observeAll(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards_sr")
    suspend fun getAllOnce(): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<FlashcardEntity>)

    @Query("DELETE FROM flashcards_sr WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM flashcards_sr WHERE isCustom = 1")
    suspend fun clearCustom()

    @Query("DELETE FROM flashcards_sr")
    suspend fun clearAll()
}

@Dao
interface QuestionAttemptDao {
    @Query("SELECT * FROM question_attempts")
    fun observeAll(): Flow<List<QuestionAttemptEntity>>

    @Query("SELECT * FROM question_attempts WHERE questionId = :questionId LIMIT 1")
    suspend fun getById(questionId: String): QuestionAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuestionAttemptEntity)

    @Query("DELETE FROM question_attempts")
    suspend fun clearAll()
}

@Dao
interface TestAttemptDao {
    @Query("SELECT * FROM test_attempts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TestAttemptEntity>>

    @Query("SELECT * FROM test_attempts ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<TestAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TestAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TestAttemptEntity>)

    @Query("DELETE FROM test_attempts")
    suspend fun clearAll()
}

@Dao
interface MockExamStateDao {
    @Query("SELECT * FROM mock_exam_state WHERE id = 'active_full_mock' LIMIT 1")
    fun observeActive(): Flow<MockExamStateEntity?>

    @Query("SELECT * FROM mock_exam_state WHERE id = 'active_full_mock' LIMIT 1")
    suspend fun getActiveOnce(): MockExamStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: MockExamStateEntity)

    @Query("DELETE FROM mock_exam_state")
    suspend fun clear()
}

@Dao
interface DescriptiveSubmissionDao {
    @Query("SELECT * FROM descriptive_submissions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DescriptiveSubmissionEntity>>

    @Query("SELECT * FROM descriptive_submissions ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<DescriptiveSubmissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DescriptiveSubmissionEntity)

    @Query("DELETE FROM descriptive_submissions")
    suspend fun clearAll()
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StudySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StudySessionEntity>)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAll()
}

@Dao
interface ScheduleSlotDao {
    @Query("SELECT * FROM schedule_slots ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ScheduleSlotEntity>>

    @Query("SELECT * FROM schedule_slots ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<ScheduleSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleSlotEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<ScheduleSlotEntity>)

    @Query("DELETE FROM schedule_slots")
    suspend fun clearAll()
}
