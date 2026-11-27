package com.example.ui.jarvis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.TopicMasterStatus
import com.example.ui.theme.JarvisBgDeep
import com.example.ui.theme.JarvisBgMidnight
import com.example.ui.theme.JarvisBlueBorder
import com.example.ui.theme.JarvisCardNavy
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisPurpleEnd
import com.example.ui.theme.JarvisPurpleStart
import com.example.ui.theme.JarvisSkyBlue
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

enum class JarvisNavDest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val inSidebar: Boolean = true,
    val isCompactBottomTab: Boolean = false
) {
    DASHBOARD("dashboard", "Home", Icons.Rounded.Home, true, true),
    SYLLABUS("syllabus", "Syllabus", Icons.Rounded.MenuBook, true, true),
    STUDY_PLANNER("study_planner", "Study Planner", Icons.Rounded.CalendarMonth, true, false),
    TOPICS("topics", "Topics", Icons.Rounded.Psychology, true, false),
    VIDEO_LIBRARY("video_library", "Video Library", Icons.Rounded.PlayCircleOutline, true, true),
    FLASHCARDS("flashcards", "Flashcards", Icons.Rounded.Style, true, false),
    MCQ_TESTS("mcq_tests", "MCQ Tests", Icons.Rounded.BuildCircle, true, true),
    TYPING_PRACTICE("typing_practice", "Typing Practice", Icons.Rounded.Computer, true, false),
    GRAND_TESTS("grand_tests", "Grand Tests", Icons.Rounded.TrackChanges, true, false),
    PAST_PAPERS("past_papers", "Past Papers", Icons.Rounded.Description, true, false),
    JARVIS("jarvis", "Jarvis Tutor", Icons.Rounded.SmartToy, true, true),
    CALL_JARVIS("call_jarvis", "Call Jarvis", Icons.Rounded.Call, true, false),
    ANALYTICS("analytics", "Analytics", Icons.Rounded.BarChart, true, false),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings, true, false),
    SEARCH("search", "Search", Icons.Rounded.Search, false, false)
}

@Composable
fun JarvisSpaceBackdrop(
    darkMode: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (darkMode) {
                    Modifier
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF0E2356), JarvisBgMidnight, JarvisBgDeep),
                                center = Offset(480f, 340f),
                                radius = 1400f
                            )
                        )
                        .drawBehind {
                            // Subtle cyber circuit grid lines matching screenshot atmosphere
                            val lineColor = JarvisCyanGlow.copy(alpha = 0.06f)
                            val accentCircle = JarvisElectricBlue.copy(alpha = 0.08f)
                            drawCircle(
                                color = accentCircle,
                                radius = size.minDimension * 0.36f,
                                center = Offset(size.width * 0.25f, size.height * 0.22f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = lineColor,
                                radius = size.minDimension * 0.48f,
                                center = Offset(size.width * 0.25f, size.height * 0.22f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, size.height * 0.14f),
                                end = Offset(size.width, size.height * 0.14f),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = lineColor,
                                start = Offset(size.width * 0.68f, 0f),
                                end = Offset(size.width * 0.68f, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                } else {
                    Modifier.background(MaterialTheme.colorScheme.background)
                }
            )
    ) {
        content()
    }
}

