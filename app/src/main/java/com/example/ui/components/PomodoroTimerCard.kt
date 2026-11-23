package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.PomodoroPhase
import com.example.data.PomodoroState
import kotlin.math.roundToInt

@Composable
fun PomodoroTimerCard(
    state: PomodoroState,
    onToggleRunPause: () -> Unit,
    onReset: () -> Unit,
    onSkipPhase: () -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onUpdateConfig: (Int, Int, Int, Int, Boolean) -> Unit,
    onOpenFullScreen: () -> Unit
) {
    var showConfigDialog by remember { mutableStateOf(false) }

    val accentColor = when (state.phase) {
        PomodoroPhase.WORK -> MaterialTheme.colorScheme.secondary
        PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
        PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pomodoro_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Column {
                        Text(
                            text = "Pomodoro Focus Timer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.workMinutes}m focus • ${state.shortBreakMinutes}m break • ${state.completedWorkSessions} cycles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pomodoro_config_button")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Configure Pomodoro intervals")
                    }
                    IconButton(
                        onClick = onOpenFullScreen,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pomodoro_fullscreen_button")
                    ) {
                        Icon(Icons.Rounded.Fullscreen, contentDescription = "Full-Screen Pomodoro Timer")
                    }
                }
            }

            // Phase Switcher Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroPhase.entries.forEach { phase ->
                    FilterChip(
                        selected = state.phase == phase,
                        onClick = { onSelectPhase(phase) },
                        label = {
                            Text(
                                text = when (phase) {
                                    PomodoroPhase.WORK -> "Focus ${state.workMinutes}m"
                                    PomodoroPhase.SHORT_BREAK -> "Break ${state.shortBreakMinutes}m"
                                    PomodoroPhase.LONG_BREAK -> "Long ${state.longBreakMinutes}m"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Countdown + Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PomodoroCountdownRing(
                    state = state,
                    accentColor = accentColor,
                    ringSize = 104.dp,
                    strokeWidth = 9.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = state.phase.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleRunPause,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pomodoro_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (state.isRunning) "Pause" else "Start")
                        }

                        OutlinedButton(
                            onClick = onReset,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pomodoro_reset_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset timer")
                        }

                        OutlinedButton(
                            onClick = onSkipPhase,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pomodoro_skip_button")
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next phase")
                        }
                    }
                }
            }
        }
    }

    if (showConfigDialog) {
        PomodoroConfigDialog(
            state = state,
            onDismiss = { showConfigDialog = false },
            onSave = { work, shortBrk, longBrk, cycles, auto ->
                onUpdateConfig(work, shortBrk, longBrk, cycles, auto)
                showConfigDialog = false
            }
        )
    }
}

@Composable
fun PomodoroCountdownRing(
    state: PomodoroState,
    accentColor: Color,
    ringSize: Dp,
    strokeWidth: Dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * state.progressFraction,
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.formattedTime,
                style = if (ringSize > 150.dp) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PomodoroConfigDialog(
    state: PomodoroState,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Int, Boolean) -> Unit
) {
    var workMins by remember { mutableFloatStateOf(state.workMinutes.toFloat()) }
    var shortBreakMins by remember { mutableFloatStateOf(state.shortBreakMinutes.toFloat()) }
    var longBreakMins by remember { mutableFloatStateOf(state.longBreakMinutes.toFloat()) }
    var cycles by remember { mutableFloatStateOf(state.cyclesBeforeLongBreak.toFloat()) }
    var autoAdvance by remember { mutableStateOf(state.autoAdvance) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Pomodoro Intervals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = workMins.roundToInt() == 25 && shortBreakMins.roundToInt() == 5,
                        onClick = {
                            workMins = 25f
                            shortBreakMins = 5f
                            longBreakMins = 15f
                        },
                        label = { Text("25 / 5") }
                    )
                    FilterChip(
                        selected = workMins.roundToInt() == 50 && shortBreakMins.roundToInt() == 10,
                        onClick = {
                            workMins = 50f
                            shortBreakMins = 10f
                            longBreakMins = 20f
                        },
                        label = { Text("50 / 10") }
                    )
                    FilterChip(
                        selected = workMins.roundToInt() == 15 && shortBreakMins.roundToInt() == 3,
                        onClick = {
                            workMins = 15f
                            shortBreakMins = 3f
                            longBreakMins = 10f
                        },
                        label = { Text("15 / 3") }
                    )
                }

                IntervalSlider(
                    label = "Focus Work",
                    valueText = "${workMins.roundToInt()} min",
                    value = workMins,
                    onValueChange = { workMins = it },
                    valueRange = 5f..90f
                )
                IntervalSlider(
                    label = "Short Break",
                    valueText = "${shortBreakMins.roundToInt()} min",
                    value = shortBreakMins,
                    onValueChange = { shortBreakMins = it },
                    valueRange = 1f..30f
                )
                IntervalSlider(
                    label = "Long Break (every ${cycles.roundToInt()} cycles)",
                    valueText = "${longBreakMins.roundToInt()} min",
                    value = longBreakMins,
                    onValueChange = { longBreakMins = it },
                    valueRange = 5f..45f
                )
                IntervalSlider(
                    label = "Cycles before Long Break",
                    valueText = "${cycles.roundToInt()}",
                    value = cycles,
                    onValueChange = { cycles = it },
                    valueRange = 2f..6f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-start next interval", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = autoAdvance,
                        onCheckedChange = { autoAdvance = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        workMins.roundToInt(),
                        shortBreakMins.roundToInt(),
                        longBreakMins.roundToInt(),
                        cycles.roundToInt(),
                        autoAdvance
                    )
                },
                modifier = Modifier.testTag("save_pomodoro_config_button")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun IntervalSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}
