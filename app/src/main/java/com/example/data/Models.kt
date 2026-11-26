package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MaterialType(val label: String) {
    READING("Reading"),
    VIDEO_CLIP("Video Clip"),
    DRIVE_FOLDER("Drive Folder"),
    GOOGLE_DOC("Google Doc"),
    NOTES("Study Notes");

    companion object {
        fun fromRaw(raw: String): MaterialType =
            entries.firstOrNull { it.name == raw } ?: READING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Student",
    val studyGoal: String = "OM M3 Certificate of Merit (5 Dec 2026)",
    val dailyTargetMinutes: Long = 180L,
    val totalStudyMinutes: Long = 0L,
    val completedSessions: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyMaterial(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val materialType: String = MaterialType.READING.name,
    val sourceUrl: String = "",
    val contentMarkdown: String = "",
    val category: String = "General",
    val estimatedMinutes: Long = 20L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val preloaded: Boolean = false,
    val assetPath: String = ""
) {
    fun resolvedType(): MaterialType = MaterialType.fromRaw(materialType)
    val isBundledPdf: Boolean get() = assetPath.isNotBlank()
}

data class PdfChapterJump(
    val title: String,
    val pageNumber: Int // 1-indexed PDF page number
)

data class McqBank(
    val id: String,
    val title: String,
    val subtitle: String,
    val questions: List<McqQuestion>
)

@Keep
@IgnoreExtraProperties
data class MaterialProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val materialId: String = "",
    val completed: Boolean = false,
    val personalNotes: String = "",
    val minutesSpent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyPlanItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val topic: String = "",
    val dayLabel: String = "Today",
    val targetMinutes: Long = 30L,
    val completed: Boolean = false,
    val linkedMaterialId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topic: String = "",
    val totalQuestions: Long = 0L,
    val correctCount: Long = 0L,
    val durationSeconds: Long = 0L,
    val fullScreenUsed: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val accuracyPercent: Int
        get() = if (totalQuestions > 0L) ((correctCount * 100L) / totalQuestions).toInt() else 0
}

data class McqQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class QuizSessionState(
    val topic: String = "",
    val questions: List<McqQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isSubmitted: Boolean = false,
    val elapsedSeconds: Int = 0
) {
    val correctCount: Int
        get() = questions.indices.count { idx -> selectedAnswers[idx] == questions[idx].correctIndex }

    val accuracyPercent: Int
        get() = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0
}

data class TutorMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val hasAttachment: Boolean = false
)

enum class TutorEngineMode(val label: String, val subtitle: String) {
    GEMINI_BUILTIN(
        label = "Gemini 3.8 Flash",
        subtitle = "$0 Primary • Multimodal + Streaming"
    ),
    GEMINI_LITE(
        label = "Gemini 3.5 Lite",
        subtitle = "$0 High-Quota • Fast Token Saver"
    ),
    POLLINATIONS_FREE(
        label = "Pollinations Free",
        subtitle = "$0 Open Text API • Zero Key"
    )
}

data class FreeReferenceResult(
    val sourceName: String,
    val title: String,
    val subtitle: String,
    val summary: String,
    val sourceUrl: String
)

@Keep
@IgnoreExtraProperties
data class Flashcard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val materialId: String = "",
    val deckTitle: String = "",
    val front: String = "",
    val back: String = "",
    val hint: String = "",
    val mastered: Boolean = false,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class PortalTab(val label: String) {
    DASHBOARD("Home"),
    MATERIALS("Materials"),
    FLASHCARDS("Cards"),
    MCQS("MCQs"),
    TUTOR("AI Tutor")
}

enum class PomodoroPhase(val label: String) {
    WORK("Focus Work"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
    val completedWorkSessions: Int = 0,
    val remainingSeconds: Int = 25 * 60,
    val totalPhaseSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val autoAdvance: Boolean = true
) {
    val progressFraction: Float
        get() = if (totalPhaseSeconds > 0) {
            ((totalPhaseSeconds - remainingSeconds).toFloat() / totalPhaseSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }
}

enum class ReaderAiAction(val label: String, val shortLabel: String) {
    SUMMARIZE_BULLETS("Summarized Bullet-Point Notes", "Bullet Notes"),
    EXPLAIN_CONCEPT("Complex Concept Explanation", "Explain Concept")
}

data class ReaderAiSessionState(
    val isVisible: Boolean = false,
    val action: ReaderAiAction = ReaderAiAction.SUMMARIZE_BULLETS,
    val materialId: String = "",
    val materialTitle: String = "",
    val materialCategory: String = "",
    val selectedSnippet: String = "",
    val pageLabel: String? = null,
    val hasPageImage: Boolean = false,
    val responseText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val savedToNotes: Boolean = false
)

sealed interface FullScreenMode {
    data class ReadingViewer(
        val material: StudyMaterial,
        val openAsVideo: Boolean = false
    ) : FullScreenMode

    data class McqExam(
        val topic: String,
        val questions: List<McqQuestion>
    ) : FullScreenMode

    data class TutorStudio(
        val initialPrompt: String? = null
    ) : FullScreenMode

    data object PomodoroFocus : FullScreenMode

    data object RoadmapFocus : FullScreenMode

    data class FlashcardDeck(
        val initialDeckFilter: String? = null
    ) : FullScreenMode
}

object DriveUrlParser {
    private val fileIdRegex = Regex("/file/d/([a-zA-Z0-9_-]+)")
    private val docIdRegex = Regex("docs\\.google\\.com/document/d/([a-zA-Z0-9_-]+)")
    private val folderIdRegex = Regex("/folders/([a-zA-Z0-9_-]+)")

    fun toEmbedUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        docIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { docId ->
            return "https://docs.google.com/document/d/$docId/preview"
        }
        fileIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            return "https://drive.google.com/file/d/$fileId/preview"
        }
        folderIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { folderId ->
            return "https://drive.google.com/embeddedfolderview?id=$folderId#list"
        }
        return trimmed
    }

    fun inferTypeFromUrl(rawUrl: String): MaterialType {
        val lower = rawUrl.lowercase()
        return when {
            lower.contains("docs.google.com/document") -> MaterialType.GOOGLE_DOC
            lower.contains("/folders/") -> MaterialType.DRIVE_FOLDER
            lower.endsWith(".mp4") || lower.contains("youtube.com") || lower.contains("youtu.be") -> MaterialType.VIDEO_CLIP
            lower.contains("drive.google.com/file") -> MaterialType.READING
            else -> MaterialType.READING
        }
    }
}

