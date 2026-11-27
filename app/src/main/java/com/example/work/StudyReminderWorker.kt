package com.example.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.data.local.OmJarvisDatabase
import com.example.data.preferences.OmJarvisSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ReminderDeepLink {
    const val EXTRA_TARGET_ROUTE = "om_jarvis_target_route"
    const val ROUTE_BRIEF = "home_brief"
    const val ROUTE_TEST = "tests"
    const val ROUTE_SYLLABUS = "syllabus"
    const val ROUTE_FLASHCARDS = "flashcards"
    const val ROUTE_MOCK = "mock_exam"
    const val ROUTE_CALL_JARVIS = "call_jarvis"
}

enum class StudyReminderType(
    val workName: String,
    val title: String,
    val body: String,
    val route: String,
    val notifId: Int
) {
    MORNING_BRIEF("om_rem_morning", "OM Jarvis Morning Briefing", "Assalam-o-Alaikum Saifullah! Your daily OM timetable & priority unit are ready.", ReminderDeepLink.ROUTE_CALL_JARVIS, 1001),
    LUNCH_TEST("om_rem_lunch", "Lunch Break Office Test", "10 Quick Adaptive MCQs ready — takes 7 minutes!", ReminderDeepLink.ROUTE_TEST, 1002),
    FLASHCARD_REVIEW("om_rem_cards", "SM-2 Flashcard Recall", "Due spaced-repetition flashcards waiting to lock into memory.", ReminderDeepLink.ROUTE_FLASHCARDS, 1003),
    EVENING_STUDY("om_rem_evening", "Evening Deep Study Block", "Continue your video lesson + bilingual OM notes now.", ReminderDeepLink.ROUTE_SYLLABUS, 1004),
    NIGHT_REVISION("om_rem_night", "Night 60/40 Exam Drill", "Timed MCQ + 5-mark descriptive writing drill ready.", ReminderDeepLink.ROUTE_TEST, 1005),
    MOCK_EXAM("om_rem_mock", "Full-Course Merit Mock Window", "Test your readiness with a timed OM Mock Examination.", ReminderDeepLink.ROUTE_MOCK, 1006)
}

class StudyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val typeName = inputData.getString(KEY_TYPE) ?: StudyReminderType.MORNING_BRIEF.name
        val type = StudyReminderType.entries.firstOrNull { it.name == typeName } ?: StudyReminderType.MORNING_BRIEF

        // Check Room database to see if today's study session has started yet
        val db = OmJarvisDatabase.getInstance(applicationContext)
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayMinutes = db.studySessionDao().getTodayTotalMinutes(todayKey) ?: 0

        // If today's session has already started (todayMinutes > 0), skip alerting that it hasn't started
        if (todayMinutes > 0) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OM Jarvis Study Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily study mission, flashcards, and mock exam alerts."
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderDeepLink.EXTRA_TARGET_ROUTE, type.route)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            type.notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⚠️ ${type.title} (Session Pending)")
            .setContentText("Your daily M3 study session hasn't started yet today! Tap to begin.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(type.notifId, notif)
        } catch (_: SecurityException) {
            // Permission not granted yet; succeed silently without crashing
        }
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "reminder_type"
        const val CHANNEL_ID = "om_jarvis_study_channel"
    }
}

object ReminderScheduler {

    fun scheduleAll(context: Context, settings: OmJarvisSettings) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (!settings.notificationsEnabled) {
            StudyReminderType.entries.forEach { wm.cancelUniqueWork(it.workName) }
            return
        }

        scheduleDaily(wm, StudyReminderType.MORNING_BRIEF, settings.morningReminderTime)
        scheduleDaily(wm, StudyReminderType.LUNCH_TEST, settings.lunchReminderTime)
        scheduleDaily(wm, StudyReminderType.FLASHCARD_REVIEW, settings.flashcardReminderTime)
        scheduleDaily(wm, StudyReminderType.EVENING_STUDY, settings.eveningReminderTime)
        scheduleDaily(wm, StudyReminderType.NIGHT_REVISION, settings.nightReminderTime)
        scheduleDaily(wm, StudyReminderType.MOCK_EXAM, settings.mockReminderTime)
    }

    fun triggerTestNotificationNow(context: Context, type: StudyReminderType) {
        val req = OneTimeWorkRequestBuilder<StudyReminderWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .setInputData(workDataOf(StudyReminderWorker.KEY_TYPE to type.name))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork("${type.workName}_now", ExistingWorkPolicy.REPLACE, req)
    }

    private fun scheduleDaily(wm: WorkManager, type: StudyReminderType, hhMm: String) {
        val parts = hhMm.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val min = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelayMinutes = ((next.timeInMillis - now.timeInMillis) / 60000L).coerceAtLeast(1L)

        val req = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(StudyReminderWorker.KEY_TYPE to type.name))
            .build()

        wm.enqueueUniquePeriodicWork(
            type.workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }
}
