package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.preferences.AiProviderType
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.StudyLanguageMode
import com.example.data.syllabus.SyllabusTopic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class TutorStyleRequest(val label: String, val instruction: String) {
    DEFAULT_BILINGUAL("Simple English + Roman Urdu", "Explain clearly in Simple English + Roman Urdu with easy structure."),
    SIMPLE_ENGLISH("Explain Simply", "Explain in short, crystal-clear Simple English bullets."),
    ROMAN_URDU("Roman Urdu", "Explain naturally in Roman Urdu mixed with ICMA English terms."),
    PAKISTANI_EXAMPLE("Pakistani / PTCL Example", "Give a realistic Pakistani corporate example (like PTCL, HBL, Engro, Systems Ltd)."),
    EXAM_ANSWER("5-Mark Exam Answer", "Write a crisp 5-mark ICMA exam answer with 5 numbered headings."),
    MEMORY_TRICK("Memory Trick", "Create a memorable acronym or mental hook so the student never forgets it."),
    VIVA_QUESTIONS("Viva / Oral Drill", "Ask 3 rapid viva questions with short model answers."),
    COMPARE_CONCEPTS("Compare Concepts", "Create a side-by-side comparison table highlighting key exam distinctions."),
    NUMERICAL("Explain Numerical", "Show the step-by-step formula, calculation example, and interpretation.")
}

data class JarvisResponse(
    val text: String,
    val isOfflineFallback: Boolean,
    val providerLabel: String
)

class SecureAiProviderManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun getOrCreateSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun saveApiKey(provider: AiProviderType, rawApiKey: String) {
        if (provider == AiProviderType.OFFLINE_ONLY) return
        val trimmed = rawApiKey.trim()
        if (trimmed.isEmpty()) {
            clearApiKey(provider)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString("enc_key_${provider.name}", encoded).apply()
    }

    fun hasApiKey(provider: AiProviderType): Boolean = getApiKey(provider).isNotBlank()

    fun getMaskedApiKey(provider: AiProviderType): String {
        val key = getApiKey(provider)
        if (key.isBlank()) return ""
        return if (key.length <= 8) "••••••••" else "${key.take(4)}••••${key.takeLast(4)}"
    }