object PreloadedMaterials {
    const val ASSET_OM_BOOK = "M3_Organizational_Management.pdf"
    const val ASSET_OM_PAST_PAPERS = "Past_Papers_OM-ML1.pdf"

    val omBookChapters: List<PdfChapterJump> = listOf(
        PdfChapterJump("Cover & Syllabus Overview", 1),
        PdfChapterJump("Unit 1: Enterprise Management, PESTEL & ESG", 8),
        PdfChapterJump("Unit 1: Practice MCQs", 16),
        PdfChapterJump("Unit 2: Operations Mgmt & Mintzberg", 19),
        PdfChapterJump("Unit 2: Practice MCQs", 28),
        PdfChapterJump("Unit 3: Production Techniques & 3D Printing", 30),
        PdfChapterJump("Unit 3: Practice MCQs", 35),
        PdfChapterJump("Unit 4: Plant Maintenance, 5S & TPM / OEE", 37),
        PdfChapterJump("Unit 4: Practice MCQs", 46),
        PdfChapterJump("Unit 5: Production Planning, MRP, ERP & OPT", 49),
        PdfChapterJump("Unit 6: Managing Operational Capacity & Queuing", 64),
        PdfChapterJump("Unit 7: Quality, Six Sigma, Kaizen, Lean & ISO", 68),
        PdfChapterJump("Unit 7: Practice Questions & SERVQUAL", 81),
        PdfChapterJump("Unit 8: Human Resource Management & Theories", 87),
        PdfChapterJump("Unit 9: Reliable HR (Pfeffer) & Kolb Learning", 112),
        PdfChapterJump("Unit 9: Practice MCQs", 116),
        PdfChapterJump("Unit 10: Marketing Concept, STP & Virtual Branding", 120),
        PdfChapterJump("Unit 11: Marketing Plans, 7Ps, BCG & Ansoff", 132),
        PdfChapterJump("Unit 11: Practice MCQs & Answers", 184),
        PdfChapterJump("Unit 12: Consumer Behavior, CSR & Web3/DAO", 197),
        PdfChapterJump("Unit 13: Enterprise Performance Mgmt & Budgets", 215),
        PdfChapterJump("Unit 13: Practice MCQs", 228),
        PdfChapterJump("Unit 14: Balanced Scorecard, Building Block & Pyramid", 230),
        PdfChapterJump("Unit 14: Practice MCQs", 248)
    )

    val pastPaperChapters: List<PdfChapterJump> = listOf(
        PdfChapterJump("Page 1: Q1 Marketing Concept, Q2 HRM, Q3 Agencies, Q4 BSC", 1),
        PdfChapterJump("Page 2: Q4 BSC Drawbacks (cont.) & Q5 Sales Orientation", 2),
        PdfChapterJump("Page 3: Q6 Mintzberg, Q7 Budgetary Control, Q8 Interviews", 3),
        PdfChapterJump("Page 4: Q8 (cont.), Q9 Quality Circles, Q10 MRP Benefits", 4)
    )

    fun chapterJumpsFor(assetPath: String): List<PdfChapterJump> = when (assetPath) {
        ASSET_OM_BOOK -> omBookChapters
        ASSET_OM_PAST_PAPERS -> pastPaperChapters
        else -> emptyList()
    }

