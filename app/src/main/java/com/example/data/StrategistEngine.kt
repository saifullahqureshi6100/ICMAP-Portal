package com.example.data

import java.util.Calendar
import kotlin.math.max

enum class EnergyMode(val label: String, val subtitle: String) {
    NORMAL("Normal Office Day", "180m Deep Routine"),
    BUSY_OFFICE("Meeting / Busy", "110m Compressed"),
    LOW_RECOVERY("Tired / Recovery", "90m Gentle Recall"),
    FINAL_SPRINT("Final Sprint (48h)", "Full-Course Mocks")
}

enum class RoutineActionType {
    READ_VIDEO,
    OFFICE_SHORT_TEST,
    FLASHCARDS,
    DAILY_TEST
}

data class DailyRoutineSlot(
    val id: String,
    val timeRange: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val actionType: RoutineActionType,
    val topicFocus: String
)

data class DailyBriefPlan(
    val daysToExam: Int,
    val energyMode: EnergyMode,
    val totalStudyMinutes: Int,
    val examPhaseTitle: String,
    val todayTopic: String,
    val todayVideoQuery: String,
    val upcomingMockAlert: String,
    val studyTechniqueTip: String,
    val urduSpokenBrief: String,
    val slots: List<DailyRoutineSlot>
)

private data class RoadmapDayConfig(
    val topic: String,
    val videoQuery: String,
    val technique: String
)

object StrategistEngine {

    private val emergencyNineDayRoadmap = listOf(
        RoadmapDayConfig(
            topic = "Day 1 Emergency Start: Balanced Scorecard, CSFs & Past Paper Q.4 + Q.7 (Units 13–14)",
            videoQuery = "Balanced Scorecard Kaplan Norton ICMA OM Unit 14",
            technique = "80/20 Pareto Emergency Rule: From ZERO prep, lock high-weightage Units 13–14 first! Anchor BSC 4 perspectives to 4 walls of your office & memorize Q.4 drawbacks."
        ),
        RoadmapDayConfig(
            topic = "Day 2: Mintzberg 5 Parts, PESTEL/ESG & Production Systems (Units 1–3 + Q.6)",
            videoQuery = "Mintzberg 5 Organizational Parts Job Batch Flow",
            technique = "60-Second Diagram Recall: Sketch Mintzberg's 5 parts during tea break — Strategic Apex, Middle Line, Operating Core, Technostructure, Support Staff."
        ),
        RoadmapDayConfig(
            topic = "Day 3: Plant Maintenance, 5S, TPM OEE, MRP & OPT Drum-Buffer-Rope (Units 4–5 + Q.10)",
            videoQuery = "TPM OEE MRP OPT Drum Buffer Rope",
            technique = "Formula Lock: OEE = Availability × Performance × Quality; 5S = Sort, Set, Shine, Standardize, Sustain; OPT = Drum-Buffer-Rope + 5 MRP Benefits (Q.10)."
        ),
        RoadmapDayConfig(
            topic = "Day 4: Quality Circles, Juran COQ, Six Sigma & Half-Book Mock Prep (Units 6–7 + Q.9)",
            videoQuery = "Quality Circles Cost of Quality Six Sigma DMAIC",
            technique = "Zero-to-Merit Check: Part A (Units 1–7) finishes tonight! Tomorrow is Half-Book Grand Test — flag every <70% MCQ."
        ),
        RoadmapDayConfig(
            topic = "Day 5: HRM Motivation, Handy Shamrock, Pfeffer 7 & Kolb (Units 8–9 + Q.2, Q.3, Q.8)",
            videoQuery = "Herzberg Handy Shamrock Pfeffer 7 Kolb Cycle",
            technique = "Past Paper Cluster Lock: Q.2 (HRM External Factors), Q.3 (Agency Outsourcing), Q.8 (Halo Effect, Contagious Bias, Stereotyping)."
        ),
        RoadmapDayConfig(
            topic = "Day 6: Marketing Concept vs Sales, 7Ps, BCG & Ansoff (Units 10–12 + Q.1, Q.5)",
            videoQuery = "Marketing Concept Sales Orientation 7Ps Ansoff BCG",
            technique = "Compare & Contrast Table: Never confuse Sales Orientation (short-term push) with Marketing Concept (target customer satisfaction)."
        ),
        RoadmapDayConfig(
            topic = "Day 7: Portion Grand Test + Building Block Model & Performance Pyramid (Units 13–14)",
            videoQuery = "Fitzgerald Moon Building Block Performance Pyramid",
            technique = "Active Recall Under Pressure: Type every 5-mark answer in 7 minutes max with numbered headings for 5 Dec Merit scoring."
        ),
        RoadmapDayConfig(
            topic = "Day 8 (Dec 3): Full-Course Merit Mock 1 & Mock 2 + Weak Topic Emergency Surgery",
            videoQuery = "ICMA Pakistan OM M3 Full Course Revision",
            technique = "Final 48h Protocol: Solve Full Mock 1 (12:45 PM) + Full Mock 2 (8:15 PM). Drill only concepts scoring under 75%."
        ),
        RoadmapDayConfig(
            topic = "Day 9 (Dec 4 — Exam Eve): Final Mock 3 + Q.1–Q.10 Headings Sweep + 11:30 PM Rest",
            videoQuery = "ICMA M3 OM Quick Revision",
            technique = "Exam-Eve Consolidation: No late-night cramming! Review Today's 5 Flashcards, scan Past Paper Q.1–Q.10 headings, sleep by 11:30 PM."
        )
    )

