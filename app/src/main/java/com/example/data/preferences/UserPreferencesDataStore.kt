package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(name = "om_jarvis_prefs")

enum class StudyLanguageMode(val label: String) {
    BILINGUAL("Simple English + Roman Urdu"),
    ENGLISH("Simple English"),
    ROMAN_URDU("Roman Urdu + Keywords")
}

enum class MotivationFrequency(val label: String) {
    DISABLE("Disabled"),
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High")
}

enum class AiProviderType(
    val label: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val isOpenAiCompatible: Boolean
) {
    OFFLINE_ONLY(
        label = "Offline Jarvis Mode",
        defaultEndpoint = "",
        defaultModel = "offline-om-engine",
        isOpenAiCompatible = false
    ),
    GROQ(
        label = "Groq",
        defaultEndpoint = "https://api.groq.com/openai/v1/chat/completions",
        defaultModel = "llama-3.3-70b-versatile",
        isOpenAiCompatible = true
    ),
    OPENROUTER(
        label = "OpenRouter",
        defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "google/gemini-2.0-flash-exp:free",
        isOpenAiCompatible = true
    ),
    OPENAI(
        label = "OpenAI",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini",
        isOpenAiCompatible = true
    ),
    DEEPSEEK(
        label = "DeepSeek",
        defaultEndpoint = "https://api.deepseek.com/chat/completions",
        defaultModel = "deepseek-chat",
        isOpenAiCompatible = true
    ),
    GEMINI(
        label = "Gemini API",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/",
        defaultModel = "gemini-2.5-flash",
        isOpenAiCompatible = false
    ),
    CUSTOM_OPENAI(
        label = "Custom OpenAI-Compatible",
        defaultEndpoint = "",
        defaultModel = "gpt-4o-mini",
        isOpenAiCompatible = true
    )
}