    val items: List<StudyMaterial> = listOf(
        StudyMaterial(
            id = "om_study_text_2026",
            title = "M3 Organizational Management — Study Text 2026",
            description = "Complete 254-page ICMA Pakistan M3 Organizational Management textbook (Units 1–14) bundled offline for full-screen reading & chapter jump.",
            materialType = MaterialType.READING.name,
            sourceUrl = "",
            assetPath = ASSET_OM_BOOK,
            contentMarkdown = """
                ### ICMA Pakistan — M3 Organizational Management (2026 Edition)
                **254 Pages • 4 Core Domains • 14 Study Units (Bundled Offline PDF)**

                Use the **PDF Book** toggle above to read every page natively in Full-Screen Mode, or jump straight to any Unit using the **Chapters** button.

                #### Syllabus Breakdown:
                - **Part A — Operations Management (Units 1–7):**
                  - Unit 1 (p.8): Enterprise Management, PESTEL, Digital Transformation & ESG
                  - Unit 2 (p.19): Operations Mgmt, Mintzberg's 5 Organizational Parts & Configurations
                  - Unit 3 (p.30): Production Systems (Job, Batch, Process/Flow, Additive Manufacturing)
                  - Unit 4 (p.37): Plant Maintenance, 5S, TPM (8 Pillars) & OEE (Availability × Performance × Quality)
                  - Unit 5 (p.49): Production Planning & Control, MRP, MRP-II, ERP & OPT (Drum-Buffer-Rope)
                  - Unit 6 (p.64): Managing Operational Capacity, FMS, Forecasting & Queuing Theory
                  - Unit 7 (p.68): Quality Management (Juran COQ, Six Sigma DMAIC, Kaizen, Lean/Kanban, TQMEX, ISO 9000/14000/27000/45000/42001/56005)
                - **Part B — Human Capital Management (Units 8–9):**
                  - Unit 8 (p.87): HRM Importance, Motivation (Taylor, Maslow, Vroom, Herzberg, McGregor X/Y), Handy's Shamrock & Psychological Contract, 70-20-10 Training & Algorithmic Bias
                  - Unit 9 (p.112): Pfeffer's 7 Reliable HR Practices, HRD & Kolb's Experiential Learning Cycle
                - **Part C — Marketing Management (Units 10–12):**
                  - Unit 10 (p.120): Marketing Orientations, STP, Virtual/Metaverse Branding & Micro-Influencers
                  - Unit 11 (p.132): 4Ps/7Ps Services Mix, Product Portfolio (BCG), Pricing, Branding & Ansoff Matrix
                  - Unit 12 (p.197): Consumer Buying Behavior, DMU Roles, B2B vs B2C, Internal/Social/NFP Marketing, Web3 & DAO
                - **Part D — Performance Management (Units 13–14):**
                  - Unit 13 (p.215): Enterprise Performance Management, Control Loop, Ratios, CSFs vs KPIs & Budgetary Control
                  - Unit 14 (p.230): Balanced Scorecard (Kaplan & Norton), Building Block Model (Fitzgerald & Moon), Performance Pyramid (Lynch & Cross) & Predictive Analytics
            """.trimIndent(),
            category = "OM Book (ICMA M3)",
            estimatedMinutes = 90L,
            preloaded = true
        ),
        StudyMaterial(
            id = "om_past_papers_ml1",
            title = "OM ML-1 Past Papers & Suggested Solutions",
            description = "10 descriptive exam questions (5 marks each) with official ICMA suggested solutions covering Marketing, HRM, BSC, Mintzberg, Quality Circles & MRP.",
            materialType = MaterialType.NOTES.name,
            sourceUrl = "",
            assetPath = ASSET_OM_PAST_PAPERS,
            contentMarkdown = """
                ### Descriptive Past Paper Questions & Suggested Solutions (OM — ML1)
                *(4 Pages • 10 Tested Questions • 5 Marks Each — Switch to **PDF Book** view for the original formatted sheets)*

                **Q.1 Define the marketing concept. (05)**
                A management orientation accepting that the key task of the organization is to determine the needs, wants, and values of a target market and adapt the organization to delivering desired satisfaction more effectively and efficiently than competitors.

                **Q.2 Importance of the HRM process & external influences. (05)**
                - *3 Reasons:* (1) Source of competitive advantage; (2) Vital part of organizational strategies; (3) Impacts organizational performance through how people are treated.
                - *4 External Factors:* Economy, labor unions, legal environment, and demographic trends.

                **Q.3 Why outsource recruitment to private employment agencies instead of in-house? (05)**
                1. HR may lack trained staff for media ads & screening.
                2. Avoids alerting competitors to expansion plans.
                3. Cost-effective screening of overwhelming applicant volume.
                4. Prevents media ads from signaling high employee turnover.
                5. Agencies maintain ready candidate databanks for urgent hires.
                6. Confidentiality for senior-level personnel exploring a move via headhunters.

                **Q.4 Disadvantages of using a Balanced Scorecard. (05)**
                - Difficult to record & process qualitative (non-financial) data.
                - Information overload from choosing too many measures.
                - Potential conflict between measures (e.g., cutting R&D boosts short-term profit).
                - Chosen measures may misalign with strategy/vision.
                - Poor communication if managers are still rewarded solely on narrow financial metrics.
                - Lack of senior management commitment causes failure; high setup/measurement cost; focuses mainly on strategic level.

                **Q.5 Organizations using sales orientation & drawbacks. (05)**
                - *Examples (Any 2):* Retailers (especially FMCG), Direct Sales/MLM companies, Telemarketing/Call centers, Real estate agencies.
                - *Drawbacks (Any 3):* Customer dissatisfaction, short-term focus, lack of innovation, limited customer insight, high sales-staff turnover.

                **Q.6 Explain Mintzberg's Effective Organization (5 Parts). (05)**
                1. **Operating core:** Workers directly producing the product or delivering the service.
                2. **Strategic apex:** Top executives formulating/implementing strategy to serve owners.
                3. **Middle line:** Chain of hierarchy from strategic apex down to frontline supervisors.
                4. **Technostructure:** Analysts standardizing processes, outputs & skills (e.g., HR, accountants).
                5. **Support staff:** Indirect assistance outside operational workflow (e.g., catering, legal, PR).

                **Q.7 How budgetary controls help earn maximum profit? (05)**
                Controls costs and improves efficiency; measures department/individual/cost-center performance; identifies cost-reduction areas; supports performance incentives; aligns inter-departmental coordination; enables variance analysis & corrective action toward long-term goals.

                **Q.8 General limitations of interviews. (05)**
                - Unreliable assessments (interviewers disagree).
                - Poor predictive validity of actual job performance.
                - *Judgment errors:* (i) Halo effect, (ii) Contagious bias (leading cues), (iii) Stereotyping (dress/accent/voice), (iv) Misjudging abstract traits (honesty/motivation), (v) Logical error.

                **Q.9 Define Quality Circle and explain its advantages. (05)**
                A small voluntary group of employees with mixed skills from all levels meeting regularly to solve real quality problems. *Advantages:* Higher quality, customer satisfaction & productivity; continuous improvement culture; operational staff understand shop-floor quality issues best; fosters organizational unity.

                **Q.10 Benefits of Material Requirement Planning (MRP). (05)**
                Improved ability to meet customer orders; reduced stock holding; computerized schedule updates quickly when demand shifts; early warning of purchasing/production bottlenecks; closer supplier relationships.
            """.trimIndent(),
            category = "Past Papers (ICMA ML1)",
            estimatedMinutes = 40L,
            preloaded = true
        ),
        StudyMaterial(
            id = "drive_folder_1",
            title = "Core Study Folder 1",
            description = "Primary Google Drive folder containing core course modules, readings, and study handouts.",
            materialType = MaterialType.DRIVE_FOLDER.name,
            sourceUrl = "https://drive.google.com/drive/folders/1JRAADz_hSw7MLr4_Pl6fuhuyRhlyt3ex",
            contentMarkdown = "### Core Study Folder 1\nOpen this folder in full-screen view to browse all attached reading PDFs, lecture notes, and reference files.\n\n**Study Tip:** After reviewing a file inside the folder, switch to the **AI Tutor** or **MCQs** tab to generate instant practice questions on what you just studied.",
            category = "Drive Collection 1",
            estimatedMinutes = 35L,
            preloaded = true
        ),
        StudyMaterial(
            id = "drive_file_2",
            title = "Primary Reading & Notes (File 2)",
            description = "Direct Google Drive study file ready for full-screen reading or video clip playback.",
            materialType = MaterialType.READING.name,
            sourceUrl = "https://drive.google.com/file/d/1C976hWaHp0S8gjrjMIe5M64p1IkmJ1JW/view?usp=drivesdk",
            contentMarkdown = "### Primary Reading & Notes (File 2)\nUse **Full-Screen Mode** to read without distractions, or click **Play as Clip** if this Drive item contains a recorded lecture or visual clip.\n\nYou can also save personal highlights in the notes drawer and generate a 5-question MCQ check right after finishing.",
            category = "Reading Material",
            estimatedMinutes = 25L,
            preloaded = true
        ),
        StudyMaterial(
            id = "drive_file_3",
            title = "Lecture Clip & Visual File (File 3)",
            description = "Google Drive study media item — supports full-screen video clip playback and full-screen document preview.",
            materialType = MaterialType.VIDEO_CLIP.name,
            sourceUrl = "https://drive.google.com/file/d/1bKnRRG5h6hCmga7EBbN37vGbxiuMSvcS/view?usp=drivesdk",
            contentMarkdown = "### Lecture Clip & Visual File (File 3)\nClick **Full-Screen Clip** to launch the immersive Drive media player with system bars hidden.\n\nPause at key timestamps to ask the **AI Tutor** for deeper explanations or worked examples.",
            category = "Video & Visuals",
            estimatedMinutes = 20L,
            preloaded = true
        ),
        StudyMaterial(
            id = "drive_folder_4",
            title = "Video Clips & Lectures Folder 4",
            description = "Google Drive folder archive for recorded lecture clips, walkthroughs, and multimedia explanations.",
            materialType = MaterialType.DRIVE_FOLDER.name,
            sourceUrl = "https://drive.google.com/drive/folders/1trJJR26p772Twd0O7hFuzl-uRCtqDRzx",
            contentMarkdown = "### Video Clips & Lectures Folder 4\nBrowse all video clips and lecture materials inside this folder in distraction-free full-screen view.",
            category = "Video Archive",
            estimatedMinutes = 40L,
            preloaded = true
        ),
        StudyMaterial(
            id = "drive_folder_5",
            title = "Practice Sets & Bank Folder 5",
            description = "Google Drive folder with past questions, reference sheets, and revision packs.",
            materialType = MaterialType.DRIVE_FOLDER.name,
            sourceUrl = "https://drive.google.com/drive/folders/1cSJry6g8mBLEz0slwzLHyz-UyiGkvfXM",
            contentMarkdown = "### Practice Sets & Bank Folder 5\nReview revision packs in full-screen, then test your retention in the **Full-Screen MCQ Arena**.",
            category = "Practice Bank",
            estimatedMinutes = 30L,
            preloaded = true
        ),
        StudyMaterial(
            id = "google_doc_6",
            title = "Master Study Guide (Google Doc 6)",
            description = "Live Google Document study guide with structured reading preview and AI Tutor integration.",
            materialType = MaterialType.GOOGLE_DOC.name,
            sourceUrl = "https://docs.google.com/document/d/1wZP3HFOlBIjIZJzw82Z_-yT7yeuuYfZYh3IVE3G3NJQ/edit?usp=drivesdk",
            contentMarkdown = "### Master Study Guide (Google Doc 6)\nOpens in **Full-Screen Reader Mode** using Google Docs preview. If the document requires Google account permissions, sign in inside the WebView or use **Open in Docs/Browser** from the top bar.",
            category = "Master Guide",
            estimatedMinutes = 30L,
            preloaded = true
        ),
        StudyMaterial(
            id = "drive_folder_extra",
            title = "OM Supplementary Notes Folder 7",
            description = "Additional OM Google Drive study folder with supplemental notes and exam handouts.",
            materialType = MaterialType.DRIVE_FOLDER.name,
            sourceUrl = "https://drive.google.com/drive/folders/1EdfluOCsZ_NUiRZE_oJRT9eXiK4d4aT3",
            contentMarkdown = "### OM Supplementary Notes Folder 7\nOpen this Google Drive folder in full-screen or browser to review supplemental OM chapters, notes, and handouts.",
            category = "Drive Collection 2",
            estimatedMinutes = 30L,
            preloaded = true
        )
    )
}

