package com.example.data.syllabus

import android.content.Context
import com.example.data.McqQuestion
import com.example.data.OmMcqBanks
import com.example.data.OmTypingQuestions
import com.example.data.PreloadedFlashcards
import com.example.data.TypingQuestion
import com.example.data.local.FlashcardEntity
import org.json.JSONObject

data class SyllabusPartInfo(
    val code: String,
    val title: String,
    val weightPercent: Int,
    val units: String
)

data class SyllabusTopic(
    val id: String,
    val partCode: String,
    val partTitle: String,
    val partWeight: Int,
    val unit: Int,
    val name: String,
    val videoFilename: String,
    val videoBaseUrl: String,
    val pdfPage: Int,
    val importance: String,
    val difficulty: String,
    val formulae: String,
    val description: String,
    val keyPoints: List<String>,
    val romanUrduSummary: String,
    val pakistaniExample: String
) {
    val videoUrl: String
        get() = if (videoFilename.isNotBlank()) "$videoBaseUrl$videoFilename" else ""
}

data class TopicMappedMcq(
    val topicId: String,
    val partCode: String,
    val unit: Int,
    val difficulty: String,
    val sourceLabel: String,
    val mcq: McqQuestion
)

data class PastPaperEntry(
    val id: String,
    val year: String,
    val partCode: String,
    val topicId: String,
    val marks: Int,
    val isOfficialPastPaper: Boolean,
    val sourceTag: String,
    val typingQuestion: TypingQuestion
)

data class SyllabusDatabaseBundle(
    val videoBaseUrl: String,
    val parts: List<SyllabusPartInfo>,
    val topics: List<SyllabusTopic>,
    val mcqs: List<TopicMappedMcq>,
    val pastPapers: List<PastPaperEntry>,
    val starterFlashcards: List<FlashcardEntity>
)

object OmSyllabusCatalog {

    const val DEFAULT_VIDEO_BASE_URL =
        "https://raw.githubusercontent.com/saifullahqureshi6100/ICMAP-Portal/main/public/videos/"

