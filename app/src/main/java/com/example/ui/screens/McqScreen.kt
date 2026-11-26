package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.McqBank
import com.example.data.OmMcqBanks
import com.example.data.OmTypingQuestions
import com.example.data.QuizAttempt
import com.example.data.QuizSessionState
import com.example.data.TypingQuestion
import com.example.ui.components.McqQuizArenaContent
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.StreamUiState
import kotlin.math.roundToInt

private val OmQuickTopics = listOf(
    "Balanced Scorecard & Building Block Model",
    "Mintzberg's Effective Organization & TPM",
    "Pfeffer's 7 HR Practices & Kolb Cycle",
    "Marketing Concept, 7Ps & Ansoff Matrix",
    "MRP, OPT Drum-Buffer-Rope & Quality Circles"
)

@Composable
fun McqScreen(
    mcqGenState: AiTaskState,
    quizSession: QuizSessionState,
    attemptsState: StreamUiState<List<QuizAttempt>>,
    typingFeedbackMap: Map<String, String> = emptyMap(),
    typingGradingId: String? = null,
    onGradeTypingAnswer: (TypingQuestion, String) -> Unit = { _, _ -> },
    onSelectMcqBank: (McqBank, Boolean) -> Unit,
    onGenerateAndStartFullScreen: (String, String, Int, Boolean) -> Unit,
    onStartCurrentFullScreen: () -> Unit,
    onSelectOption: (Int, Int) -> Unit,
    onSelectQuestionIndex: (Int) -> Unit,
    onResetQuiz: () -> Unit,
    onSubmitQuiz: () -> Unit
) {
    var topicInput by remember { mutableStateOf("Balanced Scorecard & Performance Pyramid (OM Unit 14)") }
    var notesInput by remember { mutableStateOf("") }
    var questionCount by remember { mutableFloatStateOf(5f) }
    var selectedTypingIdx by remember { mutableStateOf(0) }
    var typingAnswerText by remember(selectedTypingIdx) { mutableStateOf("") }
    var showModelAnswer by remember(selectedTypingIdx) { mutableStateOf(false) }

    val shortOfficeBank = remember {
        OmMcqBanks.banks.firstOrNull { it.id == "om_office_short_test" } ?: OmMcqBanks.banks.first()
    }
    val testOfDayBank = remember {
        OmMcqBanks.banks.firstOrNull { it.id == "om_test_of_the_day" } ?: OmMcqBanks.banks.first()
    }
    val fullMockBank = remember {
        OmMcqBanks.banks.firstOrNull { it.id == "om_mock_exam" } ?: OmMcqBanks.banks.last()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Daily Tests (60% MCQ / 40% Typing)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Short Test during Office (12:45 PM) + Test of the Day (8:15 PM) & Past-Paper Typing Lab for 5 Dec Merit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Daily Emergency Exam Quick Launchers (Office Short Test, Test of the Day, Full Mock)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Today's Scheduled OM Exams",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSelectMcqBank(shortOfficeBank, true) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("launch_office_short_test_button")
                        ) {
                            Text("Short Test (Office)", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { onSelectMcqBank(testOfDayBank, true) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("launch_test_of_the_day_button")
                        ) {
                            Text("Test of the Day", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedButton(
                        onClick = { onSelectMcqBank(fullMockBank, true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("launch_full_merit_mock_button")
                    ) {
                        Icon(Icons.Rounded.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Full-Course Merit Mock (All 14 Units)", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Interactive Live Quiz Card with Full-Screen Mode Toggle
        if (quizSession.questions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inline_quiz_card")
                ) {
                    McqQuizArenaContent(
                        session = quizSession,
                        isFullScreen = false,
                        onToggleFullScreen = onStartCurrentFullScreen,
                        onSelectOption = onSelectOption,
                        onSelectQuestionIndex = onSelectQuestionIndex,
                        onResetQuiz = onResetQuiz,
                        onSubmitQuiz = onSubmitQuiz
                    )
                }
            }
        }

        // ICMA M3 OM Curated Practice Banks
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "ICMA M3 OM Practice Question Banks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(OmMcqBanks.banks, key = { it.id }) { bank ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.width(248.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = bank.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bank.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onSelectMcqBank(bank, false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Load", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Button(
                                        onClick = { onSelectMcqBank(bank, true) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("start_bank_${bank.id}")
                                    ) {
                                        Icon(Icons.Rounded.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Test", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section B: 40% Past-Paper 5-Mark Typing Exam Lab + AI Examiner Grading
        item {
            val activeQuestion = OmTypingQuestions.items.getOrElse(selectedTypingIdx) { OmTypingQuestions.items.first() }
            val aiFeedback = typingFeedbackMap[activeQuestion.id]
            val isGrading = typingGradingId == activeQuestion.id

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("typing_paper_lab_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Section B: 40% Typing Paper (5-Mark Questions)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(OmTypingQuestions.items.size) { idx ->
                            val item = OmTypingQuestions.items[idx]
                            FilterChip(
                                selected = selectedTypingIdx == idx,
                                onClick = { selectedTypingIdx = idx },
                                label = { Text("${item.topicTag} (${item.marks}m)") }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${activeQuestion.sourceBenchmark} • ${activeQuestion.marks} Marks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeQuestion.question,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = typingAnswerText,
                        onValueChange = { typingAnswerText = it },
                        label = { Text("Type your numbered 5-mark bullet answer here...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("typing_answer_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showModelAnswer = !showModelAnswer },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(if (showModelAnswer) "Hide Solution" else "Model Answer")
                        }
                        Button(
                            onClick = { onGradeTypingAnswer(activeQuestion, typingAnswerText) },
                            enabled = !isGrading && typingAnswerText.isNotBlank(),
                            modifier = Modifier
                                .weight(1.25f)
                                .minimumInteractiveComponentSize()
                                .testTag("grade_typing_button")
                        ) {
                            if (isGrading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Grading...")
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("AI Grade (0–5)")
                            }
                        }
                    }

                    AnimatedVisibility(visible = showModelAnswer) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ICMA Examiner Model Answer & Merit Keywords:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeQuestion.modelAnswer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = !aiFeedback.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "50-Yr Teacher Examiner Feedback:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = aiFeedback.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI MCQ Generator Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "AI MCQ Quiz Builder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(OmQuickTopics) { suggestion ->
                            AssistChip(
                                onClick = { topicInput = suggestion },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("Topic or Material Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mcq_topic_input")
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Paste Study Notes / Excerpt (optional)") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Questions", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "${questionCount.roundToInt()} MCQs",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = questionCount,
                            onValueChange = { questionCount = it },
                            valueRange = 3f..12f,
                            steps = 8
                        )
                    }

                    AnimatedVisibility(visible = mcqGenState is AiTaskState.Error) {
                        val msg = (mcqGenState as? AiTaskState.Error)?.message.orEmpty()
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (topicInput.isNotBlank()) {
                                    onGenerateAndStartFullScreen(
                                        topicInput.trim(),
                                        notesInput.trim(),
                                        questionCount.roundToInt(),
                                        false
                                    )
                                }
                            },
                            enabled = mcqGenState !is AiTaskState.Loading && topicInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Generate Only")
                        }

                        Button(
                            onClick = {
                                if (topicInput.isNotBlank()) {
                                    onGenerateAndStartFullScreen(
                                        topicInput.trim(),
                                        notesInput.trim(),
                                        questionCount.roundToInt(),
                                        true
                                    )
                                }
                            },
                            enabled = mcqGenState !is AiTaskState.Loading && topicInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("generate_and_start_mcq_button")
                        ) {
                            if (mcqGenState is AiTaskState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Building...")
                            } else {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Generate & Full-Screen")
                            }
                        }
                    }
                }
            }
        }

        // Past Quiz Attempts History
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = "Attempt History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        when (attemptsState) {
            is StreamUiState.Loading -> {
                item { CircularProgressIndicator() }
            }
            is StreamUiState.Error -> {
                item {
                    Text(
                        text = attemptsState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is StreamUiState.Success -> {
                if (attemptsState.data.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Complete your first MCQ test in Full-Screen mode to log your accuracy and time here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(attemptsState.data, key = { it.id }) { attempt ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attempt.topic,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${attempt.durationSeconds}s • ${if (attempt.fullScreenUsed) "Full-Screen Mode" else "Standard"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${attempt.correctCount}/${attempt.totalQuestions} (${attempt.accuracyPercent}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