object OmMcqBanks {

    private val unit1To4Questions = listOf(
        McqQuestion(
            id = "om_u1_1",
            question = "In PESTEL analysis, which factor primarily evaluates carbon-emission regulations, waste management rules, and climate risk?",
            options = listOf("Economic", "Technological", "Environmental", "Legal"),
            correctIndex = 2,
            explanation = "The Environmental dimension in PESTEL examines ecological impact, sustainability, carbon regulations, and climate considerations."
        ),
        McqQuestion(
            id = "om_u2_1",
            question = "According to Mintzberg's Effective Organization, which part consists of HR analysts and management accountants who standardize processes and outputs?",
            options = listOf("Operating Core", "Strategic Apex", "Technostructure", "Support Staff"),
            correctIndex = 2,
            explanation = "Mintzberg's Technostructure standardizes work processes, outputs, and skills—including accountants and HR planners—outside the direct line of authority."
        ),
        McqQuestion(
            id = "om_u2_2",
            question = "Which Mintzberg component represents the hierarchy of authority linking the strategic apex down to frontline supervisors?",
            options = listOf("Support Staff", "Middle Line", "Operating Core", "Technostructure"),
            correctIndex = 1,
            explanation = "The Middle Line connects top executives (Strategic Apex) to operational workers (Operating Core) through formal managerial hierarchy."
        ),
        McqQuestion(
            id = "om_u3_1",
            question = "A luxury yacht builder constructing each vessel to a unique buyer specification uses which production technique?",
            options = listOf("Continuous Process Production", "Batch Production", "Job Production", "Flow Assembly"),
            correctIndex = 2,
            explanation = "Job production produces unique, customized one-off items tailored to individual customer orders (e.g., custom yachts, tailored suits, bridges)."
        ),
        McqQuestion(
            id = "om_u3_2",
            question = "Which modern manufacturing method builds 3D parts layer-by-layer from a digital CAD model, minimizing material waste?",
            options = listOf("Subtractive CNC Milling", "Additive Manufacturing (3D Printing)", "Batch Stamping", "Die Casting"),
            correctIndex = 1,
            explanation = "Additive manufacturing (3D printing) deposits material layer by layer from digital files, ideal for rapid prototyping and low-waste complex geometries."
        ),
        McqQuestion(
            id = "om_u4_1",
            question = "How is Overall Equipment Effectiveness (OEE) calculated in Total Productive Maintenance (TPM)?",
            options = listOf(
                "Availability + Performance + Quality",
                "Availability × Performance × Quality",
                "Uptime ÷ Total Downtime",
                "Good Units × Machine Speed"
            ),
            correctIndex = 1,
            explanation = "OEE is the product of three ratios: Availability × Performance × Quality, capturing losses from breakdowns, slow cycles, and defects."
        ),
        McqQuestion(
            id = "om_u4_2",
            question = "In the 5S workplace methodology, what does 'Seiso' (Shine) focus on?",
            options = listOf(
                "Separating needed tools from unnecessary clutter",
                "Cleaning and inspecting the workspace regularly to spot abnormalities early",
                "Writing standardized shift checklists",
                "Auditing external suppliers"
            ),
            correctIndex = 1,
            explanation = "5S comprises Seiri (Sort), Seiton (Set in order), Seiso (Shine/Clean & Inspect), Seiketsu (Standardize), and Shitsuke (Sustain)."
        )
    )

