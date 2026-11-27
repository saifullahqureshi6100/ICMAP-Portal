package com.example.ui.jarvis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import kotlin.math.roundToInt
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TopicProgressEntity
import com.example.data.local.VideoProgressEntity
import com.example.data.preferences.OmJarvisSettings
import com.example.data.syllabus.SyllabusTopic
import com.example.domain.AnalyticsSnapshot
import com.example.ui.theme.JarvisBgDeep
import com.example.ui.theme.JarvisBlueBorder
import com.example.ui.theme.JarvisCardInnerDark
import com.example.ui.theme.JarvisCardNavy
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

@Composable
fun HeroGreetingAndRingsSection(
    settings: OmJarvisSettings,
    analytics: AnalyticsSnapshot,
    isCompact: Boolean,
    onSelectPart: (String?) -> Unit
) {
    val partMap = remember(analytics.partProgress) {
        analytics.partProgress.associateBy { it.part.code }
    }
    val ringItems = listOf(
        Triple(analytics.readiness.syllabusPct, "Overall", null),
        Triple(partMap["PART_A"]?.completionPercent ?: 0, "Ops\nMgmt", "PART_A"),
        Triple(partMap["PART_B"]?.completionPercent ?: 0, "HRM", "PART_B"),
        Triple(partMap["PART_C"]?.completionPercent ?: 0, "Marketing", "PART_C"),
        Triple(partMap["PART_D"]?.completionPercent ?: 0, "Perf.\nMgmt", "PART_D")
    )

    val greetingBlock: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 74.dp else 104.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(listOf(JarvisCyanGlow, JarvisElectricBlue, JarvisCyanGlow)),
                        shape = CircleShape
                    )
                    .background(JarvisCardInnerDark),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_jarvis_robot),
                    contentDescription = "OM Jarvis AI Mentor",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Assalam-o-Alaikum ${settings.studentName.substringBefore(' ')}!",
                    style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = JarvisTextPrimary
                )
                Text(
                    text = "Let's master ICMA M3 together!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextCyan
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = JarvisGoldContainer.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, JarvisGoldAmber.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\"Hard Work Today,\nMerit Tomorrow\"",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = JarvisGold
                        )
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = JarvisGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    val ringsRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            ringItems.forEachIndexed { idx, (pct, lbl, partCode) ->
                val ringAccent = when (idx) {
                    2 -> JarvisGold
                    else -> JarvisNeonGreen
                }
                JarvisLabeledRing(
                    percent = pct,
                    label = lbl,
                    size = if (isCompact) 54.dp else 60.dp,
                    strokeWidth = 5.dp,
                    accentColor = ringAccent,
                    modifier = Modifier
                        .clickable { onSelectPart(partCode) }
                        .testTag("hero_ring_${partCode ?: "overall"}")
                )
            }
        }
    }

    if (isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            greetingBlock()
            ringsRow()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) { greetingBlock() }
            Spacer(Modifier.width(12.dp))
            ringsRow()
        }
    }
}

