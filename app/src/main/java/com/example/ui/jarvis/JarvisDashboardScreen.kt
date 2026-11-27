package com.example.ui.jarvis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TopicProgressEntity
import com.example.data.local.VideoProgressEntity
import com.example.data.preferences.OmJarvisSettings
import com.example.data.syllabus.SyllabusTopic
import com.example.domain.AnalyticsSnapshot
import com.example.ui.theme.JarvisBlueBorder
import com.example.ui.theme.JarvisCardInnerDark
import com.example.ui.theme.JarvisCoralRed
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGoldAmber
import com.example.ui.theme.JarvisGoldContainer
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisPurpleEnd
import com.example.ui.theme.JarvisPurpleStart
import com.example.ui.theme.JarvisSkyBlue
import com.example.ui.theme.JarvisTextCyan
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlin.math.max

@Composable
fun JarvisDashboardScreen(
    settings: OmJarvisSettings,
    analytics: AnalyticsSnapshot,
    activeTopic: SyllabusTopic?,
    activeVideo: VideoProgressEntity?,
    activeProgress: TopicProgressEntity?,
    showReadinessDialog: Boolean,
    onDismissReadinessDialog: () -> Unit,
    onContinueStudy: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onFilterPart: (String?) -> Unit,
    onOpenVideoWorkspace: () -> Unit,
    onOpenReadingWorkspace: () -> Unit,
    onSaveQuickNote: (SyllabusTopic, String) -> Unit,
    onStartQuickTest: () -> Unit,
    onStartWeakTest: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenVideos: () -> Unit,
    onAskJarvis: () -> Unit,
    onCallJarvis: () -> Unit,
    onOpenMockExam: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onLogMinutes: (Int) -> Unit,
    onUpdateDailyTarget: (Int) -> Unit
) {
    var showAdvicePanel by remember { mutableStateOf(true) }
    val priorityTopic = analytics.revisionNowTop5.firstOrNull()?.topic ?: activeTopic
    val weakestName = analytics.weakest5.firstOrNull()?.topic?.name ?: "Capacity Planning"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideTablet = maxWidth >= 860.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Greeting + Robot + Quote + 5 Circular Rings
            HeroGreetingAndRingsSection(
                settings = settings,
                analytics = analytics,
                isCompact = !isWideTablet,
                onSelectPart = onFilterPart
            )

            // Daily Study Hour Goal & Progress Ring Card
            DailyStudyGoalCard(
                todayMinutes = analytics.todayMinutes,
                targetMinutes = settings.dailyTargetMinutes,
                onLogMinutes = onLogMinutes,
                onUpdateTarget = onUpdateDailyTarget,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Middle Section: Today's Mission + Jarvis Teacher's Advice + Video/Reading/Notes Panel
            if (isWideTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    TodaysMissionCard(
                        priorityTopic = priorityTopic,
                        onContinueStudy = onContinueStudy,
                        modifier = Modifier.weight(0.24f)
                    )
                    if (showAdvicePanel) {
                        JarvisTeachersAdviceCard(
                            studentFirstName = settings.studentName.substringBefore(' '),
                            weakestTopicName = weakestName,
                            daysRemaining = analytics.daysRemaining,
                            meritModeEnabled = settings.meritModeEnabled,
                            onDismiss = { showAdvicePanel = false },
                            onCallJarvis = onCallJarvis,
                            onStartStudy = {
                                val target = priorityTopic?.id
                                if (target != null) onOpenTopic(target) else onContinueStudy()
                            },
                            modifier = Modifier.weight(0.48f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(0.48f)) {
                            AssistChip(
                                onClick = { showAdvicePanel = true },
                                label = { Text("Show Jarvis Teacher's Advice") },
                                leadingIcon = { Icon(Icons.Rounded.Campaign, contentDescription = null, tint = JarvisGold) }
                            )
                        }
                    }
                    RightStudyMediaPanel(
                        activeTopic = activeTopic ?: priorityTopic,
                        activeVideo = activeVideo,
                        activeProgress = activeProgress,
                        onOpenVideoWorkspace = onOpenVideoWorkspace,
                        onOpenReadingWorkspace = onOpenReadingWorkspace,
                        onSaveQuickNote = onSaveQuickNote,
                        onAskJarvis = onAskJarvis,
                        modifier = Modifier.weight(0.28f)
                    )
                }
            } else {
                if (showAdvicePanel) {
                    JarvisTeachersAdviceCard(
                        studentFirstName = settings.studentName.substringBefore(' '),
                        weakestTopicName = weakestName,
                        daysRemaining = analytics.daysRemaining,
                        meritModeEnabled = settings.meritModeEnabled,
                        onDismiss = { showAdvicePanel = false },
                        onCallJarvis = onCallJarvis,
                        onStartStudy = {
                            val target = priorityTopic?.id
                            if (target != null) onOpenTopic(target) else onContinueStudy()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    AssistChip(
                        onClick = { showAdvicePanel = true },
                        label = { Text("Show Jarvis Teacher's Advice") },
                        leadingIcon = { Icon(Icons.Rounded.Campaign, contentDescription = null, tint = JarvisGold) }
                    )
                }
                TodaysMissionCard(
                    priorityTopic = priorityTopic,
                    onContinueStudy = onContinueStudy,
                    modifier = Modifier.fillMaxWidth()
                )
                RightStudyMediaPanel(
                    activeTopic = activeTopic ?: priorityTopic,
                    activeVideo = activeVideo,
                    activeProgress = activeProgress,
                    onOpenVideoWorkspace = onOpenVideoWorkspace,
                    onOpenReadingWorkspace = onOpenReadingWorkspace,
                    onSaveQuickNote = onSaveQuickNote,
                    onAskJarvis = onAskJarvis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Bottom Row: Weak Areas | Quick Actions | Study Analytics | Daily Brief
            if (isWideTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    WeakAreasCard(
                        analytics = analytics,
                        onOpenTopic = onOpenTopic,
                        onReviseNow = onStartWeakTest,
                        modifier = Modifier.weight(0.25f)
                    )
                    QuickActionsCard(
                        onContinueStudy = onContinueStudy,
                        onTakeTest = onStartQuickTest,
                        onFlashcards = onOpenFlashcards,
                        onVideos = onOpenVideos,
                        onAskJarvis = onAskJarvis,
                        onCallJarvis = onCallJarvis,
                        onMockExam = onOpenMockExam,
                        onAnalytics = onOpenAnalytics,
                        modifier = Modifier.weight(0.26f)
                    )
                    StudyAnalyticsMiniCard(
                        analytics = analytics,
                        onOpenFullAnalytics = onOpenAnalytics,
                        modifier = Modifier.weight(0.26f)
                    )
                    DailyBriefCard(
                        weakestTopicName = weakestName,
                        onClickBriefItem = onStartQuickTest,
                        modifier = Modifier.weight(0.23f)
                    )
                }
            } else {
                WeakAreasCard(
                    analytics = analytics,
                    onOpenTopic = onOpenTopic,
                    onReviseNow = onStartWeakTest,
                    modifier = Modifier.fillMaxWidth()
                )
                QuickActionsCard(
                    onContinueStudy = onContinueStudy,
                    onTakeTest = onStartQuickTest,
                    onFlashcards = onOpenFlashcards,
                    onVideos = onOpenVideos,
                    onAskJarvis = onAskJarvis,
                    onCallJarvis = onCallJarvis,
                    onMockExam = onOpenMockExam,
                    onAnalytics = onOpenAnalytics,
                    modifier = Modifier.fillMaxWidth()
                )
                StudyAnalyticsMiniCard(
                    analytics = analytics,
                    onOpenFullAnalytics = onOpenAnalytics,
                    modifier = Modifier.fillMaxWidth()
                )
                DailyBriefCard(
                    weakestTopicName = weakestName,
                    onClickBriefItem = onStartQuickTest,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showReadinessDialog) {
        AlertDialog(
            onDismissRequest = onDismissReadinessDialog,
            title = { Text("OM Jarvis Readiness Score: ${analytics.readiness.readinessScore}%") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(analytics.readiness.explanation, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("• Weighted Syllabus: ${analytics.readiness.syllabusPct}%")
                    Text("• Video Completion: ${analytics.readiness.videoPct}%")
                    Text("• MCQ Accuracy: ${analytics.readiness.mcqAccuracyPct}%")
                    Text("• Descriptive Practice: ${analytics.readiness.descriptivePct}%")
                    Text("• Flashcard Mastery: ${analytics.readiness.flashcardPct}%")
                    Text("• 7-Day Consistency: ${analytics.readiness.consistencyPct}%")
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissReadinessDialog) { Text("Close") }
            }
        )
    }
}

@Composable
private fun WeakAreasCard(
    analytics: AnalyticsSnapshot,
    onOpenTopic: (String) -> Unit,
    onReviseNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weakTop3 = analytics.weakest5.take(3)
    JarvisGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(19.dp))
                Text("Weak Areas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
            }
        }
        Spacer(Modifier.height(10.dp))
        weakTop3.forEachIndexed { idx, item ->
            val barColor = when (idx) {
                0 -> JarvisCoralRed
                1 -> JarvisGoldAmber
                else -> JarvisNeonGreen
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTopic(item.topic.id) }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.topic.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = JarvisTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("${item.masteryPercent}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1A2C56))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((item.masteryPercent / 100f).coerceIn(0.06f, 1f))
                            .height(7.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        JarvisGradientButton(
            text = "Revise Now",
            icon = Icons.Rounded.Refresh,
            tone = JarvisButtonTone.PURPLE,
            onClick = onReviseNow,
            modifier = Modifier.fillMaxWidth().testTag("weak_revise_now_btn")
        )
    }
}

@Composable
private fun QuickActionsCard(
    onContinueStudy: () -> Unit,
    onTakeTest: () -> Unit,
    onFlashcards: () -> Unit,
    onVideos: () -> Unit,
    onAskJarvis: () -> Unit,
    onCallJarvis: () -> Unit,
    onMockExam: () -> Unit,
    onAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple("Continue\nStudy", Icons.Rounded.PlayArrow, JarvisSkyBlue) to onContinueStudy,
        Triple("Take\nTest", Icons.Rounded.Assignment, JarvisCyanGlow) to onTakeTest,
        Triple("Flashcards", Icons.Rounded.Style, JarvisPurpleEnd) to onFlashcards,
        Triple("Videos", Icons.Rounded.VideoLibrary, JarvisElectricBlue) to onVideos,
        Triple("Ask\nJarvis", Icons.Rounded.SmartToy, JarvisSkyBlue) to onAskJarvis,
        Triple("Call\nJarvis", Icons.Rounded.Call, JarvisCyanGlow) to onCallJarvis,
        Triple("Mock\nExam", Icons.Rounded.EmojiEvents, JarvisGold) to onMockExam,
        Triple("Analytics", Icons.Rounded.BarChart, JarvisNeonGreen) to onAnalytics
    )

    JarvisGlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(19.dp))
            Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { (meta, clickAction) ->
                        val (title, icon, tint) = meta
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisCardInnerDark,
                            border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 66.dp)
                                .clickable(onClick = clickAction)
                                .testTag("quick_action_${title.replace('\n', '_').lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisTextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyAnalyticsMiniCard(
    analytics: AnalyticsSnapshot,
    onOpenFullAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(0) } // 0=Weekly, 1=Monthly, 2=Overall
    val series = analytics.dailySeriesLast7Days
    val totalMins = when (mode) {
        0 -> analytics.weeklyMinutes
        1 -> analytics.weeklyMinutes
        else -> analytics.weeklyMinutes
    }
    val h = totalMins / 60
    val m = totalMins % 60

    JarvisGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFullAnalytics),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.BarChart, contentDescription = null, tint = JarvisCyanGlow, modifier = Modifier.size(19.dp))
                Text("Study Analytics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Weekly / Monthly / Overall Segmented Pills
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Weekly", "Monthly", "Overall").forEachIndexed { idx, lbl ->
                val active = mode == idx
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) JarvisElectricBlue else JarvisCardInnerDark,
                    border = BorderStroke(1.dp, if (active) JarvisCyanGlow.copy(alpha = 0.7f) else JarvisBlueBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).clickable { mode = idx }
                ) {
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else JarvisTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Glowing Purple-Blue-Cyan Gradient Bar Chart matching screenshot
        val maxVal = max(60, series.maxOfOrNull { it.second } ?: 60)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clickable(onClick = onOpenFullAnalytics)
        ) {
            val n = series.size.coerceAtLeast(1)
            val slotW = size.width / n
            val barW = slotW * 0.48f
            series.forEachIndexed { idx, (_, mins) ->
                val x = idx * slotW + (slotW - barW) / 2f
                val ratio = (mins.toFloat() / maxVal.toFloat()).coerceIn(0.14f, 1f)
                val barH = size.height * ratio
                val topColor = when {
                    idx <= 1 -> JarvisPurpleEnd
                    idx <= 3 -> JarvisElectricBlue
                    else -> JarvisCyanGlow
                }
                val botColor = when {
                    idx <= 1 -> JarvisPurpleStart
                    idx <= 3 -> Color(0xFF1D4ED8)
                    else -> JarvisElectricBlue
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(topColor, botColor)),
                    topLeft = Offset(x, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            series.forEach { (day, _) ->
                Text(day, style = MaterialTheme.typography.labelSmall, color = JarvisTextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = JarvisCardInnerDark,
            border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFullAnalytics)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = JarvisCyanGlow, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Study Time: ${h}h ${m}m this week",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextPrimary
                )
            }
        }
    }
}

@Composable
private fun DailyBriefCard(
    weakestTopicName: String,
    onClickBriefItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    JarvisGlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(19.dp))
            Text("Daily Brief", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        val items = listOf(
            "$weakestTopicName Review",
            "10 MCQs on OM",
            "1 Descriptive Answer",
            "Revise Key Points"
        )
        items.forEach { text ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClickBriefItem)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = JarvisNeonGreen, modifier = Modifier.size(16.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = JarvisTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = JarvisGoldContainer.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, JarvisGoldAmber.copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\"You are on the right track!\nKeep Going!\"",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisGold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("— OM Jarvis", style = MaterialTheme.typography.labelSmall, color = JarvisTextPrimary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = JarvisGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