    private val unit5To7Questions = listOf(
        McqQuestion(
            id = "om_u5_1",
            question = "What is the key mechanism of Optimized Production Technology (OPT) for scheduling factory workflow?",
            options = listOf(
                "Maximizing idle inventory at every workstation",
                "Drum-Buffer-Rope scheduling anchored around bottleneck capacity",
                "Eliminating all preventive maintenance checks",
                "Manual weekly paper stocktaking"
            ),
            correctIndex = 1,
            explanation = "OPT (Theory of Constraints) sets the pace via the bottleneck ('Drum'), protects it with inventory ('Buffer'), and paces material release ('Rope')."
        ),
        McqQuestion(
            id = "om_u5_2",
            question = "Which of the following is a primary benefit of Material Requirement Planning (MRP) highlighted in ICMA Past Papers?",
            options = listOf(
                "Increased safety stock holding costs",
                "Elimination of supplier relationships",
                "Early warning of purchasing or production bottlenecks and reduced stock holding",
                "Fixed annual schedules that cannot respond to demand changes"
            ),
            correctIndex = 2,
            explanation = "Computerized MRP reduces stock holding, updates quickly when demand changes, improves order fulfillment, and warns early of supply/production bottlenecks."
        ),
        McqQuestion(
            id = "om_u6_1",
            question = "How does Practical (Effective) Capacity differ from Design Capacity?",
            options = listOf(
                "Effective capacity exceeds theoretical maximum output",
                "Effective capacity adjusts design capacity for planned maintenance, setups, and shift realities",
                "Design capacity excludes machine specifications",
                "They are always identical in service industries"
            ),
            correctIndex = 1,
            explanation = "Design capacity is theoretical maximum output; Effective capacity deducts realistic allowances such as planned maintenance, changeover times, and staffing."
        ),
        McqQuestion(
            id = "om_u7_1",
            question = "What defect rate defines Six Sigma quality performance?",
            options = listOf(
                "340 defects per million opportunities",
                "3.4 defects per million opportunities (DPMO)",
                "3.4% defective units per batch",
                "60 defects per thousand units"
            ),
            correctIndex = 1,
            explanation = "Six Sigma targets near-perfection with no more than 3.4 defects per million opportunities (99.99966% defect-free), driven by DMAIC."
        ),
        McqQuestion(
            id = "om_u7_2",
            question = "In Juran's Cost of Quality (COQ) framework, expenses for inspection, testing, and quality audits fall under:",
            options = listOf("Internal Failure Costs", "External Failure Costs", "Appraisal Costs", "Prevention Costs"),
            correctIndex = 2,
            explanation = "Appraisal costs measure and inspect quality to detect defects before shipment, whereas Prevention stops defects from occurring in the first place."
        ),
        McqQuestion(
            id = "om_u7_3",
            question = "What best describes a Quality Circle?",
            options = listOf(
                "An external ISO certification audit team",
                "A small voluntary group of employees from all levels meeting regularly to solve real quality problems",
                "Top executives setting annual profit targets",
                "An automated statistical process control chart"
            ),
            correctIndex = 1,
            explanation = "Quality Circles empower operational staff—who often understand shop-floor quality issues better than managers—to collaborate on continuous improvement."
        ),
        McqQuestion(
            id = "om_u7_4",
            question = "In the TQMEX framework developed by S.K.M. Ho, which foundational step establishes workplace organization first?",
            options = listOf("ISO 9000", "5-S Practice", "BPR", "Balanced Scorecard"),
            correctIndex = 1,
            explanation = "TQMEX integrates 5-S, Business Process Re-engineering (BPR), Quality Control Circles (QCC), ISO 9000, and TPM leading into TQM."
        )
    )

    private val unit8To9Questions = listOf(
        McqQuestion(
            id = "om_u8_1",
            question = "According to Herzberg's Two-Factor Theory, which factor acts as a 'Hygiene Factor' rather than a true 'Motivator'?",
            options = listOf(
                "Recognition and achievement",
                "Challenging, meaningful work",
                "Company policy, salary, and working conditions",
                "Advancement and personal growth"
            ),
            correctIndex = 2,
            explanation = "Hygiene factors (pay, policies, job security, working conditions) prevent dissatisfaction when adequate, whereas motivators (achievement, recognition, growth) drive motivation."
        ),
        McqQuestion(
            id = "om_u8_2",
            question = "Charles Handy's 'Shamrock Organization' consists of which three workforce leaves?",
            options = listOf(
                "Strategic Apex, Technostructure, Support Staff",
                "Core Professionals, Contractual Fringe, Flexible Labor Force",
                "Full-time Managers, Trade Unions, Government Regulators",
                "Prevention, Appraisal, Failure"
            ),
            correctIndex = 1,
            explanation = "Handy's Shamrock model divides the workforce into (1) Professional Core, (2) Contractual Fringe (outsourced functions), and (3) Flexible Labor (part-time/temp)."
        ),
        McqQuestion(
            id = "om_u8_3",
            question = "In the 70-20-10 learning model, what does the '70%' represent?",
            options = listOf(
                "Formal classroom lectures and e-learning modules",
                "Challenging on-the-job experiences and hands-on tasks",
                "Peer mentoring and coaching feedback",
                "Annual HR compliance exams"
            ),
            correctIndex = 1,
            explanation = "The 70-20-10 model states roughly 70% of development comes from challenging job assignments, 20% from mentoring/social learning, and 10% from formal coursework."
        ),
        McqQuestion(
            id = "om_u8_4",
            question = "When an interviewer lets one standout trait (positive or negative) color their entire evaluation of a candidate, this limitation is called:",
            options = listOf("Logical Error", "Contagious Bias", "Halo Effect", "Predictive Validity"),
            correctIndex = 2,
            explanation = "The Halo Effect occurs when a single attribute dominates an interviewer's overall judgment of the candidate."
        ),
        McqQuestion(
            id = "om_u9_1",
            question = "Which is NOT one of Jeffrey Pfeffer's Seven Practices of Successful Organizations?",
            options = listOf(
                "Employment security and selective hiring",
                "Self-managed teams and decentralized decision-making",
                "Strictly confidential financial secrecy kept from all employees",
                "Reduced status distinctions and extensive training"
            ),
            correctIndex = 2,
            explanation = "Pfeffer advocates extensive sharing of financial and performance information throughout the organization, alongside security, selective hiring, teams, high contingent pay, training, and lower status barriers."
        ),
        McqQuestion(
            id = "om_u9_2",
            question = "What is the correct sequence of stages in Kolb's Experiential Learning Cycle?",
            options = listOf(
                "Abstract Conceptualization → Active Experimentation → Concrete Experience → Reflective Observation",
                "Concrete Experience → Reflective Observation → Abstract Conceptualization → Active Experimentation",
                "Reflective Observation → Concrete Experience → Active Experimentation → Abstract Conceptualization",
                "Active Experimentation → Reflective Observation → Concrete Experience → Abstract Conceptualization"
            ),
            correctIndex = 1,
            explanation = "Kolb's 4-stage cycle progresses from Concrete Experience (doing) → Reflective Observation (reviewing) → Abstract Conceptualization (concluding) → Active Experimentation (planning/testing)."
        )
    )