data class OmJarvisSettings(
    val onboardingCompleted: Boolean = false,
    val studentName: String = "Saifullah Qureshi",
    val examDateMillis: Long = defaultDec7ExamMillis(),
    val meritModeEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val languageMode: StudyLanguageMode = StudyLanguageMode.BILINGUAL,
    val readerFontSizeSp: Float = 16f,
    val readerLineSpacing: Float = 1.5f,
    val ttsSpeed: Float = 0.94f,
    val ttsPitch: Float = 0.85f,
    val dailyTargetMinutes: Int = 180,
    val videoCompletionThresholdPct: Int = 85,
    val masteryMcqThresholdPct: Int = 80,
    val motivationFrequency: MotivationFrequency = MotivationFrequency.NORMAL,
    val notificationsEnabled: Boolean = true,
    val morningReminderTime: String = "08:00",
    val lunchReminderTime: String = "12:45",
    val eveningReminderTime: String = "19:00",
    val nightReminderTime: String = "20:15",
    val flashcardReminderTime: String = "17:30",
    val mockReminderTime: String = "10:00",
    val lastStudiedTopicId: String = "om_u14_bsc",
    val lastStudiedActionLabel: String = "Balanced Scorecard (Q.4) • Unit 14",
    val aiProviderType: AiProviderType = AiProviderType.OFFLINE_ONLY,
    val aiModelName: String = AiProviderType.OFFLINE_ONLY.defaultModel,
    val aiCustomEndpoint: String = ""
) {
    companion object {
        fun defaultDec7ExamMillis(): Long = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 7)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val EXAM_DATE_MILLIS = longPreferencesKey("exam_date_millis")
        val MERIT_MODE = booleanPreferencesKey("merit_mode_enabled")
        val DARK_THEME = booleanPreferencesKey("dark_theme_enabled")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val DAILY_TARGET_MINUTES = intPreferencesKey("daily_target_minutes")
        val VIDEO_THRESHOLD_PCT = intPreferencesKey("video_threshold_pct")
        val MASTERY_MCQ_THRESHOLD = intPreferencesKey("mastery_mcq_threshold")
        val MOTIVATION_FREQ = stringPreferencesKey("motivation_frequency")
        val NOTIFS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MORNING_TIME = stringPreferencesKey("morning_reminder_time")
        val LUNCH_TIME = stringPreferencesKey("lunch_reminder_time")
        val EVENING_TIME = stringPreferencesKey("evening_reminder_time")
        val NIGHT_TIME = stringPreferencesKey("night_reminder_time")
        val FLASHCARD_TIME = stringPreferencesKey("flashcard_reminder_time")
        val MOCK_TIME = stringPreferencesKey("mock_reminder_time")
        val LAST_TOPIC_ID = stringPreferencesKey("last_studied_topic_id")
        val LAST_ACTION_LABEL = stringPreferencesKey("last_studied_action_label")
        val AI_PROVIDER_TYPE = stringPreferencesKey("ai_provider_type")
        val AI_MODEL_NAME = stringPreferencesKey("ai_model_name")
        val AI_CUSTOM_ENDPOINT = stringPreferencesKey("ai_custom_endpoint")
    }

    val settingsFlow: Flow<OmJarvisSettings> = context.jarvisDataStore.data.map { prefs ->
        val provider = prefs[Keys.AI_PROVIDER_TYPE]?.let { raw ->
            AiProviderType.entries.firstOrNull { it.name == raw }
        } ?: AiProviderType.OFFLINE_ONLY

        OmJarvisSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            studentName = prefs[Keys.STUDENT_NAME] ?: "Saifullah Qureshi",
            examDateMillis = prefs[Keys.EXAM_DATE_MILLIS] ?: OmJarvisSettings.defaultDec7ExamMillis(),
            meritModeEnabled = prefs[Keys.MERIT_MODE] ?: true,
            darkThemeEnabled = prefs[Keys.DARK_THEME] ?: true,
            languageMode = prefs[Keys.LANGUAGE_MODE]?.let { raw ->
                StudyLanguageMode.entries.firstOrNull { it.name == raw }
            } ?: StudyLanguageMode.BILINGUAL,
            readerFontSizeSp = prefs[Keys.FONT_SIZE_SP] ?: 16f,
            readerLineSpacing = prefs[Keys.LINE_SPACING] ?: 1.5f,
            ttsSpeed = prefs[Keys.TTS_SPEED] ?: 0.94f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 0.85f,
            dailyTargetMinutes = prefs[Keys.DAILY_TARGET_MINUTES] ?: 180,
            videoCompletionThresholdPct = prefs[Keys.VIDEO_THRESHOLD_PCT] ?: 85,
            masteryMcqThresholdPct = prefs[Keys.MASTERY_MCQ_THRESHOLD] ?: 80,
            motivationFrequency = prefs[Keys.MOTIVATION_FREQ]?.let { raw ->
                MotivationFrequency.entries.firstOrNull { it.name == raw }
            } ?: MotivationFrequency.NORMAL,
            notificationsEnabled = prefs[Keys.NOTIFS_ENABLED] ?: true,
            morningReminderTime = prefs[Keys.MORNING_TIME] ?: "08:00",
            lunchReminderTime = prefs[Keys.LUNCH_TIME] ?: "12:45",
            eveningReminderTime = prefs[Keys.EVENING_TIME] ?: "19:00",
            nightReminderTime = prefs[Keys.NIGHT_TIME] ?: "20:15",
            flashcardReminderTime = prefs[Keys.FLASHCARD_TIME] ?: "17:30",
            mockReminderTime = prefs[Keys.MOCK_TIME] ?: "10:00",
            lastStudiedTopicId = prefs[Keys.LAST_TOPIC_ID] ?: "om_u14_bsc",
            lastStudiedActionLabel = prefs[Keys.LAST_ACTION_LABEL] ?: "Balanced Scorecard (Q.4) • Unit 14",
            aiProviderType = provider,
            aiModelName = prefs[Keys.AI_MODEL_NAME] ?: provider.defaultModel,
            aiCustomEndpoint = prefs[Keys.AI_CUSTOM_ENDPOINT] ?: ""
        )
    }

    suspend fun update(transform: (OmJarvisSettings) -> OmJarvisSettings) {
        context.jarvisDataStore.edit { prefs ->
            val currentProvider = prefs[Keys.AI_PROVIDER_TYPE]?.let { raw ->
                AiProviderType.entries.firstOrNull { it.name == raw }
            } ?: AiProviderType.OFFLINE_ONLY
            val current = OmJarvisSettings(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                studentName = prefs[Keys.STUDENT_NAME] ?: "Saifullah Qureshi",
                examDateMillis = prefs[Keys.EXAM_DATE_MILLIS] ?: OmJarvisSettings.defaultDec7ExamMillis(),
                meritModeEnabled = prefs[Keys.MERIT_MODE] ?: true,
                darkThemeEnabled = prefs[Keys.DARK_THEME] ?: true,
                languageMode = prefs[Keys.LANGUAGE_MODE]?.let { r -> StudyLanguageMode.entries.firstOrNull { it.name == r } } ?: StudyLanguageMode.BILINGUAL,
                readerFontSizeSp = prefs[Keys.FONT_SIZE_SP] ?: 16f,
                readerLineSpacing = prefs[Keys.LINE_SPACING] ?: 1.5f,
                ttsSpeed = prefs[Keys.TTS_SPEED] ?: 0.94f,
                ttsPitch = prefs[Keys.TTS_PITCH] ?: 0.85f,
                dailyTargetMinutes = prefs[Keys.DAILY_TARGET_MINUTES] ?: 180,
                videoCompletionThresholdPct = prefs[Keys.VIDEO_THRESHOLD_PCT] ?: 85,
                masteryMcqThresholdPct = prefs[Keys.MASTERY_MCQ_THRESHOLD] ?: 80,
                motivationFrequency = prefs[Keys.MOTIVATION_FREQ]?.let { r -> MotivationFrequency.entries.firstOrNull { it.name == r } } ?: MotivationFrequency.NORMAL,
                notificationsEnabled = prefs[Keys.NOTIFS_ENABLED] ?: true,
                morningReminderTime = prefs[Keys.MORNING_TIME] ?: "08:00",
                lunchReminderTime = prefs[Keys.LUNCH_TIME] ?: "12:45",
                eveningReminderTime = prefs[Keys.EVENING_TIME] ?: "19:00",
                nightReminderTime = prefs[Keys.NIGHT_TIME] ?: "20:15",
                flashcardReminderTime = prefs[Keys.FLASHCARD_TIME] ?: "17:30",
                mockReminderTime = prefs[Keys.MOCK_TIME] ?: "10:00",
                lastStudiedTopicId = prefs[Keys.LAST_TOPIC_ID] ?: "om_u14_bsc",
                lastStudiedActionLabel = prefs[Keys.LAST_ACTION_LABEL] ?: "Balanced Scorecard (Q.4) • Unit 14",
                aiProviderType = currentProvider,
                aiModelName = prefs[Keys.AI_MODEL_NAME] ?: currentProvider.defaultModel,
                aiCustomEndpoint = prefs[Keys.AI_CUSTOM_ENDPOINT] ?: ""
            )
            val updated = transform(current)
            prefs[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            prefs[Keys.STUDENT_NAME] = updated.studentName
            prefs[Keys.EXAM_DATE_MILLIS] = updated.examDateMillis
            prefs[Keys.MERIT_MODE] = updated.meritModeEnabled
            prefs[Keys.DARK_THEME] = updated.darkThemeEnabled
            prefs[Keys.LANGUAGE_MODE] = updated.languageMode.name
            prefs[Keys.FONT_SIZE_SP] = updated.readerFontSizeSp.coerceIn(12f, 26f)
            prefs[Keys.LINE_SPACING] = updated.readerLineSpacing.coerceIn(1.1f, 2.2f)
            prefs[Keys.TTS_SPEED] = updated.ttsSpeed.coerceIn(0.6f, 1.5f)
            prefs[Keys.TTS_PITCH] = updated.ttsPitch.coerceIn(0.6f, 1.15f)
            prefs[Keys.DAILY_TARGET_MINUTES] = updated.dailyTargetMinutes.coerceIn(30, 600)
            prefs[Keys.VIDEO_THRESHOLD_PCT] = updated.videoCompletionThresholdPct.coerceIn(50, 100)
            prefs[Keys.MASTERY_MCQ_THRESHOLD] = updated.masteryMcqThresholdPct.coerceIn(50, 100)
            prefs[Keys.MOTIVATION_FREQ] = updated.motivationFrequency.name
            prefs[Keys.NOTIFS_ENABLED] = updated.notificationsEnabled
            prefs[Keys.MORNING_TIME] = updated.morningReminderTime
            prefs[Keys.LUNCH_TIME] = updated.lunchReminderTime
            prefs[Keys.EVENING_TIME] = updated.eveningReminderTime
            prefs[Keys.NIGHT_TIME] = updated.nightReminderTime
            prefs[Keys.FLASHCARD_TIME] = updated.flashcardReminderTime
            prefs[Keys.MOCK_TIME] = updated.mockReminderTime
            prefs[Keys.LAST_TOPIC_ID] = updated.lastStudiedTopicId
            prefs[Keys.LAST_ACTION_LABEL] = updated.lastStudiedActionLabel
            prefs[Keys.AI_PROVIDER_TYPE] = updated.aiProviderType.name
            prefs[Keys.AI_MODEL_NAME] = updated.aiModelName
            prefs[Keys.AI_CUSTOM_ENDPOINT] = updated.aiCustomEndpoint
        }
    }

    suspend fun saveLastStudied(topicId: String, label: String) {
        update { it.copy(lastStudiedTopicId = topicId, lastStudiedActionLabel = label) }
    }
}
