package com.example.data

import kotlin.math.min
import kotlin.math.roundToInt

enum class OmPortionCode(
    val partLabel: String,
    val title: String,
    val units: String,
    val weightPercent: Int,
    val isHighYield: Boolean,
    val new2025Badge: String,
    val linkedBankId: String,
    val keywords: List<String>,
    val routineSlotIds: List<String>
) {
    PART_A(
        partLabel = "Part A",
        title = "Operations Management",
        units = "Units 1–7",
        weightPercent = 35,
        isHighYield = true,
        new2025Badge = "ESG • AI Quality • 3D / OPT",
        linkedBankId = "om_bank_1_4",
        keywords = listOf(
            "operations", "pestel", "esg", "mintzberg", "production", "3d",
            "tpm", "oee", "5s", "mrp", "opt", "erp", "quality", "juran",
            "six sigma", "kaizen", "lean", "units 1", "units 5", "part a"
        ),
        routineSlotIds = listOf("slot_0800_morning", "slot_1245_lunch_short", "slot_1530_break2", "slot_2015_daily_test")
    ),
    PART_B(
        partLabel = "Part B",
        title = "Human Resource Management",
        units = "Units 8–9",
        weightPercent = 20,
        isHighYield = false,
        new2025Badge = "Algorithmic Bias • Hybrid • EX",
        linkedBankId = "om_bank_8_9",
        keywords = listOf(
            "hrm", "human resource", "human capital", "motivation", "herzberg",
            "maslow", "shamrock", "handy", "pfeffer", "kolb", "interview",
            "halo", "bias", "70-20-10", "units 8", "part b"
        ),
        routineSlotIds = listOf("slot_1730_tired_cards", "slot_1900_fresh_deep", "slot_2015_daily_test")
    ),
    PART_C(
        partLabel = "Part C",
        title = "Marketing Management",
        units = "Units 10–12",
        weightPercent = 25,
        isHighYield = true,
        new2025Badge = "Metaverse • Web3 / DAO • CSR",
        linkedBankId = "om_bank_10_12",
        keywords = listOf(
            "marketing", "sales orientation", "stp", "7ps", "4ps", "bcg",
            "ansoff", "consumer", "brand", "metaverse", "web3", "csr",
            "units 10", "part c"
        ),
        routineSlotIds = listOf("slot_0800_morning", "slot_1730_tired_cards", "slot_2015_daily_test")
    ),
    PART_D(
        partLabel = "Part D",
        title = "Performance Management",
        units = "Units 13–14",
        weightPercent = 20,
        isHighYield = false,
        new2025Badge = "EPM • Budgets • Balanced Scorecard",
        linkedBankId = "om_bank_13_14",
        keywords = listOf(
            "performance", "epm", "budget", "csf", "kpi", "balanced scorecard",
            "bsc", "fitzgerald", "building block", "pyramid", "lynch",
            "variance", "units 13", "part d"
        ),
        routineSlotIds = listOf("slot_1245_lunch_short", "slot_1900_fresh_deep", "slot_2015_daily_test")
    )
}

data class PortionStat(
    val code: OmPortionCode,
    val conceptsPct: Int,
    val planPct: Int,
    val testMarksPct: Int?,
    val testAttemptsCount: Int,
    val overallPrepPct: Int,
    val projectedMarks: Float
)

data class ExecutiveDashboardAnalytics(
    val portions: List<PortionStat>,
    val projectedTotalScore: Float,
    val highYieldMarks: Float,
    val highYieldPct: Int,
    val overallConceptsPct: Int,
    val overallTestAvgPct: Int?
)

object PortionAnalyticsCalculator {