    fun getApiKey(provider: AiProviderType): String {
        if (provider == AiProviderType.OFFLINE_ONLY) return ""
        val stored = prefs.getString("enc_key_${provider.name}", null) ?: return ""
        return try {
            val parts = stored.split(":", limit = 2)
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val data = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun clearApiKey(provider: AiProviderType) {
        prefs.edit().remove("enc_key_${provider.name}").apply()
    }

    suspend fun askJarvis(
        question: String,
        style: TutorStyleRequest,
        settings: OmJarvisSettings,
        currentTopic: SyllabusTopic?,
        weakTopics: List<String>,
        readinessScore: Int,
        isOnline: Boolean
    ): JarvisResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(settings.aiProviderType)
        val canCallCloud = isOnline &&
            settings.aiProviderType != AiProviderType.OFFLINE_ONLY &&
            apiKey.isNotBlank()

        if (canCallCloud) {
            try {
                val cloudText = executeCloudRequest(
                    provider = settings.aiProviderType,
                    apiKey = apiKey,
                    model = settings.aiModelName.ifBlank { settings.aiProviderType.defaultModel },
                    customEndpoint = settings.aiCustomEndpoint,
                    question = question,
                    style = style,
                    settings = settings,
                    currentTopic = currentTopic,
                    weakTopics = weakTopics,
                    readinessScore = readinessScore
                )
                if (cloudText.isNotBlank()) {
                    return@withContext JarvisResponse(
                        text = cloudText,
                        isOfflineFallback = false,
                        providerLabel = "${settings.aiProviderType.label} (${settings.aiModelName.ifBlank { settings.aiProviderType.defaultModel }})"
                    )
                }
            } catch (_: Exception) {
                // Gracefully fall back to Offline Jarvis Engine without breaking the study flow
            }
        }

        val offlineReply = buildOfflineJarvisReply(
            question = question,
            style = style,
            topic = currentTopic,
            weakTopics = weakTopics,
            readinessScore = readinessScore
        )
        JarvisResponse(
            text = offlineReply,
            isOfflineFallback = true,
            providerLabel = if (isOnline && apiKey.isBlank() && settings.aiProviderType != AiProviderType.OFFLINE_ONLY) {
                "Offline Jarvis Mode (No Key Saved)"
            } else {
                "Offline Jarvis Mode"
            }
        )
    }

    private fun executeCloudRequest(
        provider: AiProviderType,
        apiKey: String,
        model: String,
        customEndpoint: String,
        question: String,
        style: TutorStyleRequest,
        settings: OmJarvisSettings,
        currentTopic: SyllabusTopic?,
        weakTopics: List<String>,
        readinessScore: Int
    ): String {
        val systemPrompt = buildString {
            appendLine("You are OM Jarvis, mentor to Saifullah Qureshi preparing for ICMA Pakistan M3 Organizational Management (Exam: 7 Dec 2026, Goal: Certificate of Merit).")
            appendLine("Preferred student style: ${settings.languageMode.label}. Request focus: ${style.instruction}")
            appendLine("Current Readiness Score: $readinessScore/100. Weak areas: ${weakTopics.joinToString(", ").ifBlank { "None" }}.")
            if (currentTopic != null) {
                appendLine("Active topic context: Unit ${currentTopic.unit} - ${currentTopic.name} (${currentTopic.description}). Formula: ${currentTopic.formulae}.")
            }
            appendLine("Keep answers structured, high-yield for ICMA M3 exams, accurate, and easy to remember.")
        }

        if (provider == AiProviderType.GEMINI) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            val bodyJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", question)))
                }))
            }.toString()
            val req = Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", apiKey)
                .post(bodyJson.toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Gemini HTTP ${resp.code}")
                val raw = resp.body?.string().orEmpty()
                val root = JSONObject(raw)
                return root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "")
                    .orEmpty()
                    .trim()
            }
        }

        val endpoint = customEndpoint.trim().ifBlank { provider.defaultEndpoint }
        require(endpoint.startsWith("https://")) { "Only HTTPS endpoints are permitted." }
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", 0.35)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", question))
            })
        }.toString()
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON_MEDIA))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Provider HTTP ${resp.code}")
            val raw = resp.body?.string().orEmpty()
            val root = JSONObject(raw)
            return root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                .orEmpty()
                .trim()
        }
    }

    fun buildOfflineJarvisReply(
        question: String,
        style: TutorStyleRequest,
        topic: SyllabusTopic?,
        weakTopics: List<String>,
        readinessScore: Int
    ): String {
        val focus = topic
        return buildString {
            appendLine("Offline Jarvis Mentor Response")
            if (focus != null) {
                appendLine("Topic: Unit ${focus.unit} — ${focus.name} (${focus.partTitle})")
                appendLine()
                when (style) {
                    TutorStyleRequest.PAKISTANI_EXAMPLE -> {
                        appendLine("Pakistani Corporate Example:")
                        appendLine(focus.pakistaniExample)
                        appendLine()
                        appendLine("Roman Urdu Concept:")
                        appendLine(focus.romanUrduSummary)
                    }
                    TutorStyleRequest.EXAM_ANSWER -> {
                        appendLine("ICMA 5-Mark Exam Answer Structure:")
                        appendLine("1) Core Definition: ${focus.description}")
                        focus.keyPoints.forEachIndexed { idx, point ->
                            appendLine("${idx + 2}) $point")
                        }
                        if (focus.formulae.isNotBlank()) {
                            appendLine("Key Formula / Framework: ${focus.formulae}")
                        }
                    }
                    TutorStyleRequest.MEMORY_TRICK -> {
                        appendLine("Memory Anchor & Key Formula:")
                        appendLine(focus.formulae.ifBlank { focus.keyPoints.firstOrNull().orEmpty() })
                        appendLine()
                        appendLine("Quick Roman Urdu Recall:")
                        appendLine(focus.romanUrduSummary)
                    }
                    TutorStyleRequest.VIVA_QUESTIONS -> {
                        appendLine("Rapid Viva Drill (Practice Answers Aloud):")
                        appendLine("Q1: What is the core purpose of ${focus.name}?")
                        appendLine("A1: ${focus.description}")
                        appendLine("Q2: Name 2 exam-critical points or metrics.")
                        appendLine("A2: ${focus.keyPoints.take(2).joinToString(" | ")}")
                        appendLine("Q3: Give a real Pakistani company application.")
                        appendLine("A3: ${focus.pakistaniExample}")
                    }
                    TutorStyleRequest.NUMERICAL -> {
                        appendLine("Formula & Quantitative Breakdown:")
                        appendLine("Formula: ${focus.formulae}")
                        focus.keyPoints.forEach { appendLine("- $it") }
                    }
                    TutorStyleRequest.COMPARE_CONCEPTS -> {
                        appendLine("Key Exam Distinctions & Points:")
                        focus.keyPoints.forEachIndexed { i, p -> appendLine("${i + 1}. $p") }
                        appendLine()
                        appendLine("Formula / Anchor: ${focus.formulae}")
                    }
                    else -> {
                        appendLine("Simple English + Roman Urdu Explanation:")
                        appendLine(focus.romanUrduSummary)
                        appendLine()
                        appendLine("High-Yield Bullet Points:")
                        focus.keyPoints.forEach { appendLine("• $it") }
                        if (focus.formulae.isNotBlank()) {
                            appendLine()
                            appendLine("Formula / Memory Line: ${focus.formulae}")
                        }
                        appendLine()
                        appendLine("Pakistani Business Example: ${focus.pakistaniExample}")
                    }
                }
            } else {
                appendLine("Your Readiness Score is $readinessScore/100.")
                appendLine("Top Priority Weak Areas: ${weakTopics.joinToString(", ").ifBlank { "Keep completing Unit 14 BSC & Mintzberg Q.6." }}")
                appendLine("Question noted: \"$question\" — Open any topic inside the Syllabus tab for chapter-specific notes, 5-mark answers, and Pakistani examples offline.")
            }
        }.trim()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "om_jarvis_ai_key_aes"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFS_FILE = "om_jarvis_encrypted_keys"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