    fun loadBundle(context: Context): SyllabusDatabaseBundle {
        val rawJson = try {
            context.assets.open("om_syllabus_config.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
        return parseFromJson(rawJson)
    }

    fun parseFromJson(rawJson: String): SyllabusDatabaseBundle {
        val partsList = mutableListOf<SyllabusPartInfo>()
        val topicsList = mutableListOf<SyllabusTopic>()
        var baseUrl = DEFAULT_VIDEO_BASE_URL

        if (rawJson.isNotBlank()) {
            try {
                val root = JSONObject(rawJson)
                baseUrl = root.optString("videoBaseUrl", DEFAULT_VIDEO_BASE_URL)
                val partsArr = root.optJSONArray("parts")
                if (partsArr != null) {
                    for (pIdx in 0 until partsArr.length()) {
                        val pObj = partsArr.getJSONObject(pIdx)
                        val pCode = pObj.getString("code")
                        val pTitle = pObj.getString("title")
                        val pWeight = pObj.optInt("weightPercent", 25)
                        val pUnits = pObj.optString("units", "")
                        partsList.add(SyllabusPartInfo(pCode, pTitle, pWeight, pUnits))

                        val tArr = pObj.optJSONArray("topics") ?: continue
                        for (tIdx in 0 until tArr.length()) {
                            val tObj = tArr.getJSONObject(tIdx)
                            val kpArr = tObj.optJSONArray("keyPoints")
                            val keyPoints = buildList {
                                if (kpArr != null) {
                                    for (k in 0 until kpArr.length()) add(kpArr.getString(k))
                                }
                            }
                            topicsList.add(
                                SyllabusTopic(
                                    id = tObj.getString("id"),
                                    partCode = pCode,
                                    partTitle = pTitle,
                                    partWeight = pWeight,
                                    unit = tObj.optInt("unit", 1),
                                    name = tObj.getString("name"),
                                    videoFilename = tObj.optString("videoFilename", ""),
                                    videoBaseUrl = baseUrl,
                                    pdfPage = tObj.optInt("pdfPage", 1),
                                    importance = tObj.optString("importance", "HIGH"),
                                    difficulty = tObj.optString("difficulty", "MEDIUM"),
                                    formulae = tObj.optString("formulae", ""),
                                    description = tObj.optString("description", ""),
                                    keyPoints = keyPoints,
                                    romanUrduSummary = tObj.optString("romanUrduSummary", ""),
                                    pakistaniExample = tObj.optString("pakistaniExample", "")
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall back to empty list and build defaults below
            }
        }

        if (partsList.isEmpty()) {
            partsList.addAll(
                listOf(
                    SyllabusPartInfo("PART_A", "Operations Management", 35, "Units 1–7"),
                    SyllabusPartInfo("PART_B", "Human Resource Management", 20, "Units 8–9"),
                    SyllabusPartInfo("PART_C", "Marketing Management", 25, "Units 10–12"),
                    SyllabusPartInfo("PART_D", "Performance Management", 20, "Units 13–14")
                )
            )
        }

        val mappedMcqs = buildMappedMcqs(topicsList)
        val pastPapers = buildPastPapers()
        val flashcards = buildStarterFlashcards(topicsList)

        return SyllabusDatabaseBundle(
            videoBaseUrl = baseUrl,
            parts = partsList,
            topics = topicsList,
            mcqs = mappedMcqs,
            pastPapers = pastPapers,
            starterFlashcards = flashcards
        )
    }

    private fun buildMappedMcqs(topics: List<SyllabusTopic>): List<TopicMappedMcq> {
        val bankQuestions = OmMcqBanks.banks
            .filter { it.id.startsWith("om_bank_") }
            .flatMap { bank -> bank.questions }

        val existing = bankQuestions.distinctBy { it.id }.map { q ->
            val (topicId, partCode, unit) = inferTopicForMcq(q.id, q.question, topics)
            TopicMappedMcq(
                topicId = topicId,
                partCode = partCode,
                unit = unit,
                difficulty = "MEDIUM",
                sourceLabel = "ICMA M3 Syllabus Bank",
                mcq = q
            )
        }

        val extraHighYield = listOf(
            TopicMappedMcq(
                topicId = "om_u05_jit",
                partCode = "PART_A",
                unit = 5,
                difficulty = "MEDIUM",
                sourceLabel = "Jarvis Practice Question",
                mcq = McqQuestion(
                    id = "om_u5_jit_1",
                    question = "Which operating condition is MOST critical for a Just-In-Time (JIT) production environment to function without line stoppages?",
                    options = listOf(
                        "Maintaining six months of safety stock in central warehouses",
                        "Zero-defect quality, short setup times, and highly reliable supplier deliveries",
                        "Ignoring preventive machine maintenance until breakdowns occur",
                        "Producing huge batch sizes regardless of customer demand"
                    ),
                    correctIndex = 1,
                    explanation = "Because JIT eliminates buffer inventories, supplier reliability, zero-defect quality, and quick changeovers are essential."
                )
            ),
            TopicMappedMcq(
                topicId = "om_u05_scm",
                partCode = "PART_A",
                unit = 5,
                difficulty = "HARD",
                sourceLabel = "Jarvis Practice Question",
                mcq = McqQuestion(
                    id = "om_u5_scm_1",
                    question = "What causes the 'Bullwhip Effect' across an upstream supply chain?",
                    options = listOf(
                        "Sharing live retail point-of-sale data across all tiers",
                        "Small fluctuations in retail consumer demand getting amplified into larger order swings at upstream factories",
                        "Using Vendor-Managed Inventory (VMI)",
                        "Adopting 5S Sort and Shine in a warehouse"
                    ),
                    correctIndex = 1,
                    explanation = "Distorted demand signals and batch ordering at each supply tier amplify minor retail demand shifts into large upstream swings."
                )
            ),
            TopicMappedMcq(
                topicId = "om_u10_stp",
                partCode = "PART_C",
                unit = 10,
                difficulty = "EASY",
                sourceLabel = "Jarvis Practice Question",
                mcq = McqQuestion(
                    id = "om_u10_stp_1",
                    question = "In STP marketing, deciding which specific customer segments an enterprise will actually focus its resources to serve is called:",
                    options = listOf("Positioning", "Market Targeting", "Behavioral Profiling", "Mass Production"),
                    correctIndex = 1,
                    explanation = "Segmentation divides the market, Targeting selects the most attractive segment(s), and Positioning crafts the brand image in buyers' minds."
                )
            ),
            TopicMappedMcq(
                topicId = "om_u10_metaverse",
                partCode = "PART_C",
                unit = 10,
                difficulty = "MEDIUM",
                sourceLabel = "2025 Syllabus Update Question",
                mcq = McqQuestion(
                    id = "om_u10_2025_1",
                    question = "Why do modern digital brands frequently partner with 'micro-influencers' (10k–50k followers) instead of only mega-celebrities?",
                    options = listOf(
                        "Micro-influencers always charge higher retainers",
                        "Micro-influencers generate stronger niche community trust and higher authentic engagement rates",
                        "Micro-influencers replace the need for product quality",
                        "They operate exclusively in traditional print newspapers"
                    ),
                    correctIndex = 1,
                    explanation = "ICMA Study Scheme 2025 highlights micro-influencers for higher engagement, niche targeting, and stronger peer credibility."
                )
            )
        )

        return existing + extraHighYield
    }

    private fun inferTopicForMcq(
        qId: String,
        stem: String,
        topics: List<SyllabusTopic>
    ): Triple<String, String, Int> {
        val l = stem.lowercase()
        return when {
            qId.startsWith("om_u1_") || l.contains("pestel") -> Triple("om_u01_pestel", "PART_A", 1)
            qId.startsWith("om_u2_") || l.contains("mintzberg") -> Triple("om_u02_mintzberg", "PART_A", 2)
            qId.startsWith("om_u3_") || l.contains("yacht") || l.contains("additive") -> Triple("om_u03_production", "PART_A", 3)
            qId.startsWith("om_u4_") && l.contains("5s") -> Triple("om_u04_5s", "PART_A", 4)
            qId.startsWith("om_u4_") || l.contains("oee") -> Triple("om_u04_oee", "PART_A", 4)
            qId.startsWith("om_u5_") && l.contains("opt") -> Triple("om_u05_opt", "PART_A", 5)
            qId.startsWith("om_u5_") || l.contains("mrp") -> Triple("om_u05_mrp_erp", "PART_A", 5)
            qId.startsWith("om_u6_") || l.contains("capacity") -> Triple("om_u06_capacity", "PART_A", 6)
            l.contains("quality circle") || l.contains("tqmex") -> Triple("om_u07_kaizen_qc", "PART_A", 7)
            qId.startsWith("om_u7_") -> Triple("om_u07_coq", "PART_A", 7)
            l.contains("herzberg") -> Triple("om_u08_herzberg", "PART_B", 8)
            l.contains("shamrock") -> Triple("om_u08_psych_contract", "PART_B", 8)
            l.contains("70-20-10") -> Triple("om_u08_mcgregor", "PART_B", 8)
            l.contains("halo") || l.contains("interviewer") -> Triple("om_u08_interview_bias", "PART_B", 8)
            qId.startsWith("om_u9_") -> Triple("om_u09_kolb_pfeffer", "PART_B", 9)
            qId.startsWith("om_u10_") -> Triple("om_u10_orientations", "PART_C", 10)
            l.contains("bcg") || l.contains("7ps") -> Triple("om_u11_bcg", "PART_C", 11)
            qId.startsWith("om_u11_") || qId.startsWith("om_u12_") -> Triple("om_u11_ansoff", "PART_C", 11)
            qId.startsWith("om_u13_") -> Triple("om_u13_epm_budget", "PART_D", 13)
            qId.startsWith("om_u14_") -> Triple("om_u14_bsc", "PART_D", 14)
            else -> topics.firstOrNull()?.let { Triple(it.id, it.partCode, it.unit) } ?: Triple("om_u01_enterprise", "PART_A", 1)
        }
    }

    private fun buildPastPapers(): List<PastPaperEntry> = listOf(
        PastPaperEntry("pp_q1", "ICMA Past Paper", "PART_C", "om_u10_orientations", 5, true, "Official Past Paper Q.1 & Q.5", OmTypingQuestions.items[5]),
        PastPaperEntry(
            id = "pp_q2",
            year = "ICMA Past Paper",
            partCode = "PART_B",
            topicId = "om_u08_hrm_taylor",
            marks = 5,
            isOfficialPastPaper = true,
            sourceTag = "Official Past Paper Q.2",
            typingQuestion = TypingQuestion(
                id = "typ_q2_hrm",
                topicTag = "HRM Strategic Importance",
                marks = 5,
                sourceBenchmark = "ICMA ML-1 Past Paper Q.2",
                question = "State three reasons why the HRM process is important and four external factors affecting HRM.",
                keywords = listOf("competitive advantage", "strategies", "economy", "unions", "legal", "demographic"),
                modelAnswer = "3 Reasons: (1) Competitive advantage; (2) Vital part of corporate strategies; (3) Impacts organizational performance.\n4 External Factors: Economy, Labor Unions, Legal environment, and Demographic trends."
            )
        ),
        PastPaperEntry(
            id = "pp_q3",
            year = "ICMA Past Paper",
            partCode = "PART_B",
            topicId = "om_u08_hrm_taylor",
            marks = 5,
            isOfficialPastPaper = true,
            sourceTag = "Official Past Paper Q.3",
            typingQuestion = TypingQuestion(
                id = "typ_q3_agency",
                topicTag = "Recruitment Outsourcing",
                marks = 5,
                sourceBenchmark = "ICMA ML-1 Past Paper Q.3",
                question = "Give five reasons why an organization outsources recruitment to private employment agencies instead of recruiting in-house.",
                keywords = listOf("trained staff", "competitors", "cost-effective", "turnover", "databanks", "confidentiality"),
                modelAnswer = "1) HR lacks trained staff for screening/ads. 2) Avoids alerting rivals to expansion. 3) Cost-effective filtering of mass applicants. 4) Prevents ads from signaling high turnover. 5) Ready candidate databanks + confidentiality for executive headhunting."
            )
        ),
        PastPaperEntry("pp_q4", "ICMA Past Paper", "PART_D", "om_u14_bsc", 5, true, "Official Past Paper Q.4", OmTypingQuestions.items[0]),
        PastPaperEntry("pp_q6", "ICMA Past Paper", "PART_A", "om_u02_mintzberg", 5, true, "Official Past Paper Q.6", OmTypingQuestions.items[1]),
        PastPaperEntry(
            id = "pp_q7",
            year = "ICMA Past Paper",
            partCode = "PART_D",
            topicId = "om_u13_epm_budget",
            marks = 5,
            isOfficialPastPaper = true,
            sourceTag = "Official Past Paper Q.7",
            typingQuestion = TypingQuestion(
                id = "typ_q7_budget",
                topicTag = "Budgetary Control",
                marks = 5,
                sourceBenchmark = "ICMA ML-1 Past Paper Q.7",
                question = "Explain how budgetary controls assist management in maximizing profit.",
                keywords = listOf("costs", "efficiency", "cost-center", "variance", "coordination", "corrective"),
                modelAnswer = "Budgetary control curtails waste and boosts efficiency, measures department/cost-center performance, reveals cost-reduction areas, coordinates divisions, supports incentive schemes, and enables timely variance corrective action."
            )
        ),
        PastPaperEntry("pp_q8", "ICMA Past Paper", "PART_B", "om_u08_interview_bias", 5, true, "Official Past Paper Q.8", OmTypingQuestions.items[4]),
        PastPaperEntry("pp_q9", "ICMA Past Paper", "PART_A", "om_u07_kaizen_qc", 5, true, "Official Past Paper Q.9", OmTypingQuestions.items[2]),
        PastPaperEntry("pp_q10", "ICMA Past Paper", "PART_A", "om_u05_mrp_erp", 5, true, "Official Past Paper Q.10", OmTypingQuestions.items[3]),
        PastPaperEntry(
            id = "pp_jarvis_esg",
            year = "2025 Scheme Drill",
            partCode = "PART_A",
            topicId = "om_u01_pestel",
            marks = 5,
            isOfficialPastPaper = false,
            sourceTag = "Jarvis Practice Question",
            typingQuestion = TypingQuestion(
                id = "typ_jarvis_esg",
                topicTag = "ESG & Digital Transformation",
                marks = 5,
                sourceBenchmark = "Jarvis Practice Question (2025 Syllabus)",
                question = "Explain how Environmental, Social, and Governance (ESG) integration impacts operations decisions for a Pakistani export manufacturer.",
                keywords = listOf("carbon", "energy", "safety", "governance", "compliance", "waste"),
                modelAnswer = "Environmental: Lower carbon emissions, water recycling, and renewable energy to pass EU/export audits.\nSocial: Safe working conditions, fair wages, and zero child/forced labor.\nGovernance: Ethical supplier procurement and transparent audit reporting."
            )
        ),
        PastPaperEntry(
            id = "pp_jarvis_building_block",
            year = "Model Framework Drill",
            partCode = "PART_D",
            topicId = "om_u14_bsc",
            marks = 5,
            isOfficialPastPaper = false,
            sourceTag = "Jarvis Practice Question",
            typingQuestion = TypingQuestion(
                id = "typ_jarvis_bb",
                topicTag = "Fitzgerald & Moon Building Block",
                marks = 5,
                sourceBenchmark = "Jarvis Practice Question",
                question = "Outline the three components of Fitzgerald & Moon's Building Block Model for service performance management.",
                keywords = listOf("dimensions", "standards", "rewards", "ownership", "controllability", "determinants"),
                modelAnswer = "1) Dimensions: 2 Results (Financial, Competitiveness) + 4 Determinants (Quality, Flexibility, Resource Utilization, Innovation).\n2) Standards: Ownership, Achievability, Equity.\n3) Rewards: Clarity, Motivation, Controllability."
            )
        )
    )

    private fun buildStarterFlashcards(topics: List<SyllabusTopic>): List<FlashcardEntity> {
        val baseCards = PreloadedFlashcards.cards.mapIndexed { idx, fc ->
            val mapped = when (idx) {
                0 -> "om_u02_mintzberg" to "PART_A"
                1 -> "om_u04_oee" to "PART_A"
                2 -> "om_u05_opt" to "PART_A"
                3 -> "om_u07_coq" to "PART_A"
                4 -> "om_u08_psych_contract" to "PART_B"
                5 -> "om_u08_interview_bias" to "PART_B"
                else -> "om_u14_bsc" to "PART_D"
            }
            FlashcardEntity(
                id = fc.id,
                topicId = mapped.first,
                partCode = mapped.second,
                deckTitle = fc.deckTitle,
                front = fc.front,
                back = fc.back,
                hint = fc.hint,
                difficulty = "HIGH",
                isCustom = false
            )
        }

        val topicCards = topics.mapIndexed { i, t ->
            FlashcardEntity(
                id = "syl_fc_${t.id}_$i",
                topicId = t.id,
                partCode = t.partCode,
                deckTitle = "Unit ${t.unit} — ${t.partTitle}",
                front = "${t.name}: Core Formula & Exam Essence?",
                back = "${t.formulae}\n\n${t.romanUrduSummary}",
                hint = t.keyPoints.firstOrNull().orEmpty(),
                difficulty = t.difficulty,
                isCustom = false
            )
        }
        return (baseCards + topicCards).distinctBy { it.id }
    }
}
