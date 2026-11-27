package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TopicProgressEntity::class,
        VideoProgressEntity::class,
        FlashcardEntity::class,
        QuestionAttemptEntity::class,
        TestAttemptEntity::class,
        MockExamStateEntity::class,
        DescriptiveSubmissionEntity::class,
        StudySessionEntity::class,
        ScheduleSlotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OmJarvisDatabase : RoomDatabase() {
    abstract fun topicProgressDao(): TopicProgressDao
    abstract fun videoProgressDao(): VideoProgressDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun questionAttemptDao(): QuestionAttemptDao
    abstract fun testAttemptDao(): TestAttemptDao
    abstract fun mockExamStateDao(): MockExamStateDao
    abstract fun descriptiveSubmissionDao(): DescriptiveSubmissionDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun scheduleSlotDao(): ScheduleSlotDao

    companion object {
        @Volatile
        private var instance: OmJarvisDatabase? = null

        fun getInstance(context: Context): OmJarvisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OmJarvisDatabase::class.java,
                    "om_jarvis_study.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