    fun daysToDec5Exam(): Int {
        val now = Calendar.getInstance()
        val startToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val exam = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = exam.timeInMillis - startToday.timeInMillis
        return max(0, (diffMillis / (24L * 3600L * 1000L)).toInt())
    }

    fun buildDailyBriefPlan(
        weakTopics: List<String>,
        energyMode: EnergyMode = EnergyMode.NORMAL
    ): DailyBriefPlan {
        val daysLeft = daysToDec5Exam()
        val effectiveMode = if (daysLeft in 1..2 && energyMode == EnergyMode.NORMAL) {
            EnergyMode.FINAL_SPRINT
        } else {
            energyMode
        }

        val dayIndex = when {
            daysLeft >= 9 -> 0
            daysLeft <= 1 -> emergencyNineDayRoadmap.lastIndex
            else -> (9 - daysLeft).coerceIn(0, emergencyNineDayRoadmap.lastIndex)
        }
        val roadmapDay = emergencyNineDayRoadmap[dayIndex]
        val priorityWeak = weakTopics.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Balanced Scorecard & Mintzberg 5 Parts"

        val upcomingMockAlert = when {
            daysLeft <= 0 -> "5 December Exam Hall Day: Light 20-min Flashcard warm-up only. You are ready for the Pakistan Certificate of Merit!"
            daysLeft in 1..2 -> "CRITICAL ALERT (${daysLeft}d left to 5 Dec): Final 48h Full-Course Mock 1, 2 & 3 Window — 60% MCQ + 40% Typing active!"
            daysLeft == 3 -> "ADVANCE ALERT (48h warning): 2–3 Full-Course Merit Mocks start tomorrow (Dec 3–4). Weak areas queued in today's 5 flashcards."
            daysLeft in 4..5 -> "ADVANCE ALERT (24–48h early): Portion Grand Test & Half-Book 60/40 Mock coming up — 35m revision buffer added tonight!"
            else -> "ADVANCE ALERT (2 Days Early): Weekly & Part A Grand Test scheduled in 2 days — 30m early revision built into tonight's schedule."
        }

        val morningMin = when (effectiveMode) {
            EnergyMode.BUSY_OFFICE -> 20
            EnergyMode.LOW_RECOVERY -> 20
            else -> 30
        }
        val lunchMin = if (effectiveMode == EnergyMode.BUSY_OFFICE) 20 else 30
        val break2Min = 15
        val eveningCardMin = if (effectiveMode == EnergyMode.LOW_RECOVERY) 25 else 20
        val nightFreshMin = when (effectiveMode) {
            EnergyMode.LOW_RECOVERY -> 35
            EnergyMode.BUSY_OFFICE -> 40
            EnergyMode.FINAL_SPRINT -> 70
            EnergyMode.NORMAL -> 60
        }
        val dailyTestMin = when (effectiveMode) {
            EnergyMode.LOW_RECOVERY -> 20
            EnergyMode.BUSY_OFFICE -> 25
            EnergyMode.FINAL_SPRINT -> 60
            EnergyMode.NORMAL -> 50
        }

        val slots = listOf(
            DailyRoutineSlot(
                id = "slot_0800_morning",
                timeRange = "08:00 AM – 08:30 AM",
                title = "Morning Pre-Office Topic + Lecture Clip",
                description = "Zero-prep launch: Read '${roadmapDay.topic}' & watch today's core concept video.",
                durationMinutes = morningMin,
                actionType = RoutineActionType.READ_VIDEO,
                topicFocus = roadmapDay.topic
            ),
            DailyRoutineSlot(
                id = "slot_1245_lunch_short",
                timeRange = "12:45 PM – 01:15 PM",
                title = "Office Lunch Break 1: Short Test during Office",
                description = "Fast revision of '$priorityWeak' + solve Short Test during Office (10 targeted MCQs).",
                durationMinutes = lunchMin,
                actionType = RoutineActionType.OFFICE_SHORT_TEST,
                topicFocus = priorityWeak
            ),
            DailyRoutineSlot(
                id = "slot_1530_break2",
                timeRange = "03:30 PM – 03:45 PM",
                title = "Office Tea Break 2: 1-Page Keyword Drill",
                description = "Scan bullet headings & past paper examiner points on your phone while resting your eyes.",
                durationMinutes = break2Min,
                actionType = RoutineActionType.READ_VIDEO,
                topicFocus = "OM ML-1 Past Papers Q.1–Q.10"
            ),
            DailyRoutineSlot(
                id = "slot_1730_tired_cards",
                timeRange = "05:30 PM – 05:50 PM",
                title = "Home Tired Recovery: Today's 5 Curated Flashcards",
                description = "Thakan ke waqt zero heavy reading — tap-to-flip Today's 5 Cards covering weak, yesterday & upcoming concepts.",
                durationMinutes = eveningCardMin,
                actionType = RoutineActionType.FLASHCARDS,
                topicFocus = priorityWeak
            ),
            DailyRoutineSlot(
                id = "slot_1900_fresh_deep",
                timeRange = "07:00 PM – 08:00 PM",
                title = "Fresh Mind Deep Block + Mock Revision Buffer",
                description = "Tea/dinner ke baad fresh mindset: Master units + 15m advance prep for upcoming Grand Test ($priorityWeak).",
                durationMinutes = nightFreshMin,
                actionType = RoutineActionType.READ_VIDEO,
                topicFocus = roadmapDay.topic
            ),
            DailyRoutineSlot(
                id = "slot_2015_daily_test",
                timeRange = "08:15 PM – 09:45 PM",
                title = "Test of the Day (60% MCQ / 40% Typing)",
                description = "15m recovery break ke baad: Full ICMA-pattern paper (MCQs + 5-mark Typing Questions) with AI Grading.",
                durationMinutes = dailyTestMin,
                actionType = RoutineActionType.DAILY_TEST,
                topicFocus = roadmapDay.topic
            )
        )

        val totalStudyMinutes = slots.sumOf { it.durationMinutes }
        val totalHoursFormatted = "%.1f".format(totalStudyMinutes / 60.0)

        val urduSpokenBrief = buildString {
            append("Assalam-o-Alaikum Beta! Main aapka 30-Year Finance Strategist aur 50-Year Master Teacher bol raha hoon. ")
            append("5 December 2026 ke final OM paper mein sirf $daysLeft din baqi hain, aur aaj hum Zero Preparation se Pakistan Certificate of Merit ka safar shuru kar rahe hain. ")
            append("Aaj aapke paas kul $totalHoursFormatted ghante ($totalStudyMinutes minute) ka focused study plan hai. ")
            append("Subah 8:00 se 8:30 office jaane se pehle '${roadmapDay.topic}' parhein aur video clip dekhein. ")
            append("Office mein 12:45 se 1:15 lunch break mein '$priorityWeak' dohra kar 'Short Test during Office' attempt karein. ")
            append("Dopehar 3:30 break 2 mein 15 minute past paper headings dekhein. ")
            append("Shaam 5:30 office se ghar aa kar aap thakay honge, is liye sirf Today's 5 Flashcards aaram se flip aur samajh kar yaad karein. ")
            append("Raat 7 se 8 fresh ho kar deep study aur aane wale Grand Test ki revision karein, phir 15 minute rest ke baad 8:15 par 'Test of the Day' (60% MCQ aur 40% Typing) zaroor dein, aur 11:30 PM sukoon se so jaein. ")
            append(upcomingMockAlert)
        }

        val phaseTitle = when {
            daysLeft <= 1 -> "Exam Eve (5 Dec Final Polish)"
            daysLeft == 2 -> "Final 48h: Full-Course Mocks"
            daysLeft in 3..5 -> "Mid-Sprint: Half-Book & Grand Mocks"
            else -> "Phase 1: 9-Day Zero-to-Merit Launch"
        }

        return DailyBriefPlan(
            daysToExam = daysLeft,
            energyMode = effectiveMode,
            totalStudyMinutes = totalStudyMinutes,
            examPhaseTitle = phaseTitle,
            todayTopic = roadmapDay.topic,
            todayVideoQuery = roadmapDay.videoQuery,
            upcomingMockAlert = upcomingMockAlert,
            studyTechniqueTip = roadmapDay.technique,
            urduSpokenBrief = urduSpokenBrief,
            slots = slots
        )
    }
}