    private val unit10To12Questions = listOf(
        McqQuestion(
            id = "om_u10_1",
            question = "Which statement best defines the 'Marketing Concept' orientation?",
            options = listOf(
                "Maximize production volume to lower unit cost regardless of buyer taste",
                "Determine needs, wants, and values of a target market and deliver satisfaction more effectively than competitors",
                "Rely on aggressive personal selling to clear existing inventory",
                "Focus solely on engineering product features without market research"
            ),
            correctIndex = 1,
            explanation = "The marketing concept starts with target customer needs and delivers superior value and satisfaction more effectively and efficiently than rivals."
        ),
        McqQuestion(
            id = "om_u10_2",
            question = "Which drawback is most closely associated with a 'Sales Orientation'?",
            options = listOf(
                "Over-investing in long-term R&D innovation",
                "Short-term focus, customer dissatisfaction from pushy tactics, and high sales-staff turnover",
                "Excessive emphasis on deep customer insight research",
                "Refusal to use promotions or outbound calls"
            ),
            correctIndex = 1,
            explanation = "Sales orientation focuses on pushing existing stock via persuasive selling—risking dissatisfied buyers, short-termism, weak innovation, limited customer insight, and high staff turnover."
        ),
        McqQuestion(
            id = "om_u11_1",
            question = "Which three additional 'Ps' expand the traditional 4Ps into the 7Ps Services Marketing Mix?",
            options = listOf(
                "Profit, Purpose, Positioning",
                "People, Process, Physical Evidence",
                "Packaging, Publicity, Partnerships",
                "Planning, Pricing, Performance"
            ),
            correctIndex = 1,
            explanation = "Services add People (staff/service interaction), Process (delivery workflow), and Physical Evidence (tangible cues like environment or receipts) to Product, Price, Place, and Promotion."
        ),
        McqQuestion(
            id = "om_u11_2",
            question = "In the Ansoff Growth Matrix, launching a brand-new product into a completely new market represents:",
            options = listOf("Market Penetration", "Product Development", "Market Development", "Diversification"),
            correctIndex = 3,
            explanation = "Diversification combines new products with new markets, carrying the highest strategic risk among Ansoff's four growth options."
        ),
        McqQuestion(
            id = "om_u11_3",
            question = "In the BCG Growth-Share Matrix, a high market-share business unit operating in a slow-growth mature market is classified as a:",
            options = listOf("Star", "Question Mark", "Cash Cow", "Dog"),
            correctIndex = 2,
            explanation = "Cash Cows hold high relative market share in low-growth markets, generating strong cash surpluses to fund Stars and Question Marks."
        ),
        McqQuestion(
            id = "om_u12_1",
            question = "What is the first stage of the 5-stage Consumer Buying Decision Process?",
            options = listOf("Information Search", "Problem / Need Recognition", "Evaluation of Alternatives", "Post-Purchase Behavior"),
            correctIndex = 1,
            explanation = "Consumer decision-making begins with Need/Problem Recognition, followed by Information Search, Evaluation of Alternatives, Purchase Decision, and Post-Purchase Behavior."
        )
    )

    private val unit13To14Questions = listOf(
        McqQuestion(
            id = "om_u13_1",
            question = "Which best distinguishes a Critical Success Factor (CSF) from a Key Performance Indicator (KPI)?",
            options = listOf(
                "A CSF is a qualitative area the organization must excel in; a KPI is the quantifiable metric measuring progress on it",
                "CSFs are only financial while KPIs are always qualitative",
                "KPIs are set before strategy exists; CSFs are calculated daily by accountants",
                "There is no difference; the terms are exact synonyms"
            ),
            correctIndex = 0,
            explanation = "CSFs define the vital strategic areas where success is essential (e.g., high customer retention), while KPIs supply the specific numerical targets and metrics."
        ),
        McqQuestion(
            id = "om_u13_2",
            question = "How does budgetary control contribute to profit maximization?",
            options = listOf(
                "By prohibiting any variance investigation",
                "By controlling costs, measuring department/cost-center performance, and guiding corrective action",
                "By isolating departments so costs and revenues are never coordinated",
                "By replacing all strategic plans with static historical spend"
            ),
            correctIndex = 1,
            explanation = "Budgetary control supports cost reduction, variance analysis, cross-departmental coordination, performance measurement, and incentive alignment."
        ),
        McqQuestion(
            id = "om_u14_1",
            question = "What are the four perspectives in Kaplan & Norton's Balanced Scorecard?",
            options = listOf(
                "Profit, Liquidity, Solvency, Gearing",
                "Financial, Customer, Internal Business Processes, Learning & Growth",
                "Operating Core, Middle Line, Technostructure, Support Staff",
                "Product, Price, Place, Promotion"
            ),
            correctIndex = 1,
            explanation = "The Balanced Scorecard balances Financial metrics with Customer, Internal Business Process, and Learning & Growth perspectives."
        ),
        McqQuestion(
            id = "om_u14_2",
            question = "Which is a recognized disadvantage of the Balanced Scorecard noted in ICMA Past Papers?",
            options = listOf(
                "It ignores non-financial customer loyalty entirely",
                "Information overload from too many measures, difficulty processing qualitative data, and potential conflict between metrics",
                "It requires zero senior management involvement",
                "It can only measure manufacturing labor hours"
            ),
            correctIndex = 1,
            explanation = "BSC pitfalls include qualitative data recording difficulty, information overload, metric conflicts (e.g., cutting R&D for short-term profit), misalignment, and setup cost."
        ),
        McqQuestion(
            id = "om_u14_3",
            question = "Fitzgerald & Moon's Building Block Model for service businesses is structured around which three pillars?",
            options = listOf(
                "Dimensions, Standards, and Rewards",
                "Inputs, Outputs, and Outcomes",
                "Vision, Objectives, and Tactics",
                "Sort, Shine, and Sustain"
            ),
            correctIndex = 0,
            explanation = "The Building Block Model integrates 6 Performance Dimensions (2 results + 4 determinants), 3 Standards (ownership, achievability, equity), and 3 Rewards (clarity, motivation, controllability)."
        ),
        McqQuestion(
            id = "om_u14_4",
            question = "In Lynch & Cross's Performance Pyramid, what sits at the very apex connecting corporate strategy to customer and financial goals?",
            options = listOf("Departmental Waste & Cycle Time", "Corporate Vision", "Operating Core", "Individual Bonus Targets"),
            correctIndex = 1,
            explanation = "Lynch & Cross's Performance Pyramid cascades Corporate Vision down through Business Units, Core Business Processes, and Departments/Work Centers."
        )
    )

