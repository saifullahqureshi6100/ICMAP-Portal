package com.example.ui.jarvis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OutlinedFlag
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PdfChapterJump
import com.example.data.TypingQuestion
import com.example.data.local.MockExamStateEntity
import com.example.data.local.TestAttemptEntity
import com.example.data.syllabus.PastPaperEntry
import com.example.data.syllabus.SyllabusDatabaseBundle
import com.example.data.syllabus.SyllabusTopic
import com.example.domain.DescriptiveEvaluationResult
import com.example.ui.components.PdfBookViewer
import com.example.viewmodel.McqRunnerState

enum class ExamLabTab(val label: String) {
    MCQS("MCQs"),
    TYPING("Typing"),
    MOCK_EXAM("3h Mock"),
    PAST_PAPERS("Past Papers")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestingAndMockScreen(
    bundle: SyllabusDatabaseBundle,
    runnerState: McqRunnerState,
    activeMockState: MockExamStateEntity?,
    testAttempts: List<TestAttemptEntity>,
    lastDescriptiveEval: DescriptiveEvaluationResult?,
    initialTab: ExamLabTab = ExamLabTab.MCQS,
    onStartAdaptiveMcq: (modeTitle: String, count: Int, topicId: String?, partCode: String?, weakOnly: Boolean, timed: Boolean) -> Unit,
    onStartFullMock: () -> Unit,
    onResumeSavedMock: () -> Unit,
    onSelectMcqOption: (Int, Int) -> Unit,
    onJumpToQuestion: (Int) -> Unit,
    onToggleFlag: (Int) -> Unit,
    onToggleUnansweredFilter: () -> Unit,
    onSubmitMcqRunner: () -> Unit,
    onExitMcqRunner: () -> Unit,
    onEvaluateDescriptive: (topicId: String, question: TypingQuestion, answer: String) -> Unit,
    onAskJarvisForQuestion: (SyllabusTopic?, String) -> Unit
) {
    var activeTab by remember { mutableStateOf(initialTab) }
    var showPastPapersPdf by remember { mutableStateOf(false) }

    LaunchedEffect(initialTab) {
        activeTab = initialTab
    }

    if (runnerState.isActive) {
        ActiveMcqRunnerView(
            state = runnerState,
            onSelectOption = onSelectMcqOption,
            onJump = onJumpToQuestion,
            onToggleFlag = onToggleFlag,
            onToggleUnansweredFilter = onToggleUnansweredFilter,
            onSubmit = onSubmitMcqRunner,
            onExit = onExitMcqRunner
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab.ordinal) {
            ExamLabTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = { Text(tab.label, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (activeTab) {
            ExamLabTab.MCQS -> {
                var timedExamMode by remember { mutableStateOf(true) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Adaptive ICMA M3 MCQ Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Prioritizes weak topics, missed questions, and unread syllabus areas. Every answer records real accuracy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Timed Exam Pressure Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Switch(checked = timedExamMode, onCheckedChange = { timedExamMode = it })
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 20, 30, 50).forEach { cnt ->
                                        Button(
                                            onClick = {
                                                onStartAdaptiveMcq("Quick $cnt Adaptive", cnt, null, null, false, timedExamMode)
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("start_quick_${cnt}_btn")
                                        ) { Text("Quick $cnt") }
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            onStartAdaptiveMcq("Weak-Topic Recovery Drill", 15, null, null, true, timedExamMode)
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("start_weak_mcq_btn")
                                    ) { Text("Weak Topics Only") }
                                }
                            }
                        }
                    }

                    item {
                        Text("Part-Wise Practice Drills", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bundle.parts.forEach { p ->
                                OutlinedButton(
                                    onClick = {
                                        onStartAdaptiveMcq("${p.title} (${p.weightPercent}%)", 15, null, p.code, false, timedExamMode)
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) { Text("${p.title} (${p.weightPercent}%)") }
                            }
                        }
                    }

                    if (testAttempts.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Recent Test History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(testAttempts.take(8), key = { it.id }) { attempt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(attempt.modeTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${attempt.correctCount}/${attempt.totalQuestions} correct • ${attempt.durationSeconds}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${attempt.accuracyPercent}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ExamLabTab.TYPING -> {
                DescriptiveTypingArena(
                    pastPapers = bundle.pastPapers,
                    topics = bundle.topics,
                    lastEval = lastDescriptiveEval,
                    onEvaluate = onEvaluateDescriptive,
                    onAskJarvis = onAskJarvisForQuestion
                )
            }

            ExamLabTab.MOCK_EXAM -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeMockState != null && activeMockState.isActive) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Saved Mock Exam In Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Remaining time: ${activeMockState.remainingSeconds / 60} mins • Question #${activeMockState.currentIndex + 1}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = onResumeSavedMock,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("resume_mock_btn")
                                    ) { Text("Resume Saved 3-Hour Mock") }
                                }
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Text("Grand Exam & 3-Hour Simulator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Simulates full ICMA M3 weighted exam distribution (Part A 35%, Part B 20%, Part C 25%, Part D 20%) with auto-save every 10 seconds, question flagging, and unanswered question filter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onStartFullMock,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("launch_3h_mock_btn")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Start 3-Hour Full Book Mock Simulator")
                                }
                            }
                        }
                    }
                }
            }

            ExamLabTab.PAST_PAPERS -> {
                var filterYear by remember { mutableStateOf("ALL") }
                val filtered = bundle.pastPapers.filter {
                    when (filterYear) {
                        "OFFICIAL" -> it.isOfficialPastPaper
                        "JARVIS" -> !it.isOfficialPastPaper
                        else -> true
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Past Papers & Exam Drills", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            FilledTonalButton(
                                onClick = { showPastPapersPdf = true },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("open_past_papers_pdf_btn")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Official PDF")
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL" to "All (${bundle.pastPapers.size})", "OFFICIAL" to "Official Past Papers", "JARVIS" to "Jarvis Practice").forEach { (k, lbl) ->
                                FilterChip(
                                    selected = filterYear == k,
                                    onClick = { filterYear = k },
                                    label = { Text(lbl) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                    items(filtered, key = { it.id }) { pp ->
                        val topic = bundle.topics.firstOrNull { it.id == pp.topicId }
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (pp.isOfficialPastPaper) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    ) {
                                        Text(
                                            text = pp.sourceTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text("${pp.marks} Marks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(pp.typingQuestion.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Model Key Answer:\n${pp.typingQuestion.modelAnswer}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { activeTab = ExamLabTab.TYPING },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) { Text("Practice Typing") }
                                    OutlinedButton(
                                        onClick = { onAskJarvisForQuestion(topic, "Give a 5-mark exam answer + Pakistani example for: ${pp.typingQuestion.question}") },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ask Jarvis")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPastPapersPdf) {
        Dialog(
            onDismissRequest = { showPastPapersPdf = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showPastPapersPdf = false }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close Past Papers PDF")
                        }
                        Text("ICMA ML-1 Official Past Papers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    PdfBookViewer(
                        assetPath = "Past_Papers_OM-ML1.pdf",
                        chapters = listOf(
                            PdfChapterJump("Official Exam Paper Page 1", 1),
                            PdfChapterJump("Suggested Answers Start", 2)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DescriptiveTypingArena(
    pastPapers: List<PastPaperEntry>,
    topics: List<SyllabusTopic>,
    lastEval: DescriptiveEvaluationResult?,
    onEvaluate: (String, TypingQuestion, String) -> Unit,
    onAskJarvis: (SyllabusTopic?, String) -> Unit
) {
    var selectedIdx by remember { mutableIntStateOf(0) }
    val activeEntry = pastPapers.getOrNull(selectedIdx) ?: return
    val activeQuestion = activeEntry.typingQuestion
    var answerDraft by remember(activeEntry.id) { mutableStateOf("") }
    var showModel by remember(activeEntry.id) { mutableStateOf(false) }

    val wordCount = answerDraft.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Question ${selectedIdx + 1}/${pastPapers.size} (${activeEntry.sourceTag})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { selectedIdx = (selectedIdx + 1) % pastPapers.size },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Next Question") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(activeQuestion.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${activeQuestion.marks} Marks • Aim for 4–5 clear headings ($wordCount words typed)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(
                        value = answerDraft,
                        onValueChange = { answerDraft = it },
                        label = { Text("Type your structured ICMA exam answer here...") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("descriptive_answer_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (answerDraft.isNotBlank()) {
                                    onEvaluate(activeEntry.topicId, activeQuestion, answerDraft)
                                }
                            },
                            enabled = answerDraft.isNotBlank(),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("evaluate_descriptive_btn")
                        ) { Text("Evaluate Answer") }
                        OutlinedButton(
                            onClick = { showModel = !showModel },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(if (showModel) "Hide Model" else "Model Answer") }
                    }
                    if (showModel) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(activeQuestion.modelAnswer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
        if (lastEval != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(lastEval.feedbackText, style = MaterialTheme.typography.bodySmall)
                        val topic = topics.firstOrNull { it.id == activeEntry.topicId }
                        OutlinedButton(
                            onClick = {
                                onAskJarvis(topic, "Improve my 5-mark exam answer for: ${activeQuestion.question}. Missing keywords: ${lastEval.missingKeywords.joinToString(", ")}")
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Ask Jarvis to Coach My Answer") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveMcqRunnerView(
    state: McqRunnerState,
    onSelectOption: (Int, Int) -> Unit,
    onJump: (Int) -> Unit,
    onToggleFlag: (Int) -> Unit,
    onToggleUnansweredFilter: () -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit
) {
    val activeQ = state.questions.getOrNull(state.currentIndex) ?: return
    val mins = state.remainingSeconds / 60
    val secs = state.remainingSeconds % 60
    val indicesToShow = if (state.showUnansweredOnly) {
        state.questions.indices.filter { it !in state.selectedAnswers }
    } else {
        state.questions.indices.toList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onExit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Exit Test")
                    }
                    Column {
                        Text(state.modeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Answered ${state.selectedAnswers.size}/${state.questions.size} • Flagged ${state.flaggedIndices.size}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (state.isTimedMode) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(String.format("%02d:%02d", mins, secs), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { (state.selectedAnswers.size.toFloat() / state.questions.size.coerceAtLeast(1)) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }

        if (state.isSubmitted) {
            item {
                val attempt = state.lastAttemptSummary
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Text(
                                text = "Result: ${state.correctCount}/${state.questions.size} (${attempt?.accuracyPercent ?: 0}%)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (!attempt?.weakTopicsCsv.isNullOrBlank()) {
                            Text("Needs Revision: ${attempt?.weakTopicsCsv}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!attempt?.strongTopicsCsv.isNullOrBlank()) {
                            Text("Strong Concepts: ${attempt?.strongTopicsCsv}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onExit, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Finish & Return to Exam Lab")
                        }
                    }
                }
            }
        }

        // Question Navigator + Unanswered Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Question ${state.currentIndex + 1} of ${state.questions.size}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = state.showUnansweredOnly,
                    onClick = onToggleUnansweredFilter,
                    label = { Text("Unanswered Only") }
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                indicesToShow.take(40).forEach { idx ->
                    val answered = idx in state.selectedAnswers
                    val flagged = idx in state.flaggedIndices
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            idx == state.currentIndex -> MaterialTheme.colorScheme.primary
                            flagged -> MaterialTheme.colorScheme.secondaryContainer
                            answered -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.clickable { onJump(idx) }
                    ) {
                        Text(
                            text = if (flagged) "${idx + 1}⚑" else "${idx + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (idx == state.currentIndex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Question Card & Options
        item {
            val selectedOpt = state.selectedAnswers[state.currentIndex]
            val isFlagged = state.currentIndex in state.flaggedIndices
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(activeQ.sourceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        IconButton(onClick = { onToggleFlag(state.currentIndex) }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                if (isFlagged) Icons.Rounded.Flag else Icons.Rounded.OutlinedFlag,
                                contentDescription = "Flag question",
                                tint = if (isFlagged) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(activeQ.mcq.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    activeQ.mcq.options.forEachIndexed { optIdx, optText ->
                        val chosen = selectedOpt == optIdx
                        val isCorrect = optIdx == activeQ.mcq.correctIndex
                        val bgColor = when {
                            state.isSubmitted && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                            state.isSubmitted && chosen && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                            chosen -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !state.isSubmitted) {
                                    onSelectOption(state.currentIndex, optIdx)
                                }
                                .testTag("mcq_option_${state.currentIndex}_$optIdx")
                        ) {
                            Text(
                                text = "${'A' + optIdx}. $optText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (chosen || (state.isSubmitted && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    if (state.isSubmitted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Why Correct / Exam Explanation:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(activeQ.mcq.explanation, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onJump(state.currentIndex - 1) },
                    enabled = state.currentIndex > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Previous") }
                OutlinedButton(
                    onClick = { onJump(state.currentIndex + 1) },
                    enabled = state.currentIndex < state.questions.lastIndex,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Next") }
                if (!state.isSubmitted) {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("submit_mcq_runner_btn")
                    ) { Text("Submit") }
                }
            }
        }
    }
}