@Composable
fun TodaysMissionCard(
    priorityTopic: SyllabusTopic?,
    onContinueStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    JarvisGlassCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = JarvisCyanGlow, modifier = Modifier.size(20.dp))
            Text(
                text = "Today's Mission",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JarvisTextPrimary
            )
        }
        Spacer(Modifier.height(10.dp))
        Image(
            painter = painterResource(id = R.drawable.img_mission_team),
            contentDescription = "Today's Mission Strategy Graphic",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, JarvisBlueBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = priorityTopic?.name ?: "MRP vs ERP",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = JarvisTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Priority Topic",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = JarvisGold
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = JarvisTextCyan, modifier = Modifier.size(15.dp))
            Text("Est. Time: 1h 20m", style = MaterialTheme.typography.bodySmall, color = JarvisTextSecondary)
        }
        Spacer(Modifier.height(12.dp))
        JarvisGradientButton(
            text = "Continue Study",
            icon = Icons.Rounded.PlayArrow,
            tone = JarvisButtonTone.PURPLE,
            onClick = onContinueStudy,
            modifier = Modifier.fillMaxWidth().testTag("mission_continue_btn")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JarvisTeachersAdviceCard(
    studentFirstName: String,
    weakestTopicName: String,
    daysRemaining: Int,
    meritModeEnabled: Boolean,
    onDismiss: () -> Unit,
    onCallJarvis: () -> Unit,
    onStartStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    JarvisGlassCard(
        glowCyan = true,
        borderStrokeWidth = 1.8.dp,
        cornerRadius = 20.dp,
        containerColor = Color(0xFF0A1738).copy(alpha = 0.96f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Campaign, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(22.dp))
                Text(
                    text = "Jarvis Teacher's Advice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = JarvisGold
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp).testTag("dismiss_advice_btn")
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss advice", tint = JarvisTextSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Robot + Cyan Audio Waveform
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.width(88.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_jarvis_robot),
                    contentDescription = "Jarvis Advisor",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, JarvisCyanGlow.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                )
                JarvisAudioWaveform(modifier = Modifier.fillMaxWidth())
            }

            // Right Advice Content + Merit Badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assalam-o-Alaikum $studentFirstName!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Aaj hum pehle aapke weakest topic $weakestTopicName ko cover karenge. Phir related video, reading aur 10 MCQs complete karenge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextSecondary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = JarvisCardInnerDark,
                        border = BorderStroke(1.dp, JarvisPurpleEnd.copy(alpha = 0.8f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .background(Brush.horizontalGradient(listOf(JarvisPurpleStart, JarvisPurpleEnd)))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$daysRemaining Days Left",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (meritModeEnabled) "Certificate of Merit" else "Exam Pass Goal",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisGold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Today's Mission Inner Checklist
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = JarvisCardInnerDark.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.TrackChanges, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                            Text("Today's Mission:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = JarvisGold)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AdviceChecklistItem("$weakestTopicName – 40 min")
                            AdviceChecklistItem("1 Descriptive Answer")
                            AdviceChecklistItem("Video – 20 min")
                            AdviceChecklistItem("Revision – 15 min")
                            AdviceChecklistItem("10 MCQs")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bottom 3 Action Buttons (Call Jarvis | Start Today's Study | Later)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JarvisGradientButton(
                text = "Call Jarvis",
                icon = Icons.Rounded.Call,
                tone = JarvisButtonTone.PURPLE,
                onClick = onCallJarvis,
                modifier = Modifier.weight(1f).testTag("advice_call_jarvis_btn")
            )
            JarvisGradientButton(
                text = "Start Today's Study",
                icon = Icons.Rounded.PlayArrow,
                tone = JarvisButtonTone.BLUE,
                onClick = onStartStudy,
                modifier = Modifier.weight(1.25f).testTag("advice_start_study_btn")
            )
            JarvisGradientButton(
                text = "Later",
                tone = JarvisButtonTone.DARK,
                onClick = onDismiss,
                modifier = Modifier.weight(0.75f).testTag("advice_later_btn")
            )
        }
    }
}

@Composable
private fun AdviceChecklistItem(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = JarvisNeonGreen, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = JarvisTextPrimary)
    }
}

