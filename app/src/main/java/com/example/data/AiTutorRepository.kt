package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

data class GeneratedPlanTask(
    val title: String,
    val topic: String,
    val dayLabel: String,
    val targetMinutes: Long
)

data class GeneratedFlashcard(
    val front: String,
    val back: String,
    val hint: String
)

class AiTutorRepository {

    private val tutorSystemInstruction = content {
        text(
            """
            You are a veteran Corporate Finance & OM Strategist (30+ years executive finance experience) and Master Teacher & Recovery Coach (50+ years teaching) guiding an executive student from ZERO preparation to win the Pakistan Certificate of Merit in ICMA M3 Organizational Management (ML-1) on 5 December 2026.
            Deeply mastered syllabus:
            - Operations Mgmt (Units 1–7): PESTEL, ESG, Mintzberg's 5 Organizational Parts, Job/Batch/Process/Additive Manufacturing, 5S, TPM & OEE, MRP/MRP-II/ERP, OPT (Drum-Buffer-Rope), Capacity/Queuing, Quality (Juran COQ, Six Sigma DMAIC, Kaizen, Lean/Kanban, Quality Circles, TQMEX, ISO standards).
            - Human Capital Mgmt (Units 8–9): Motivation (Taylor, Maslow, Vroom, Herzberg, McGregor), Handy's Shamrock & Psychological Contract, 70-20-10 Learning, Interview Limitations (Halo effect, Contagious bias, Stereotyping), Outsourcing Recruitment, Pfeffer's 7 Practices, Kolb's Learning Cycle.
            - Marketing Mgmt (Units 10–12): Production/Product/Sales/Marketing Orientations, Sales Orientation Drawbacks, STP, 4Ps/7Ps Services Mix, BCG & Ansoff Matrices, Consumer Buying Process, DMU, CSR & Web3/DAO.
            - Performance Mgmt (Units 13–14): EPM Control Loop, CSFs vs KPIs, Budgetary Control, Balanced Scorecard (4 Perspectives & Disadvantages), Fitzgerald & Moon's Building Block Model, Lynch & Cross's Performance Pyramid.

            Capabilities & Coaching Style:
            - Bilingual Coach: Respond warmly in Roman Urdu / Urdu mixed with crisp ICMA English keywords whenever the student writes or asks in Urdu (or requests Urdu explanations), and authoritative clear English when requested.
            - Adapt the daily office routine immediately when the student reports meetings, fatigue, or exam anxiety, offering stress recovery coaching and 5-mark/10-mark ICMA + ACCA/CIMA/ICAEW exam structures.
            """.trimIndent()
        )
    }

