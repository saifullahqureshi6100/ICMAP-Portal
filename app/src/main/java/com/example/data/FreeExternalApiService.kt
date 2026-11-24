package com.example.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Integrates $0, zero-API-key, zero-signup public REST APIs alongside built-in Gemini:
 * 1. Pollinations Open Text Endpoint (anonymous chat completions)
 * 2. Wikipedia Search & REST Summary API (instant academic/OM concept cards)
 * 3. Free Dictionary API (word/term definitions & phonetics)
 */
class FreeExternalApiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun askPollinationsChat(
        userPrompt: String,
        recentMessages: List<TutorMessage>
    ): String = withTimeout(30_000L) {
        withContext(Dispatchers.IO) {
            try {
                val messagesJson = JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put(
                                "content",
                                "You are a helpful, concise Study Tutor inside a mobile Study Portal. " +
                                    "Give clear structured bullet points, definitions, and exam-ready explanations " +
                                    "covering Organizational Management (OM), HRM, Marketing, Operations, Performance Management, and general subjects."
                            )
                        }
                    )
                    recentMessages.takeLast(6).forEach { msg ->
                        put(
                            JSONObject().apply {
                                put("role", if (msg.isUser) "user" else "assistant")
                                put("content", msg.text.take(1200))
                            }
                        )
                    }
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                    )
                }

                val payload = JSONObject().apply {
                    put("model", "openai")
                    put("messages", messagesJson)
                }

                val request = Request.Builder()
                    .url(POLLINATIONS_TEXT_URL)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/plain")
                    .header("User-Agent", APP_USER_AGENT)
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty().trim()
                    if (!response.isSuccessful) {
                        val detail = extractErrorMessage(rawBody)
                        throw IllegalStateException(
                            "Pollinations free endpoint returned HTTP ${response.code}" +
                                (if (detail.isNotBlank()) ": $detail" else "") +
                                ". Switch back to Built-in Gemini for reliable unlimited tutoring."
                        )
                    }
                    if (rawBody.isBlank()) {
                        throw IllegalStateException("Pollinations free endpoint returned an empty response.")
                    }

                    // Endpoint may return either OpenAI-compatible JSON or direct plain text
                    if (rawBody.startsWith("{")) {
                        val parsed = parseOpenAiStyleContent(rawBody)
                        if (!parsed.isNullOrBlank()) return@withContext parsed
                    }
                    rawBody
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Pollinations request timed out", e)
                throw IllegalStateException(
                    "Pollinations free endpoint timed out after 30s. Switch back to Built-in Gemini or retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Pollinations request failed", e)
                throw e
            }
        }
    }

    suspend fun fetchWikipediaSummary(query: String): FreeReferenceResult = withTimeout(20_000L) {
        withContext(Dispatchers.IO) {
            val cleaned = query.trim()
            if (cleaned.isEmpty()) {
                throw IllegalArgumentException("Enter a topic or term to look up.")
            }
            try {
                val encodedSearch = URLEncoder.encode(cleaned, StandardCharsets.UTF_8.name())
                val searchUrl =
                    "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedSearch&srlimit=1&format=json&utf8=1"
                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", APP_USER_AGENT)
                    .get()
                    .build()

                val resolvedTitle = client.newCall(searchRequest).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Wikipedia search failed (HTTP ${resp.code}).")
                    }
                    val root = JSONObject(body)
                    val hits = root.optJSONObject("query")?.optJSONArray("search")
                    if (hits == null || hits.length() == 0) {
                        throw IllegalStateException("No Wikipedia article found for \"$cleaned\".")
                    }
                    hits.getJSONObject(0).getString("title")
                }

                val encodedTitle = URLEncoder.encode(resolvedTitle.replace(' ', '_'), StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"
                val summaryRequest = Request.Builder()
                    .url(summaryUrl)
                    .header("User-Agent", APP_USER_AGENT)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(summaryRequest).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Wikipedia summary failed (HTTP ${resp.code}).")
                    }
                    val obj = JSONObject(body)
                    val title = obj.optString("title", resolvedTitle)
                    val description = obj.optString("description", "Wikipedia Reference")
                    val extract = obj.optString("extract", "").trim()
                    val pageUrl = obj.optJSONObject("content_urls")
                        ?.optJSONObject("mobile")
                        ?.optString("page", "")
                        .orEmpty()
                    if (extract.isBlank()) {
                        throw IllegalStateException("Wikipedia summary for \"$title\" is empty.")
                    }
                    FreeReferenceResult(
                        sourceName = "Wikipedia REST API ($0 • No Key)",
                        title = title,
                        subtitle = description,
                        summary = extract,
                        sourceUrl = pageUrl
                    )
                }
            } catch (e: TimeoutCancellationException) {
                throw IllegalStateException("Wikipedia lookup timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Wikipedia lookup failed", e)
                throw e
            }
        }
    }

    suspend fun fetchDictionaryDefinition(term: String): FreeReferenceResult = withTimeout(20_000L) {
        withContext(Dispatchers.IO) {
            val cleaned = term.trim().substringBefore(' ').lowercase()
            if (cleaned.isEmpty()) {
                throw IllegalArgumentException("Enter an English word to define.")
            }
            try {
                val encodedWord = URLEncoder.encode(cleaned, StandardCharsets.UTF_8.name())
                val url = "https://api.dictionaryapi.dev/api/v2/entries/en/$encodedWord"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", APP_USER_AGENT)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(
                            if (resp.code == 404) {
                                "No dictionary entry found for \"$cleaned\". Try Wikipedia Concept lookup for multi-word terms."
                            } else {
                                "Dictionary API returned HTTP ${resp.code}."
                            }
                        )
                    }
                    val arr = JSONArray(body)
                    if (arr.length() == 0) {
                        throw IllegalStateException("No dictionary entries returned for \"$cleaned\".")
                    }
                    val entry = arr.getJSONObject(0)
                    val word = entry.optString("word", cleaned)
                    val phonetic = entry.optString("phonetic", "")
                    val meanings = entry.optJSONArray("meanings") ?: JSONArray()
                    val summaryBuilder = StringBuilder()
                    for (i in 0 until minOf(meanings.length(), 3)) {
                        val m = meanings.getJSONObject(i)
                        val pos = m.optString("partOfSpeech", "")
                        val defs = m.optJSONArray("definitions") ?: continue
                        if (defs.length() > 0) {
                            val d = defs.getJSONObject(0)
                            val defText = d.optString("definition", "")
                            val example = d.optString("example", "")
                            if (defText.isNotBlank()) {
                                summaryBuilder.append("[$pos] $defText")
                                if (example.isNotBlank()) {
                                    summaryBuilder.append("\nExample: \"$example\"")
                                }
                                if (i < minOf(meanings.length(), 3) - 1) {
                                    summaryBuilder.append("\n\n")
                                }
                            }
                        }
                    }
                    val finalSummary = summaryBuilder.toString().trim()
                    if (finalSummary.isBlank()) {
                        throw IllegalStateException("No readable definitions found for \"$word\".")
                    }
                    FreeReferenceResult(
                        sourceName = "Free Dictionary API ($0 • No Key)",
                        title = word.replaceFirstChar { it.uppercase() },
                        subtitle = phonetic.ifBlank { "English Lexicon" },
                        summary = finalSummary,
                        sourceUrl = "https://en.wiktionary.org/wiki/$encodedWord"
                    )
                }
            } catch (e: TimeoutCancellationException) {
                throw IllegalStateException("Dictionary lookup timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Dictionary lookup failed", e)
                throw e
            }
        }
    }

    private fun parseOpenAiStyleContent(jsonStr: String): String? = runCatching {
        val obj = JSONObject(jsonStr)
        val choices = obj.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        choices.getJSONObject(0)
            .optJSONObject("message")
            ?.optString("content", "")
            ?.trim()
    }.getOrNull()

    private fun extractErrorMessage(raw: String): String {
        if (raw.isBlank()) return ""
        return runCatching {
            val obj = JSONObject(raw)
            obj.optJSONObject("error")?.optString("message")
                ?: obj.optString("error")
                ?: obj.optString("message")
        }.getOrNull().orEmpty().ifBlank { raw.take(140) }
    }

    private companion object {
        const val TAG = "FreeExternalApi"
        const val POLLINATIONS_TEXT_URL = "https://text.pollinations.ai/"
        const val APP_USER_AGENT = "StudyPortalAndroid/1.0"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