@Composable
fun JarvisGlassCard(
    modifier: Modifier = Modifier,
    glowCyan: Boolean = false,
    cornerRadius: Dp = 18.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    borderStrokeWidth: Dp = if (glowCyan) 1.5.dp else 1.dp,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (glowCyan) {
        Brush.linearGradient(listOf(JarvisCyanGlow, JarvisElectricBlue, JarvisCyanGlow.copy(alpha = 0.7f)))
    } else {
        Brush.linearGradient(
            listOf(
                JarvisBlueBorder.copy(alpha = 0.85f),
                JarvisCyanGlow.copy(alpha = 0.35f),
                JarvisBlueBorder.copy(alpha = 0.7f)
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(borderStrokeWidth, borderBrush),
        tonalElevation = if (glowCyan) 8.dp else 2.dp,
        shadowElevation = if (glowCyan) 10.dp else 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

enum class JarvisButtonTone { BLUE, PURPLE, DARK }

@Composable
fun JarvisGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: JarvisButtonTone = JarvisButtonTone.BLUE,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val brush = when (tone) {
        JarvisButtonTone.BLUE -> Brush.horizontalGradient(listOf(JarvisElectricBlue, JarvisSkyBlue))
        JarvisButtonTone.PURPLE -> Brush.horizontalGradient(listOf(JarvisPurpleStart, JarvisPurpleEnd))
        JarvisButtonTone.DARK -> Brush.horizontalGradient(listOf(Color(0xFF132248), Color(0xFF1B3060)))
    }
    val border = when (tone) {
        JarvisButtonTone.BLUE -> BorderStroke(1.dp, JarvisCyanGlow.copy(alpha = 0.65f))
        JarvisButtonTone.PURPLE -> BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.65f))
        JarvisButtonTone.DARK -> BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.65f))
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = border,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) brush else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun JarvisLabeledRing(
    percent: Int,
    label: String,
    size: Dp = 54.dp,
    strokeWidth: Dp = 5.dp,
    accentColor: Color = JarvisNeonGreen,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        MasteryRing(
            percent = percent,
            size = size,
            strokeWidth = strokeWidth,
            accentColor = accentColor,
            trackColor = Color(0xFF1E3261),
            centerLabel = "${percent}%"
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = JarvisTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MasteryRing(
    percent: Int,
    size: Dp = 52.dp,
    strokeWidth: Dp = 5.dp,
    accentColor: Color = JarvisNeonGreen,
    trackColor: Color = Color(0xFF1E3261),
    centerLabel: String = "${percent}%",
    modifier: Modifier = Modifier
) {
    val clamped = (percent.coerceIn(0, 100) / 100f)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accentColor, JarvisCyanGlow, accentColor)),
                startAngle = -90f,
                sweepAngle = 360f * clamped.coerceAtLeast(0.02f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun JarvisAudioWaveform(modifier: Modifier = Modifier) {
    val heights = listOf(0.25f, 0.45f, 0.7f, 0.4f, 0.95f, 0.6f, 0.85f, 0.35f, 0.75f, 0.5f, 0.3f, 0.65f, 0.4f, 0.25f)
    Canvas(modifier = modifier.height(26.dp)) {
        val count = heights.size
        val step = size.width / count
        val barW = (step * 0.48f).coerceAtLeast(3f)
        heights.forEachIndexed { idx, ratio ->
            val x = idx * step + step / 2f
            val h = size.height * ratio
            val top = (size.height - h) / 2f
            drawLine(
                brush = Brush.verticalGradient(listOf(JarvisCyanGlow, JarvisElectricBlue)),
                start = Offset(x, top),
                end = Offset(x, top + h),
                strokeWidth = barW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OnlineStatusPill(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isOnline) JarvisNeonGreen.copy(alpha = 0.16f) else JarvisGold.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, if (isOnline) JarvisNeonGreen.copy(alpha = 0.5f) else JarvisGold.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) JarvisNeonGreen else JarvisGold)
            )
            Icon(
                imageVector = if (isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                contentDescription = if (isOnline) "Online" else "Offline",
                modifier = Modifier.size(13.dp),
                tint = if (isOnline) JarvisNeonGreen else JarvisGold
            )
            Text(
                text = if (isOnline) "ONLINE" else "OFFLINE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) JarvisNeonGreen else JarvisGold
            )
        }
    }
}

@Composable
fun TopicStatusBadge(status: TopicMasterStatus) {
    val bgColor = when (status) {
        TopicMasterStatus.MASTERED -> JarvisNeonGreen.copy(alpha = 0.2f)
        TopicMasterStatus.NOT_STARTED -> Color(0xFF1A2C56)
        else -> JarvisElectricBlue.copy(alpha = 0.22f)
    }
    val textColor = when (status) {
        TopicMasterStatus.MASTERED -> JarvisNeonGreen
        TopicMasterStatus.NOT_STARTED -> JarvisTextSecondary
        else -> JarvisCyanGlow
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(0.75.dp, textColor.copy(alpha = 0.45f))
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