    fun compute(
        materials: List<StudyMaterial>,
        progressMap: Map<String, MaterialProgress>,
        plans: List<StudyPlanItem>,
        attempts: List<QuizAttempt>,
        flashcards: List<Flashcard>,
        completedRoutineIds: Set<String>
    ): ExecutiveDashboardAnalytics {
        val coreBookDone = progressMap["om_study_text_2026"]?.completed == true
        val pastPapersDone = progressMap["om_past_papers_ml1"]?.completed == true
        val generalReadingRatio = when {
            coreBookDone && pastPapersDone -> 0.40f
            coreBookDone -> 0.25f
            pastPapersDone -> 0.15f
            else -> 0f
        }

        val portionStats = OmPortionCode.entries.map { portion ->
            // 1. Concepts Mastery from flashcards + materials mapped to this portion
            val portionCards = flashcards.filter { card ->
                val text = "${card.deckTitle} ${card.front} ${card.back}".lowercase()
                portion.keywords.any { kw -> text.contains(kw) }
            }
            val cardScore = portionCards.sumOf { card ->
                when {
                    card.mastered -> 1.0
                    card.reviewCount > 0L -> 0.55
                    else -> 0.0
                }
            }
            val cardRatio = if (portionCards.isNotEmpty()) {
                (cardScore / portionCards.size).toFloat()
            } else 0f

            val matchedMaterials = materials.filter { mat ->
                val hay = "${mat.title} ${mat.category} ${mat.description}".lowercase()
                portion.keywords.any { kw -> hay.contains(kw) }
            }
            val completedPortionMaterials = matchedMaterials.count { progressMap[it.id]?.completed == true }
            val materialBonus = if (matchedMaterials.isNotEmpty()) {
                (completedPortionMaterials.toFloat() / matchedMaterials.size) * 0.25f
            } else 0f

            val conceptsPct = min(100, ((cardRatio * 0.60f + generalReadingRatio + materialBonus) * 100f).roundToInt())

            // 2. Plan Progress from studyPlans + daily routine slots
            val portionPlans = plans.filter { plan ->
                val hay = "${plan.title} ${plan.topic}".lowercase()
                portion.keywords.any { kw -> hay.contains(kw) }
            }
            val planTaskRatio = if (portionPlans.isNotEmpty()) {
                portionPlans.count { it.completed }.toFloat() / portionPlans.size
            } else 0f
            val doneSlotsCount = portion.routineSlotIds.count { it in completedRoutineIds }
            val slotRatio = if (portion.routineSlotIds.isNotEmpty()) {
                doneSlotsCount.toFloat() / portion.routineSlotIds.size
            } else 0f
            val planPct = min(100, ((planTaskRatio * 0.60f + slotRatio * 0.40f) * 100f).roundToInt())

            // 3. Test Marks from QuizAttempt history
            val matchingAttempts = attempts.filter { attempt ->
                val lower = attempt.topic.lowercase()
                val matchesPortion = portion.keywords.any { kw -> lower.contains(kw) }
                val isComprehensive = lower.contains("short test") ||
                    lower.contains("test of the day") ||
                    lower.contains("full-course") ||
                    lower.contains("mock")
                matchesPortion || isComprehensive
            }
            val totalCorrect = matchingAttempts.sumOf { it.correctCount }
            val totalQs = matchingAttempts.sumOf { it.totalQuestions }
            val testMarksPct = if (totalQs > 0L) {
                min(100, ((totalCorrect * 100L) / totalQs).toInt())
            } else null

            // 4. Combined Preparation & Projected Contribution
            val overallPrepPct = if (testMarksPct != null) {
                (testMarksPct * 0.45f + conceptsPct * 0.35f + planPct * 0.20f).roundToInt().coerceIn(0, 100)
            } else {
                (conceptsPct * 0.60f + planPct * 0.40f).roundToInt().coerceIn(0, 100)
            }
            val projectedMarks = ((overallPrepPct / 100f) * portion.weightPercent)

            PortionStat(
                code = portion,
                conceptsPct = conceptsPct,
                planPct = planPct,
                testMarksPct = testMarksPct,
                testAttemptsCount = matchingAttempts.size,
                overallPrepPct = overallPrepPct,
                projectedMarks = (projectedMarks * 10f).roundToInt() / 10f
            )
        }

        val totalProjected = (portionStats.sumOf { it.projectedMarks.toDouble() }.toFloat() * 10f).roundToInt() / 10f
        val partA = portionStats.firstOrNull { it.code == OmPortionCode.PART_A }?.projectedMarks ?: 0f
        val partC = portionStats.firstOrNull { it.code == OmPortionCode.PART_C }?.projectedMarks ?: 0f
        val highYieldMarks = ((partA + partC) * 10f).roundToInt() / 10f
        val highYieldPct = ((highYieldMarks / 60f) * 100f).roundToInt().coerceIn(0, 100)
        val overallConcepts = if (portionStats.isNotEmpty()) {
            portionStats.map { it.conceptsPct }.average().roundToInt()
        } else 0
        val testedStats = portionStats.mapNotNull { it.testMarksPct }
        val overallTestAvg = if (testedStats.isNotEmpty()) testedStats.average().roundToInt() else null

        return ExecutiveDashboardAnalytics(
            portions = portionStats,
            projectedTotalScore = totalProjected,
            highYieldMarks = highYieldMarks,
            highYieldPct = highYieldPct,
            overallConceptsPct = overallConcepts,
            overallTestAvgPct = overallTestAvg
        )
    }
}