@Composable
fun RightStudyMediaPanel(
    activeTopic: SyllabusTopic?,
    activeVideo: VideoProgressEntity?,
    activeProgress: TopicProgressEntity?,
    onOpenVideoWorkspace: () -> Unit,
    onOpenReadingWorkspace: () -> Unit,
    onSaveQuickNote: (SyllabusTopic, String) -> Unit,
    onAskJarvis: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Video, 1 = Reading, 2 = Notes
    var noteDraft by remember(activeTopic?.id, activeProgress?.personalNotes) {
        mutableStateOf(activeProgress?.personalNotes.orEmpty())
    }
    val videoPct = activeVideo?.watchedPercent ?: 0

    JarvisGlassCard(modifier = modifier) {
        // Top 3-Tab Pill Bar: Video | Reading | Notes
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = JarvisCardInnerDark,
            border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val tabs = listOf(
                    Triple(0, "Video", Icons.Rounded.PlayCircleOutline),
                    Triple(1, "Reading", Icons.Rounded.MenuBook),
                    Triple(2, "Notes", Icons.Rounded.EditNote)
                )
                tabs.forEach { (idx, title, icon) ->
                    val active = selectedTab == idx
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .then(
                                if (active) Modifier.background(Brush.horizontalGradient(listOf(JarvisElectricBlue, JarvisSkyBlue)))
                                else Modifier
                            )
                            .clickable { selectedTab = idx }
                            .testTag("media_panel_tab_${title.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(icon, contentDescription = title, tint = if (active) Color.White else JarvisTextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else JarvisTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // Interactive Video Preview Card matching screenshot presenter & scrub bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, JarvisBlueBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenVideoWorkspace)
                        .testTag("dashboard_video_player_card")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_lecture_presenter),
                        contentDescription = activeTopic?.name ?: "Video Lecture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f))
                    )
                    // Topic Tag Overlay top-left
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisBgDeep.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Text(
                            text = activeTopic?.name ?: "MRP vs ERP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    // Centered Play Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(JarvisBgDeep.copy(alpha = 0.75f))
                            .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Media3 Lecture", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    // Bottom Scrubber HUD Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (activeVideo != null && activeVideo.lastPositionMs > 0L) {
                                        "Resume ${activeVideo.watchedPercent}%"
                                    } else "00:00 / 24:50",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Icon(Icons.Rounded.Fullscreen, contentDescription = "Fullscreen player", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (videoPct / 100f).coerceIn(0.05f, 1f) },
                            color = JarvisCyanGlow,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
            1 -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = JarvisCardInnerDark,
                    border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(activeTopic?.name ?: "Topic Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                        Text(activeTopic?.romanUrduSummary.orEmpty(), style = MaterialTheme.typography.bodySmall, color = JarvisTextSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        OutlinedButton(onClick = onOpenReadingWorkspace, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                            Text("Open Full Reader + PDF")
                        }
                    }
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        label = { Text("Quick note for ${activeTopic?.name ?: "topic"}") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { if (activeTopic != null) onSaveQuickNote(activeTopic, noteDraft) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) { Text("Save Quick Note") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = JarvisNeonGreen, modifier = Modifier.size(16.dp))
            Text(
                text = "Video Completed $videoPct%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = JarvisNeonGreen
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (videoPct / 100f).coerceIn(0f, 1f) },
            color = JarvisNeonGreen,
            trackColor = Color(0xFF1A2C56),
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.height(12.dp))
        JarvisGradientButton(
            text = "Ask Jarvis 🤖",
            icon = Icons.Rounded.SmartToy,
            tone = JarvisButtonTone.BLUE,
            onClick = onAskJarvis,
            modifier = Modifier.fillMaxWidth().testTag("panel_ask_jarvis_btn")
        )
    }
}

@Composable
fun DailyStudyGoalCard(
    todayMinutes: Int,
    targetMinutes: Int,
    onLogMinutes: (Int) -> Unit,
    onUpdateTarget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var goalSliderValue by remember { mutableStateOf(targetMinutes.toFloat()) }

    val pct = ((todayMinutes.toFloat() / targetMinutes.coerceAtLeast(1)) * 100f).roundToInt()

    JarvisGlassCard(glowCyan = true, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = JarvisCyanGlow, modifier = Modifier.size(19.dp))
                    Text("Daily Study Hour Goal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${todayMinutes}m logged of ${targetMinutes}m goal (${String.format("%.1f", todayMinutes / 60f)}h / ${String.format("%.1f", targetMinutes / 60f)}h)",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary
                )
            }
            MasteryRing(
                percent = pct,
                size = 56.dp,
                strokeWidth = 6.dp,
                accentColor = if (pct >= 100) JarvisNeonGreen else JarvisElectricBlue,
                centerLabel = "$pct%"
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (todayMinutes.toFloat() / targetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f) },
            color = if (pct >= 100) JarvisNeonGreen else JarvisCyanGlow,
            trackColor = JarvisCardInnerDark,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onLogMinutes(15) },
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("goal_log_15m_btn")
            ) { Text("+15m") }
            OutlinedButton(
                onClick = { onLogMinutes(30) },
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("goal_log_30m_btn")
            ) { Text("+30m") }
            FilledTonalButton(
                onClick = {
                    goalSliderValue = targetMinutes.toFloat()
                    showEditGoalDialog = true
                },
                modifier = Modifier.weight(1.2f).heightIn(min = 40.dp).testTag("edit_daily_goal_btn")
            ) { Text("Edit Goal") }
        }
    }

    if (showEditGoalDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditGoalDialog = false },
            title = { Text("Set Daily Study Hour Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target: ${goalSliderValue.roundToInt()} minutes (${String.format("%.1f", goalSliderValue / 60f)} hours)")
                    androidx.compose.material3.Slider(
                        value = goalSliderValue,
                        onValueChange = { goalSliderValue = it },
                        valueRange = 60f..360f,
                        steps = 19
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onUpdateTarget(goalSliderValue.roundToInt())
                        showEditGoalDialog = false
                    }
                ) { Text("Save Goal") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEditGoalDialog = false }) { Text("Cancel") }
            }
        )
    }
}