    val banks: List<McqBank> = listOf(
        McqBank(
            id = "om_office_short_test",
            title = "Short Test during Office (10 Qs • 12:45 PM)",
            subtitle = "Lunch-break rapid drill across Balanced Scorecard, Mintzberg, Quality Circles, HRM & MRP",
            questions = unit13To14Questions.take(3) +
                unit1To4Questions.take(3) +
                unit5To7Questions.take(2) +
                unit8To9Questions.take(2)
        ),
        McqBank(
            id = "om_test_of_the_day",
            title = "Test of the Day — High-Yield Merit (20 Qs)",
            subtitle = "Daily 8:15 PM timed 60% MCQ section benchmarked to ICMA / ACCA / CIMA",
            questions = unit1To4Questions.take(4) +
                unit5To7Questions.take(4) +
                unit8To9Questions.take(4) +
                unit10To12Questions.take(4) +
                unit13To14Questions.take(4)
        ),
        McqBank(
            id = "om_mock_exam",
            title = "Full-Course Merit Mock Exam (All Units)",
            subtitle = "Comprehensive timed simulation across all 14 OM Units before 5 Dec 2026",
            questions = unit1To4Questions +
                unit5To7Questions +
                unit8To9Questions +
                unit10To12Questions +
                unit13To14Questions
        ),
        McqBank(
            id = "om_bank_1_4",
            title = "Units 1–4: Enterprise, Mintzberg, Production & TPM",
            subtitle = "${unit1To4Questions.size} MCQs • PESTEL, Mintzberg, Job/Batch/3D Printing, 5S & OEE",
            questions = unit1To4Questions
        ),
        McqBank(
            id = "om_bank_5_7",
            title = "Units 5–7: MRP, OPT, Quality, Six Sigma & Lean",
            subtitle = "${unit5To7Questions.size} MCQs • MRP/OPT-DBR, Capacity, Juran COQ, Six Sigma, Quality Circles, TQMEX",
            questions = unit5To7Questions
        ),
        McqBank(
            id = "om_bank_8_9",
            title = "Units 8–9: HRM, Motivation, Pfeffer & Kolb",
            subtitle = "${unit8To9Questions.size} MCQs • Herzberg, Handy Shamrock, 70-20-10, Interview Bias, Pfeffer & Kolb",
            questions = unit8To9Questions
        ),
        McqBank(
            id = "om_bank_10_12",
            title = "Units 10–12: Marketing Concept, 7Ps, BCG & Ansoff",
            subtitle = "${unit10To12Questions.size} MCQs • Marketing vs Sales Orientation, 7Ps, Ansoff, BCG & Buyer Process",
            questions = unit10To12Questions
        ),
        McqBank(
            id = "om_bank_13_14",
            title = "Units 13–14: EPM, BSC, Building Block & Pyramid",
            subtitle = "${unit13To14Questions.size} MCQs • CSFs/KPIs, Budgets, Balanced Scorecard, Fitzgerald & Moon, Lynch & Cross",
            questions = unit13To14Questions
        )
    )
}

object PreloadedFlashcards {
    const val STARTER_DECK_TITLE = "ICMA M3 OM Starter Deck"

    val cards: List<Flashcard> = listOf(
        Flashcard(
            id = "starter_fc_1",
            materialId = "om_study_text_2026",
            deckTitle = STARTER_DECK_TITLE,
            front = "What are Mintzberg's 5 Organizational Parts?",
            back = "1. Strategic Apex (top executives)\n2. Middle Line (managerial hierarchy)\n3. Operating Core (frontline producers/service staff)\n4. Technostructure (analysts standardizing work/outputs)\n5. Support Staff (indirect services like legal, HR, catering).",
            hint = "Apex, Middle, Core, plus two outside advisory/support wings."
        ),
        Flashcard(
            id = "starter_fc_2",
            materialId = "om_study_text_2026",
            deckTitle = STARTER_DECK_TITLE,
            front = "How is Overall Equipment Effectiveness (OEE) calculated in TPM?",
            back = "OEE = Availability × Performance × Quality\nIt captures losses from equipment breakdowns/setups, slow cycle speeds, and defective output.",
            hint = "Multiply three operational ratios."
        ),
        Flashcard(
            id = "starter_fc_3",
            materialId = "om_study_text_2026",
            deckTitle = STARTER_DECK_TITLE,
            front = "Explain OPT's Drum-Buffer-Rope scheduling mechanism.",
            back = "• Drum: The bottleneck constraint that sets the factory production pace.\n• Buffer: Time/inventory buffer placed right before the bottleneck to keep it fed.\n• Rope: Communication schedule linking material release to the bottleneck's consumption rate.",
            hint = "Theory of Constraints scheduling anchored around the bottleneck."
        ),
        Flashcard(
            id = "starter_fc_4",
            materialId = "om_study_text_2026",
            deckTitle = STARTER_DECK_TITLE,
            front = "What are Juran's 4 Cost of Quality (COQ) categories?",
            back = "1. Prevention Costs (training, quality planning)\n2. Appraisal Costs (inspection, testing, audits)\n3. Internal Failure Costs (scrap, rework before delivery)\n4. External Failure Costs (warranty claims, recalls after delivery).",
            hint = "Two conformance costs + two failure costs."
        ),
        Flashcard(
            id = "starter_fc_5",
            materialId = "om_study_text_2026",
            deckTitle = STARTER_DECK_TITLE,
            front = "What are the 3 leaves of Charles Handy's Shamrock Organization?",
            back = "1. Professional Core (essential qualified full-time managers & specialists)\n2. Contractual Fringe (non-core work outsourced to specialist firms)\n3. Flexible Labor Force (part-time, temporary & seasonal workers).",
            hint = "Core + Outsourced + Flexible."
        ),
        Flashcard(
            id = "starter_fc_6",
            materialId = "om_past_papers_ml1",
            deckTitle = STARTER_DECK_TITLE,
            front = "List 4 common judgment errors in selection interviews (OM Past Paper Q.8).",
            back = "1. Halo Effect (one standout trait colors total judgment)\n2. Contagious Bias (giving leading cues from earlier candidates)\n3. Stereotyping (judging by accent, dress, or voice)\n4. Logical Error / misjudging abstract traits like honesty.",
            hint = "Halo, Contagious, Stereotyping, Logical."
        ),
        Flashcard(
            id = "starter_fc_7",
            materialId = "om_past_papers_ml1",
            deckTitle = STARTER_DECK_TITLE,
            front = "What are the 4 perspectives in Kaplan & Norton's Balanced Scorecard?",
            back = "1. Financial (shareholder value & profitability)\n2. Customer (satisfaction, retention, market share)\n3. Internal Business Processes (cycle time, quality, innovation)\n4. Learning & Growth (skills, IT systems, culture).",
            hint = "Balances financial lagging results with 3 leading perspectives."
        ),
        Flashcard(
            id = "starter_fc_8",
            materialId = "om_study_text_2026",
            deckTitle = STARTER_DECK_TITLE,
            front = "What are the 3 pillars of Fitzgerald & Moon's Building Block Model?",
            back = "1. Dimensions (2 results: financial & competitiveness; 4 determinants: quality, flexibility, resource utilization, innovation)\n2. Standards (ownership, achievability, equity)\n3. Rewards (clarity, motivation, controllability).",
            hint = "Designed specifically for service-sector performance."
        )
    )
}

