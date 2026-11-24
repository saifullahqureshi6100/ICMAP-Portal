package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MaterialProgress
import com.example.data.McqBank
import com.example.data.OmMcqBanks
import com.example.data.QuizAttempt
import com.example.data.StudyMaterial
import com.example.data.StudyPlanItem
import com.example.ui.theme.EmeraldSuccess

enum class RoadmapNodeStatus(val badge: String) {
    COMPLETED("COMPLETED"),
    IN_PROGRESS("ACTIVE"),
    NEXT_MILESTONE("NEXT MILESTONE"),
    UPCOMING("UPCOMING")
}

private enum class RoadmapViewMode(val label: String) {
    MODULES("Study Modules"),
    PLAN_MILESTONES("Plan Milestones")
}

private data class RoadmapModuleNode(
    val id: String,
    val stepNumber: String,
    val title: String,
    val subtitle: String,
    val milestoneLabel: String,
    val icon: ImageVector,
    val status: RoadmapNodeStatus,
    val progressFraction: Float,
    val linkedMaterial: StudyMaterial?,
    val linkedBank: McqBank?,
    val bestQuizScore: Int?,
    val materialDone: Boolean
)

@Composable
fun StudyRoadmapCard(
    materials: List<StudyMaterial>,
    progressMap: Map<String, MaterialProgress>,
    plans: List<StudyPlanItem>,
    attempts: List<QuizAttempt>,
    onOpenMaterial: (StudyMaterial, Boolean) -> Unit,
    onToggleMaterialComplete: (String, Boolean, Long) -> Unit,
    onLaunchMcqBank: (McqBank) -> Unit,
    onTogglePlanItem: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null
) {
    var viewMode by remember { mutableStateOf(RoadmapViewMode.MODULES) }
    var expandedNodeId by remember { mutableStateOf<String?>(null) }

    val roadmapNodes = remember(materials, progressMap, attempts) {
        buildModuleNodes(materials, progressMap, attempts)
    }

    val completedCount = roadmapNodes.count { it.status == RoadmapNodeStatus.COMPLETED }
    val overallRoadmapProgress by animateFloatAsState(
        targetValue = if (roadmapNodes.isNotEmpty()) {
            roadmapNodes.map { it.progressFraction }.average().toFloat().coerceIn(0f, 1f)
        } else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "roadmap_progress"
    )

    val nextMilestoneNode = remember(roadmapNodes) {
        roadmapNodes.firstOrNull {
            it.status == RoadmapNodeStatus.NEXT_MILESTONE || it.status == RoadmapNodeStatus.IN_PROGRESS
        } ?: roadmapNodes.firstOrNull { it.status == RoadmapNodeStatus.UPCOMING }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_roadmap_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Route,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "STUDY ROADMAP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "$completedCount of ${roadmapNodes.size} modules mastered",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${(overallRoadmapProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    if (onToggleFullScreen != null) {
                        IconButton(
                            onClick = onToggleFullScreen,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("roadmap_fullscreen_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = if (isFullScreen) "Exit Full-Screen Roadmap" else "Full-Screen Roadmap"
                            )
                        }
                    }
                }
            }

            // Overall Progress Rail
            LinearProgressIndicator(
                progress = { overallRoadmapProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Upcoming Milestone Spotlight Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (nextMilestoneNode != null) Icons.Rounded.Flag else Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nextMilestoneNode != null) "UPCOMING MILESTONE" else "ALL MILESTONES COMPLETE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = nextMilestoneNode?.milestoneLabel
                                ?: "You've completed every core study module and checkpoint!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Switcher between Study Modules & Plan Milestones
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoadmapViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("roadmap_mode_${mode.name.lowercase()}")
                    )
                }
            }

            when (viewMode) {
                RoadmapViewMode.MODULES -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        roadmapNodes.forEachIndexed { index, node ->
                            val prevCompleted = index > 0 && roadmapNodes[index - 1].status == RoadmapNodeStatus.COMPLETED
                            val isExpanded = expandedNodeId == node.id ||
                                (expandedNodeId == null && node.status == RoadmapNodeStatus.NEXT_MILESTONE)
                            RoadmapModuleRow(
                                node = node,
                                isFirst = index == 0,
                                isLast = index == roadmapNodes.lastIndex,
                                prevCompleted = prevCompleted,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedNodeId = if (expandedNodeId == node.id) "" else node.id
                                },
                                onOpenMaterial = onOpenMaterial,
                                onToggleMaterialComplete = onToggleMaterialComplete,
                                onLaunchMcqBank = onLaunchMcqBank
                            )
                        }
                    }
                }

                RoadmapViewMode.PLAN_MILESTONES -> {
                    if (plans.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "No scheduled plan milestones yet",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Tap \"AI Plan\" or \"Task\" below in Study Plan Schedule to populate your personal milestone track.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val firstPendingIdx = plans.indexOfFirst { !it.completed }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            plans.forEachIndexed { idx, plan ->
                                val status = when {
                                    plan.completed -> RoadmapNodeStatus.COMPLETED
                                    idx == firstPendingIdx -> RoadmapNodeStatus.NEXT_MILESTONE
                                    else -> RoadmapNodeStatus.UPCOMING
                                }
                                val prevDone = idx > 0 && plans[idx - 1].completed
                                PlanMilestoneTimelineRow(
                                    index = idx,
                                    item = plan,
                                    status = status,
                                    isFirst = idx == 0,
                                    isLast = idx == plans.lastIndex,
                                    prevCompleted = prevDone,
                                    onToggle = { onTogglePlanItem(plan.id, !plan.completed) }
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
private fun RoadmapModuleRow(
    node: RoadmapModuleNode,
    isFirst: Boolean,
    isLast: Boolean,
    prevCompleted: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenMaterial: (StudyMaterial, Boolean) -> Unit,
    onToggleMaterialComplete: (String, Boolean, Long) -> Unit,
    onLaunchMcqBank: (McqBank) -> Unit
) {
    val activeAccent = when (node.status) {
        RoadmapNodeStatus.COMPLETED -> EmeraldSuccess
        RoadmapNodeStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
        RoadmapNodeStatus.NEXT_MILESTONE -> MaterialTheme.colorScheme.tertiary
        RoadmapNodeStatus.UPCOMING -> MaterialTheme.colorScheme.outline
    }

    val pulseTransition = rememberInfiniteTransition(label = "node_pulse")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Left Timeline Connector & Node Marker
        RoadmapTrackRail(
            status = node.status,
            stepText = node.stepNumber,
            isFirst = isFirst,
            isLast = isLast,
            prevCompleted = prevCompleted,
            activeAccent = activeAccent,
            haloScale = haloScale,
            completedColor = EmeraldSuccess,
            upcomingTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )

        // Right Module Content Card
        val cardColor by animateColorAsState(
            targetValue = when (node.status) {
                RoadmapNodeStatus.COMPLETED -> EmeraldSuccess.copy(alpha = 0.08f)
                RoadmapNodeStatus.NEXT_MILESTONE -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
                RoadmapNodeStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                RoadmapNodeStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
            label = "node_card_color"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = if (isLast) 2.dp else 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onToggleExpand)
                .testTag("roadmap_node_${node.id}")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activeAccent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = node.status.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (node.bestQuizScore != null) {
                                Text(
                                    text = "Quiz ${node.bestQuizScore}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = node.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) 4 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse module" else "Expand module",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { node.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = activeAccent,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = node.icon,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Milestone: ${node.milestoneLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (node.linkedMaterial != null) {
                                Button(
                                    onClick = {
                                        onOpenMaterial(
                                            node.linkedMaterial,
                                            node.linkedMaterial.id == "drive_file_3"
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("roadmap_open_${node.id}")
                                ) {
                                    Icon(
                                        imageVector = if (node.linkedMaterial.id == "drive_file_3") {
                                            Icons.Rounded.PlayCircle
                                        } else {
                                            Icons.AutoMirrored.Rounded.MenuBook
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Study", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            if (node.linkedBank != null) {
                                OutlinedButton(
                                    onClick = { onLaunchMcqBank(node.linkedBank) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("roadmap_quiz_${node.id}")
                                ) {
                                    Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Quiz", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            if (node.linkedMaterial != null) {
                                OutlinedButton(
                                    onClick = {
                                        onToggleMaterialComplete(
                                            node.linkedMaterial.id,
                                            !node.materialDone,
                                            if (!node.materialDone) node.linkedMaterial.estimatedMinutes else 0L
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = if (node.materialDone) {
                                        ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                                    } else {
                                        ButtonDefaults.outlinedButtonColors()
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("roadmap_complete_${node.id}")
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Toggle completion", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (node.materialDone) "Done" else "Mark",
                                        style = MaterialTheme.typography.labelMedium
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

@Composable
private fun PlanMilestoneTimelineRow(
    index: Int,
    item: StudyPlanItem,
    status: RoadmapNodeStatus,
    isFirst: Boolean,
    isLast: Boolean,
    prevCompleted: Boolean,
    onToggle: () -> Unit
) {
    val activeAccent = when (status) {
        RoadmapNodeStatus.COMPLETED -> EmeraldSuccess
        RoadmapNodeStatus.NEXT_MILESTONE, RoadmapNodeStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        RoadmapNodeStatus.UPCOMING -> MaterialTheme.colorScheme.outline
    }

    val pulseTransition = rememberInfiniteTransition(label = "plan_pulse")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plan_halo"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        RoadmapTrackRail(
            status = status,
            stepText = "%02d".format(index + 1),
            isFirst = isFirst,
            isLast = isLast,
            prevCompleted = prevCompleted,
            activeAccent = activeAccent,
            haloScale = haloScale,
            completedColor = EmeraldSuccess,
            upcomingTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = when (status) {
                RoadmapNodeStatus.COMPLETED -> EmeraldSuccess.copy(alpha = 0.08f)
                RoadmapNodeStatus.NEXT_MILESTONE -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = if (isLast) 2.dp else 10.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onToggle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = status.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = activeAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• ${item.dayLabel} • ${item.targetMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = if (item.completed) "Completed milestone" else "Mark milestone complete",
                    tint = if (item.completed) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
private fun RoadmapTrackRail(
    status: RoadmapNodeStatus,
    stepText: String,
    isFirst: Boolean,
    isLast: Boolean,
    prevCompleted: Boolean,
    activeAccent: Color,
    haloScale: Float,
    completedColor: Color,
    upcomingTrackColor: Color
) {
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    Box(
        modifier = Modifier
            .width(38.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width / 2f
            val nodeCenterY = 24.dp.toPx()
            val strokePx = 3.dp.toPx()

            if (!isFirst) {
                val topDone = prevCompleted || status == RoadmapNodeStatus.COMPLETED
                drawLine(
                    color = if (topDone) completedColor else upcomingTrackColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, nodeCenterY - 14.dp.toPx()),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round,
                    pathEffect = if (topDone) null else dashEffect
                )
            }

            if (!isLast) {
                val bottomDone = status == RoadmapNodeStatus.COMPLETED
                drawLine(
                    color = if (bottomDone) completedColor else upcomingTrackColor,
                    start = Offset(centerX, nodeCenterY + 14.dp.toPx()),
                    end = Offset(centerX, size.height),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round,
                    pathEffect = if (bottomDone) null else dashEffect
                )
            }

            if (status == RoadmapNodeStatus.NEXT_MILESTONE || status == RoadmapNodeStatus.IN_PROGRESS) {
                drawCircle(
                    color = activeAccent.copy(alpha = 0.22f),
                    radius = 15.dp.toPx() * haloScale,
                    center = Offset(centerX, nodeCenterY)
                )
            }
        }

        // Node Circle Marker
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (status) {
                        RoadmapNodeStatus.COMPLETED -> completedColor
                        RoadmapNodeStatus.NEXT_MILESTONE, RoadmapNodeStatus.IN_PROGRESS -> activeAccent
                        RoadmapNodeStatus.UPCOMING -> MaterialTheme.colorScheme.surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (status == RoadmapNodeStatus.COMPLETED) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (status == RoadmapNodeStatus.UPCOMING) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        Color.White
                    }
                )
            }
        }
    }
}

private fun buildModuleNodes(
    materials: List<StudyMaterial>,
    progressMap: Map<String, MaterialProgress>,
    attempts: List<QuizAttempt>
): List<RoadmapModuleNode> {
    val byId = materials.associateBy { it.id }
    val banksById = OmMcqBanks.banks.associateBy { it.id }

    fun bestScoreFor(keywords: List<String>): Int? {
        return attempts
            .filter { attempt -> keywords.any { kw -> attempt.topic.contains(kw, ignoreCase = true) } }
            .maxOfOrNull { it.accuracyPercent }
    }

    data class RawNodeSpec(
        val id: String,
        val step: String,
        val title: String,
        val subtitle: String,
        val milestone: String,
        val icon: ImageVector,
        val materialId: String,
        val bankId: String,
        val quizKeywords: List<String>
    )

    val specs = listOf(
        RawNodeSpec(
            id = "mod_ops",
            step = "01",
            title = "Operations & Quality Foundations",
            subtitle = "OM Study Text Units 1–7: PESTEL, Mintzberg, Production, TPM/OEE, MRP & Six Sigma",
            milestone = "Complete OM Study Text core reading & score 70%+ on Units 1–7 MCQs",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            materialId = "om_study_text_2026",
            bankId = "om_bank_1_4",
            quizKeywords = listOf("Units 1", "Units 5", "Mintzberg", "TPM", "MRP", "Six Sigma")
        ),
        RawNodeSpec(
            id = "mod_hrm",
            step = "02",
            title = "Human Capital & Leadership",
            subtitle = "OM Units 8–9 & Reading File 2: Motivation Theories, Handy's Shamrock, Pfeffer & Kolb",
            milestone = "Finish HRM reading & master Pfeffer's 7 Practices and Kolb's Cycle",
            icon = Icons.Rounded.AutoAwesome,
            materialId = "drive_file_2",
            bankId = "om_bank_8_9",
            quizKeywords = listOf("Units 8", "HRM", "Pfeffer", "Kolb")
        ),
        RawNodeSpec(
            id = "mod_marketing",
            step = "03",
            title = "Marketing Strategy & Video Walkthroughs",
            subtitle = "OM Units 10–12 & Lecture Clips: 7Ps Mix, STP, BCG, Ansoff & Consumer Behavior",
            milestone = "Watch Drive Lecture Clips & pass Units 10–12 Marketing quiz",
            icon = Icons.Rounded.PlayCircle,
            materialId = "drive_file_3",
            bankId = "om_bank_10_12",
            quizKeywords = listOf("Units 10", "Marketing", "Ansoff", "7Ps")
        ),
        RawNodeSpec(
            id = "mod_epm",
            step = "04",
            title = "Enterprise Performance & Master Guide",
            subtitle = "OM Units 13–14 & Google Doc 6: CSFs/KPIs, Budgets, Balanced Scorecard & Building Block",
            milestone = "Review Master Doc 6 & compare BSC, Building Block, and Performance Pyramid",
            icon = Icons.Rounded.Route,
            materialId = "google_doc_6",
            bankId = "om_bank_13_14",
            quizKeywords = listOf("Units 13", "Balanced Scorecard", "EPM", "Building Block")
        ),
        RawNodeSpec(
            id = "mod_past_papers",
            step = "05",
            title = "Past Papers & Final Mock Checkpoint",
            subtitle = "OM ML-1 Descriptive Q1–Q10 Solutions + Timed 15-Question Mixed Mock Exam",
            milestone = "Complete Past Papers PDF & pass the 15-Question Mixed Mock Exam (75%+)",
            icon = Icons.Rounded.EmojiEvents,
            materialId = "om_past_papers_ml1",
            bankId = "om_mock_exam",
            quizKeywords = listOf("Mock Exam", "Past Papers", "Mixed")
        )
    )

    var assignedNextMilestone = false
    return specs.map { spec ->
        val mat = byId[spec.materialId]
        val bank = banksById[spec.bankId]
        val matDone = progressMap[spec.materialId]?.completed == true
        val bestQuiz = bestScoreFor(spec.quizKeywords)
        val quizPassed = (bestQuiz ?: 0) >= 70

        val progress = when {
            matDone && quizPassed -> 1f
            matDone -> 0.7f
            quizPassed -> 0.6f
            (bestQuiz ?: 0) > 0 -> 0.35f
            else -> 0f
        }

        val status = when {
            matDone && (quizPassed || spec.id != "mod_past_papers") -> RoadmapNodeStatus.COMPLETED
            progress > 0f && !assignedNextMilestone -> {
                assignedNextMilestone = true
                RoadmapNodeStatus.IN_PROGRESS
            }
            !assignedNextMilestone -> {
                assignedNextMilestone = true
                RoadmapNodeStatus.NEXT_MILESTONE
            }
            else -> RoadmapNodeStatus.UPCOMING
        }

        RoadmapModuleNode(
            id = spec.id,
            stepNumber = spec.step,
            title = spec.title,
            subtitle = spec.subtitle,
            milestoneLabel = spec.milestone,
            icon = spec.icon,
            status = status,
            progressFraction = progress,
            linkedMaterial = mat,
            linkedBank = bank,
            bestQuizScore = bestQuiz,
            materialDone = matDone
        )
    }
}