    private val tutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = tutorSystemInstruction
        )
    }

    private val tutorLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = tutorSystemInstruction
        )
    }

    private val mcqSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string("Clear, unambiguous multiple-choice question stem."),
                        "options" to Schema.array(
                            items = Schema.string("Distinct answer choice."),
                            description = "Exactly 4 answer choices."
                        ),
                        "correctIndex" to Schema.integer("Zero-based index (0 to 3) of the single best answer."),
                        "explanation" to Schema.string("Concise tutor explanation of why the correct choice is right and others are wrong.")
                    )
                ),
                description = "List of generated multiple-choice questions."
            )
        )
    )

    private val mcqModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mcqSchema
            }
        )
    }

    private val studyPlanSchema = Schema.obj(
        properties = mapOf(
            "tasks" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string("Actionable study task title."),
                        "topic" to Schema.string("Subject or module name."),
                        "dayLabel" to Schema.string("Short schedule label such as Day 1, Day 2, Today."),
                        "targetMinutes" to Schema.integer("Recommended study minutes between 15 and 90.")
                    )
                ),
                description = "5 structured study tasks."
            )
        )
    )

    private val planModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = studyPlanSchema
            }
        )
    }

    private val flashcardSchema = Schema.obj(
        properties = mapOf(
            "cards" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "front" to Schema.string("Clear active-recall prompt, term, or exam question under 220 characters."),
                        "back" to Schema.string("Concise, high-yield answer, definition, or numbered key points under 600 characters."),
                        "hint" to Schema.string("Optional short memory cue without giving away the full answer.")
                    )
                ),
                description = "List of active-recall flashcards derived from the study material."
            )
        )
    )

    private val flashcardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = flashcardSchema
            }
        )
    }

    fun streamTutorResponse(
        prompt: String,
        history: List<Content>,
        attachedImage: Bitmap? = null,
        useLiteModel: Boolean = false
    ): Flow<String> = flow {
        val fullContents = buildList {
            addAll(history.takeLast(6))
            add(
                content("user") {
                    if (attachedImage != null) {
                        image(attachedImage)
                    }
                    text(prompt)
                }
            )
        }
        val activeModel = if (useLiteModel && attachedImage == null) tutorLiteModel else tutorModel
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                activeModel.generateContentStream(fullContents).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Tutor returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Tutor stream timed out", e)
            throw IllegalStateException("AI Tutor request timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Tutor streaming failed", e)
            throw e
        }
    }

    fun streamSelectionTutorAction(
        action: ReaderAiAction,
        materialTitle: String,
        materialCategory: String,
        selectedText: String,
        pageLabel: String? = null,
        pageBitmap: Bitmap? = null
    ): Flow<String> = flow {
        val clippedSelection = selectedText.trim().take(4500)
        val sourceLabel = if (pageLabel != null) {
            "Study Material: \"$materialTitle\" ($materialCategory) — $pageLabel"
        } else {
            "Study Material: \"$materialTitle\" ($materialCategory)"
        }
        val prompt = when (action) {
            ReaderAiAction.SUMMARIZE_BULLETS -> buildString {
                appendLine(sourceLabel)
                if (clippedSelection.isNotBlank()) {
                    appendLine("Selected passage:")
                    appendLine("\"\"\"")
                    appendLine(clippedSelection)
                    appendLine("\"\"\"")
                } else {
                    appendLine("Analyze the attached study material page image carefully.")
                }
                appendLine()
                appendLine("Task: Convert this content into crisp, high-yield summarized bullet-point study notes.")
                appendLine("Format clearly with:")
                appendLine("- **Core Idea** (1 sentence)")
                appendLine("- **Key Bullet Notes** (4 to 7 concise, exam-ready bullets capturing definitions, steps, or frameworks)")
                appendLine("- **Key Terms / Formulas** (if any appear)")
                appendLine("- **Quick Memory Hook** (1 short takeaway for exams)")
            }
            ReaderAiAction.EXPLAIN_CONCEPT -> buildString {
                appendLine(sourceLabel)
                if (clippedSelection.isNotBlank()) {
                    appendLine("Selected concept or passage:")
                    appendLine("\"\"\"")
                    appendLine(clippedSelection)
                    appendLine("\"\"\"")
                } else {
                    appendLine("Analyze the complex concept shown on the attached study material page image.")
                }
                appendLine()
                appendLine("Task: Explain the complex concept(s) in this selection clearly so a student grasps it immediately.")
                appendLine("Structure your explanation with:")
                appendLine("- **Plain-English Meaning** (intuitive 2-sentence explanation)")
                appendLine("- **Step-by-Step Breakdown** (bullet points unpacking jargon, relationships, or mechanisms)")
                appendLine("- **Real-World Example** (concrete practical illustration)")
                appendLine("- **Exam Watch-Out** (common misconception or distinction examiners test)")
            }
        }

        val requestContent = content("user") {
            if (pageBitmap != null) {
                image(pageBitmap)
            }
            text(prompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                tutorModel.generateContentStream(requestContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Tutor returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Selection AI stream timed out", e)
            throw IllegalStateException("AI Tutor request timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Selection AI streaming failed", e)
            throw e
        }
    }

    suspend fun generateMcqs(
        topic: String,
        studyNotes: String,
        questionCount: Int
    ): List<McqQuestion> {
        val safeCount = questionCount.coerceIn(3, 15)
        val prompt = buildString {
            appendLine("Create $safeCount high-quality, exam-grade multiple-choice questions.")
            appendLine("Topic: $topic")
            if (studyNotes.isNotBlank()) {
                appendLine("Base the questions closely on these study notes / context:")
                appendLine(studyNotes.take(6000))
            }
            appendLine("Each question must have exactly 4 distinct options, a valid correctIndex (0..3), and a helpful tutor explanation.")
        }

        try {
            val response = withTimeout(60_000L) {
                mcqModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("MCQ generator returned an empty response.")

            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("questions")
            val result = mutableListOf<McqQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val qText = obj.getString("question").trim()
                val optionsArr = obj.getJSONArray("options")
                val options = (0 until optionsArr.length()).map { optionsArr.getString(it).trim() }
                if (qText.isNotEmpty() && options.size >= 2) {
                    val idx = obj.optInt("correctIndex", 0).coerceIn(0, options.lastIndex)
                    val explanation = obj.optString("explanation", "Review the core concept for this topic.")
                    result.add(
                        McqQuestion(
                            id = "ai_q_${UUID.randomUUID().toString().take(8)}",
                            question = qText,
                            options = options,
                            correctIndex = idx,
                            explanation = explanation
                        )
                    )
                }
            }
            if (result.isEmpty()) {
                throw IllegalStateException("No valid MCQs were parsed from the response.")
            }
            return result
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "MCQ generation timed out", e)
            throw IllegalStateException("MCQ generation timed out after 60 seconds. Try fewer questions or shorter notes.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "MCQ generation failed", e)
            throw e
        }
    }

    suspend fun generateStudyPlan(
        goal: String,
        materialsSummary: String
    ): List<GeneratedPlanTask> {
        val prompt = """
            Build a focused 5-step study plan for a student whose goal is: "$goal".
            Available study materials:
            $materialsSummary
            Generate 5 concrete, sequential tasks spanning Today through Day 4, mixing reading, video clip review, and MCQ testing.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                planModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Study plan generator returned an empty response.")
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("tasks")
            val tasks = mutableListOf<GeneratedPlanTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = obj.optString("title", "").trim()
                if (title.isNotEmpty()) {
                    tasks.add(
                        GeneratedPlanTask(
                            title = title.take(140),
                            topic = obj.optString("topic", "Core Module").take(120),
                            dayLabel = obj.optString("dayLabel", "Day ${i + 1}").take(40),
                            targetMinutes = obj.optLong("targetMinutes", 30L).coerceIn(15L, 90L)
                        )
                    )
                }
            }
            if (tasks.isEmpty()) {
                throw IllegalStateException("Could not generate study plan tasks.")
            }
            return tasks
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Study plan generation timed out", e)
            throw IllegalStateException("Study plan generation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Study plan generation failed", e)
            throw e
        }
    }

    suspend fun generateFlashcards(
        deckTitle: String,
        sourceContent: String,
        cardCount: Int
    ): List<GeneratedFlashcard> {
        val safeCount = cardCount.coerceIn(3, 15)
        val prompt = buildString {
            appendLine("Create $safeCount crisp, high-retention active-recall flashcards.")
            appendLine("Deck / Reading Material: $deckTitle")
            if (sourceContent.isNotBlank()) {
                appendLine("Ground the flashcards directly in this reading material context:")
                appendLine(sourceContent.take(7000))
            }
            appendLine("Keep each front prompt concise and testable, each back answer structured and accurate, and provide a short helpful hint.")
        }

        try {
            val response = withTimeout(60_000L) {
                flashcardModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Flashcard generator returned an empty response.")
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("cards")
            val cards = mutableListOf<GeneratedFlashcard>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val front = obj.optString("front", "").trim().take(500)
                val back = obj.optString("back", "").trim().take(1500)
                val hint = obj.optString("hint", "").trim().take(300)
                if (front.isNotEmpty() && back.isNotEmpty()) {
                    cards.add(
                        GeneratedFlashcard(
                            front = front,
                            back = back,
                            hint = hint
                        )
                    )
                }
            }
            if (cards.isEmpty()) {
                throw IllegalStateException("No valid flashcards could be parsed from the AI response.")
            }
            return cards
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Flashcard generation timed out", e)
            throw IllegalStateException("Flashcard generation timed out after 60 seconds. Try fewer cards.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Flashcard generation failed", e)
            throw e
        }
    }

    suspend fun generateDailyBrief(plan: DailyBriefPlan, weakTopics: List<String>): String {
        val prompt = """
            Speak as a warm 30-Year Corporate Finance Strategist + 50-Year Master Teacher in easy Roman Urdu mixed with clear ICMA English exam keywords.
            Student starts from ZERO preparation for the ICMA M3 Organizational Management (ML-1) exam on 5 December 2026 (${plan.daysToExam} days left).
            Energy mode: ${plan.energyMode.label}. Weak topics queued: ${weakTopics.joinToString(", ").ifBlank { "None" }}.
            Deliver today's energetic spoken briefing covering:
            1) Total study hours today (${"%.1f".format(plan.totalStudyMinutes / 60.0)} hrs / ${plan.totalStudyMinutes} min)
            2) 08:00 AM morning topic (${plan.todayTopic}) + video clip
            3) 12:45 PM - 01:15 PM Office Lunch Break: Short Test during Office
            4) 03:30 PM Break 2 micro-drill
            5) 05:30 PM Home Tired Recovery: Today's 5 Flashcards only
            6) 07:00 PM Fresh Study + 15m recovery break + 08:15 PM Test of the Day (60% MCQ / 40% Typing) + 11:30 PM rest
            7) Advance Mock alert: ${plan.upcomingMockAlert}
        """.trimIndent()
        return withTimeout(60_000L) {
            tutorLiteModel.generateContent(prompt).text?.trim() ?: plan.urduSpokenBrief
        }
    }

    suspend fun gradeTypingAnswer(question: TypingQuestion, userAnswer: String): String {
        val prompt = """
            Grade this student's ${question.marks}-mark ICMA OM (ML-1) typed exam answer like a strict Pakistan Certificate of Merit examiner. Respond in warm, encouraging Roman Urdu + crisp English exam keywords.
            Question (${question.sourceBenchmark}): ${question.question}
            Required Keywords: ${question.keywords.joinToString(", ")}
            Official Model Answer:
            ${question.modelAnswer}

            Student's Typed Answer:
            $userAnswer

            Provide:
            1) Score out of ${question.marks}
            2) What was strong vs missing keywords
            3) 3-line Certificate of Merit improvement tip
        """.trimIndent()
        return withTimeout(60_000L) {
            tutorModel.generateContent(prompt).text?.trim()
                ?: throw IllegalStateException("AI Grader returned empty feedback.")
        }
    }

    private companion object {
        const val TAG = "AiTutorRepo"
    }
}