data class TypingQuestion(
    val id: String,
    val topicTag: String,
    val marks: Int,
    val sourceBenchmark: String,
    val question: String,
    val keywords: List<String>,
    val modelAnswer: String
)

object OmTypingQuestions {
    val items: List<TypingQuestion> = listOf(
        TypingQuestion(
            id = "typ_q4_bsc",
            topicTag = "Balanced Scorecard",
            marks = 5,
            sourceBenchmark = "ICMA ML-1 Past Paper Q.4",
            question = "Explain five key disadvantages or pitfalls of implementing a Balanced Scorecard (BSC).",
            keywords = listOf("qualitative", "overload", "conflict", "misaligned", "senior management"),
            modelAnswer = "1) Difficulty measuring qualitative non-financial data.\n2) Information overload from too many KPIs.\n3) Conflict between metrics (e.g., cutting R&D boosts short-term profit).\n4) Measures misaligned with corporate strategy.\n5) Failure without senior management commitment & high setup cost."
        ),
        TypingQuestion(
            id = "typ_q6_mintzberg",
            topicTag = "Mintzberg 5 Parts",
            marks = 5,
            sourceBenchmark = "ICMA ML-1 Past Paper Q.6",
            question = "Describe the five basic parts of Mintzberg's Effective Organization with corporate examples.",
            keywords = listOf("operating core", "strategic apex", "middle line", "technostructure", "support staff"),
            modelAnswer = "1) Operating Core: Frontline workers producing goods/services.\n2) Strategic Apex: Top executives setting strategy.\n3) Middle Line: Managerial hierarchy linking apex to frontline.\n4) Technostructure: Analysts standardizing processes/outputs (e.g., management accountants, HR planners).\n5) Support Staff: Indirect services (legal, PR, catering)."
        ),
        TypingQuestion(
            id = "typ_q9_qc",
            topicTag = "Quality Circles & TPM",
            marks = 5,
            sourceBenchmark = "ICMA ML-1 Past Paper Q.9",
            question = "Define a Quality Circle and state four advantages to a manufacturing or service organization.",
            keywords = listOf("voluntary", "shop-floor", "productivity", "continuous improvement", "unity"),
            modelAnswer = "A small voluntary group of employees with mixed skills meeting regularly to solve workplace quality problems. Advantages: (1) Higher product quality & customer satisfaction; (2) Increased productivity & lower waste; (3) Operational staff know shop-floor defects best; (4) Fosters continuous improvement culture & unity."
        ),
        TypingQuestion(
            id = "typ_q10_mrp",
            topicTag = "MRP & OPT",
            marks = 5,
            sourceBenchmark = "ICMA ML-1 Past Paper Q.10",
            question = "Explain five operational and financial benefits of Material Requirement Planning (MRP).",
            keywords = listOf("customer orders", "stock holding", "bottlenecks", "suppliers", "demand"),
            modelAnswer = "1) Improved ability to meet customer delivery deadlines.\n2) Reduced inventory holding & working capital costs.\n3) Fast computerized schedule updates when demand shifts.\n4) Early warning of purchasing or production bottlenecks.\n5) Closer, reliable relationships with suppliers."
        ),
        TypingQuestion(
            id = "typ_q8_interviews",
            topicTag = "HRM & Motivation",
            marks = 5,
            sourceBenchmark = "ICMA ML-1 Past Paper Q.8",
            question = "Discuss the general limitations and interviewer judgment errors in selection interviews.",
            keywords = listOf("unreliable", "predictive validity", "halo effect", "contagious bias", "stereotyping"),
            modelAnswer = "Interviews suffer from low reliability (assessors disagree) and weak predictive validity of actual job performance. Key judgment errors: (1) Halo effect; (2) Contagious bias (leading cues); (3) Stereotyping based on dress/voice/accent; (4) Misjudging abstract traits like honesty; (5) Logical error."
        ),
        TypingQuestion(
            id = "typ_q1_mktg",
            topicTag = "Marketing Concept & STP",
            marks = 5,
            sourceBenchmark = "ICMA ML-1 Past Paper Q.1 & Q.5",
            question = "Define the Marketing Concept and contrast it with three drawbacks of Sales Orientation.",
            keywords = listOf("target market", "satisfaction", "competitors", "short-term", "turnover"),
            modelAnswer = "Marketing Concept: Determining needs, wants, and values of a target market and delivering desired satisfaction more effectively/efficiently than rivals. Sales Orientation drawbacks: (1) Customer dissatisfaction from pushy tactics; (2) Short-term focus over loyalty; (3) Lack of innovation & high sales staff turnover."
        )
    )
}



