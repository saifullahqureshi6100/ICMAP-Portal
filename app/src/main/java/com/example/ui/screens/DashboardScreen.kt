package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DailyBriefPlan
import com.example.data.EnergyMode
import com.example.data.FullScreenMode
import com.example.data.MaterialProgress
import com.example.data.McqBank
import com.example.data.PomodoroPhase
import com.example.data.PomodoroState
import com.example.data.PreloadedMaterials
import com.example.data.QuizAttempt
import com.example.data.RoutineActionType
import com.example.data.StudyMaterial
import com.example.data.StudyPlanItem
import com.example.ui.components.PomodoroTimerCard
import com.example.ui.components.StrategistBriefCard
import com.example.ui.components.StudyRoadmapCard
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.DashboardSummary
import com.example.viewmodel.StreamUiState
import androidx.compose.material.icons.rounded.Timer

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    pomodoroState: PomodoroState,
    materialsState: StreamUiState<List<StudyMaterial>>,
    progressMapState: StreamUiState<Map<String, MaterialProgress>>,
    plansState: StreamUiState<List<StudyPlanItem>>,
    attemptsState: StreamUiState<List<QuizAttempt>>,
    planGenState: AiTaskState,
    dailyBriefPlan: DailyBriefPlan,
    aiBriefText: String?,
    briefState: AiTaskState,
    completedRoutineIds: Set<String>,
    weakTopics: List<String>,
    onSelectEnergyMode: (EnergyMode) -> Unit,
    onToggleRoutineSlot: (String) -> Unit,
    onGenerateAiBrief: () -> Unit,
    onLaunchRoutineAction: (RoutineActionType, String) -> Unit,
    onTogglePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSkipPomodoro: () -> Unit,
    onSelectPomodoroPhase: (PomodoroPhase) -> Unit,
    onUpdatePomodoroConfig: (Int, Int, Int, Int, Boolean) -> Unit,
    onLaunchFullScreenPomodoro: () -> Unit,
    onUpdateGoal: (String, Long) -> Unit,
    onLogMinutes: (Long) -> Unit,
    onAddPlanItem: (String, String, String, Long) -> Unit,
    onTogglePlanItem: (String, Boolean) -> Unit,
    onDeletePlanItem: (String) -> Unit,
    onGenerateAiPlan: (String) -> Unit,
    onLaunchFullScreenMaterial: (StudyMaterial, Boolean) -> Unit,
    onToggleMaterialComplete: (String, Boolean, Long) -> Unit,
    onLaunchMcqBank: (McqBank) -> Unit,
    onLaunchFullScreenRoadmap: () -> Unit,
    onLaunchFullScreenMcq: () -> Unit,
    onLaunchFullScreenFlashcards: () -> Unit,
    onLaunchFullScreenTutor: () -> Unit
) {
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showAddPlanDialog by remember { mutableStateOf(false) }

    val materials = (materialsState as? StreamUiState.Success)?.data ?: PreloadedMaterials.items
    val progressMap = (progressMapState as? StreamUiState.Success)?.data ?: emptyMap()
    val plansList = (plansState as? StreamUiState.Success)?.data ?: emptyList()
    val attemptsList = (attemptsState as? StreamUiState.Success)?.data ?: emptyList()
    val firstReading = materials.firstOrNull { it.id == "om_study_text_2026" } ?: materials.first()
    val pastPapers = materials.firstOrNull { it.id == "om_past_papers_ml1" } ?: firstReading
    val firstVideo = materials.firstOrNull { it.id == "drive_file_3" } ?: materials.first()

    val overallProgress = remember(summary) {
        val matRatio = if (summary.totalMaterials > 0) summary.completedMaterials.toFloat() / summary.totalMaterials else 0f
        val planRatio = if (summary.totalPlans > 0) summary.completedPlans.toFloat() / summary.totalPlans else 0f
        if (summary.totalPlans > 0) ((matRatio + planRatio) / 2f).coerceIn(0f, 1f) else matRatio.coerceIn(0f, 1f)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. 30-Yr Strategist & 50-Yr Master Teacher Urdu Daily Brief + Office Routine
        item {
            StrategistBriefCard(
                plan = dailyBriefPlan,
                aiBriefText = aiBriefText,
                briefState = briefState,
                completedRoutineIds = completedRoutineIds,
                weakTopics = weakTopics,
                onSelectEnergyMode = onSelectEnergyMode,
                onToggleRoutineSlot = onToggleRoutineSlot,
                onGenerateAiBrief = onGenerateAiBrief,
                onLaunchRoutineAction = onLaunchRoutineAction,
                onAskTeacher = onLaunchFullScreenTutor
            )
        }

        // 1. Hero Mastery & Goal Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "OM MERIT TRACKING • 5 DEC 2026",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                            )
                            Text(
                                text = summary.profile?.studyGoal ?: "OM M3 Certificate of Merit (5 Dec 2026)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${summary.profile?.totalStudyMinutes ?: 0} min logged",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "•  ${summary.quizCount} quizzes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { showEditGoalDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("edit_goal_button")
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Goal", style = MaterialTheme.typography.labelMedium)
                                }
                                FilledTonalButton(
                                    onClick = { onLogMinutes(15L) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("log_15_min_button")
                                ) {
                                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("+15m", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        MasteryRing(
                            progress = overallProgress,
                            ringColor = MaterialTheme.colorScheme.secondaryContainer,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            textColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // 2. Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Materials",
                    value = "${summary.completedMaterials}/${summary.totalMaterials}",
                    subtitle = "Completed"
                )
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Plan Tasks",
                    value = "${summary.completedPlans}/${summary.totalPlans}",
                    subtitle = "Done"
                )
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    title = "MCQ Avg",
                    value = if (summary.quizCount > 0) "${summary.averageQuizAccuracy}%" else "--",
                    subtitle = "${summary.quizCount} attempts"
                )
            }
        }

        // 2b. Visual Study Roadmap & Upcoming Milestones
        item {
            StudyRoadmapCard(
                materials = materials,
                progressMap = progressMap,
                plans = plansList,
                attempts = attemptsList,
                onOpenMaterial = onLaunchFullScreenMaterial,
                onToggleMaterialComplete = onToggleMaterialComplete,
                onLaunchMcqBank = onLaunchMcqBank,
                onTogglePlanItem = onTogglePlanItem,
                isFullScreen = false,
                onToggleFullScreen = onLaunchFullScreenRoadmap
            )
        }

        // 3. Full-Screen Quick Launchers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Launch Full-Screen Study Modes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        FullScreenQuickCard(
                            title = "OM Book PDF (254p)",
                            subtitle = firstReading.title,
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            tag = "quick_fullscreen_reading",
                            onClick = { onLaunchFullScreenMaterial(firstReading, false) }
                        )
                    }
                    item {
                        FullScreenQuickCard(
                            title = "OM Past Papers",
                            subtitle = pastPapers.title,
                            icon = Icons.Rounded.PictureAsPdf,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            tag = "quick_fullscreen_past_papers",
                            onClick = { onLaunchFullScreenMaterial(pastPapers, false) }
                        )
                    }
                    item {
                        FullScreenQuickCard(
                            title = "Video Clip Full-Screen",
                            subtitle = firstVideo.title,
                            icon = Icons.Rounded.PlayCircle,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            tag = "quick_fullscreen_video",
                            onClick = { onLaunchFullScreenMaterial(firstVideo, true) }
                        )
                    }
                    item {
                        FullScreenQuickCard(
                            title = "MCQs Full-Screen",
                            subtitle = "Distraction-free exam mode",
                            icon = Icons.Rounded.Quiz,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            tag = "quick_fullscreen_mcq",
                            onClick = onLaunchFullScreenMcq
                        )
                    }
                    item {
                        FullScreenQuickCard(
                            title = "Flashcards Flip",
                            subtitle = "Shuffle & active recall deck",
                            icon = Icons.Rounded.Style,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            tag = "quick_fullscreen_flashcards",
                            onClick = onLaunchFullScreenFlashcards
                        )
                    }
                    item {
                        FullScreenQuickCard(
                            title = "AI Tutor Full-Screen",
                            subtitle = "Socratic coach & notes helper",
                            icon = Icons.Rounded.AutoAwesome,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            tag = "quick_fullscreen_tutor",
                            onClick = onLaunchFullScreenTutor
                        )
                    }
                    item {
                        FullScreenQuickCard(
                            title = "Pomodoro Full-Screen",
                            subtitle = "${pomodoroState.workMinutes}m work / ${pomodoroState.shortBreakMinutes}m break",
                            icon = Icons.Rounded.Timer,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            tag = "quick_fullscreen_pomodoro",
                            onClick = onLaunchFullScreenPomodoro
                        )
                    }
                }
            }
        }

        // 3b. Configurable Pomodoro Focus Timer Card
        item {
            PomodoroTimerCard(
                state = pomodoroState,
                onToggleRunPause = onTogglePomodoro,
                onReset = onResetPomodoro,
                onSkipPhase = onSkipPomodoro,
                onSelectPhase = onSelectPomodoroPhase,
                onUpdateConfig = onUpdatePomodoroConfig,
                onOpenFullScreen = onLaunchFullScreenPomodoro
            )
        }

        // 4. Study Plan Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Study Plan Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Check off tasks or let AI build a plan from your Drive links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showAddPlanDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_plan_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Task")
                    }
                    Button(
                        onClick = {
                            onGenerateAiPlan(summary.profile?.studyGoal ?: "Master all Drive study modules")
                        },
                        enabled = planGenState !is AiTaskState.Loading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("ai_generate_plan_button")
                    ) {
                        if (planGenState is AiTaskState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("AI Plan")
                    }
                }
            }

            AnimatedVisibility(visible = planGenState is AiTaskState.Error) {
                val msg = (planGenState as? AiTaskState.Error)?.message.orEmpty()
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 5. Study Plan Items
        when (plansState) {
            is StreamUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is StreamUiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = plansState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is StreamUiState.Success -> {
                if (plansState.data.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "No Study Plan Tasks Yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tap \"AI Plan\" above to auto-generate a personalized schedule around your 6 Drive links, or add a custom task.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(plansState.data, key = { it.id }) { plan ->
                        StudyPlanRow(
                            item = plan,
                            onToggle = { onTogglePlanItem(plan.id, !plan.completed) },
                            onDelete = { onDeletePlanItem(plan.id) }
                        )
                    }
                }
            }
        }

        // 6. Recent Quiz Progress
        if (attemptsState is StreamUiState.Success && attemptsState.data.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recent MCQ Accuracy",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(attemptsState.data.take(3), key = { it.id }) { attempt ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = attempt.topic,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${attempt.correctCount}/${attempt.totalQuestions} (${attempt.accuracyPercent}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (attempt.accuracyPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (showEditGoalDialog) {
        EditGoalDialog(
            currentGoal = summary.profile?.studyGoal ?: "Master Core Drive Modules",
            currentMinutes = summary.profile?.dailyTargetMinutes ?: 45L,
            onDismiss = { showEditGoalDialog = false },
            onSave = { goal, mins ->
                onUpdateGoal(goal, mins)
                showEditGoalDialog = false
            }
        )
    }

    if (showAddPlanDialog) {
        AddPlanItemDialog(
            onDismiss = { showAddPlanDialog = false },
            onAdd = { title, topic, day, mins ->
                onAddPlanItem(title, topic, day, mins)
                showAddPlanDialog = false
            }
        )
    }
}

@Composable
private fun MasteryRing(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mastery",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MetricStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun FullScreenQuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .width(185.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = "Enter Full-Screen",
                        tint = contentColor,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudyPlanRow(
    item: StudyPlanItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (item.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (item.completed) "Mark incomplete" else "Mark completed",
                    tint = if (item.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${item.topic} • ${item.targetMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete plan item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditGoalDialog(
    currentGoal: String,
    currentMinutes: Long,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal) }
    var minutesText by remember { mutableStateOf(currentMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Study Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("Exam or Study Goal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Daily Target (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = minutesText.toLongOrNull()?.coerceIn(5L, 720L) ?: 45L
                    onSave(goalText.ifBlank { "Master Core Modules" }, mins)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddPlanItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("Core Module") }
    var dayLabel by remember { mutableStateOf("Today") }
    var minutes by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Study Plan Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g., Read Doc 6 & solve 10 MCQs") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayLabel,
                        onValueChange = { dayLabel = it },
                        label = { Text("Day") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            title.trim(),
                            topic.ifBlank { "General" },
                            dayLabel.ifBlank { "Today" },
                            minutes.toLongOrNull()?.coerceIn(5L, 480L) ?: 30L
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
