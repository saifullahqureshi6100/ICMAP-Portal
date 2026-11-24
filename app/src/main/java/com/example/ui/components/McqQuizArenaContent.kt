package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.QuizSessionState

@Composable
fun McqQuizArenaContent(
    session: QuizSessionState,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onSelectOption: (Int, Int) -> Unit,
    onSelectQuestionIndex: (Int) -> Unit,
    onResetQuiz: () -> Unit,
    onSubmitQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questions = session.questions
    val currentIndex = session.currentIndex.coerceIn(0, (questions.lastIndex).coerceAtLeast(0))
    val currentQuestion = questions.getOrNull(currentIndex)
    val correctCount = session.correctCount
    val mins = session.elapsedSeconds / 60
    val secs = session.elapsedSeconds % 60

    Column(
        modifier = modifier
            .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
    ) {
        // Header with Full-Screen Toggle, Timer & Restart
        Surface(
            color = if (isFullScreen) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            },
            tonalElevation = if (isFullScreen) 3.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFullScreen) {
                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exit_fullscreen_mcq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FullscreenExit,
                            contentDescription = "Exit Full-Screen Quiz Mode"
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.topic,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isFullScreen) "FULL-SCREEN QUIZ ARENA" else "INTERACTIVE QUIZ • TAP TO EXPAND",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onResetQuiz,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mcq_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Restart quiz"
                    )
                }

                if (!isFullScreen) {
                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("start_fullscreen_mcq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = "Enter Full-Screen Quiz Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Question Navigator Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(questions) { index, q ->
                val answered = session.selectedAnswers.containsKey(index)
                val isCurrent = index == currentIndex
                val pillColor = when {
                    session.isSubmitted && session.selectedAnswers[index] == q.correctIndex -> Color(0xFF059669)
                    session.isSubmitted -> MaterialTheme.colorScheme.error
                    isCurrent -> MaterialTheme.colorScheme.primary
                    answered -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    session.isSubmitted || isCurrent -> Color.White
                    answered -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = CircleShape,
                    color = pillColor,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onSelectQuestionIndex(index) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor
                        )
                    }
                }
            }
        }

        if (currentQuestion != null) {
            val bodyModifier = if (isFullScreen) {
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            } else {
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            }

            Column(
                modifier = bodyModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (session.isSubmitted) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Exam Completed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Score: $correctCount / ${questions.size} (${session.accuracyPercent}%)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(onClick = onResetQuiz) {
                                Text("Retry")
                            }
                        }
                    }
                }

                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = currentQuestion.question,
                    style = if (isFullScreen) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                currentQuestion.options.forEachIndexed { optIdx, optionText ->
                    val isSelected = session.selectedAnswers[currentIndex] == optIdx
                    val isCorrectOption = optIdx == currentQuestion.correctIndex

                    val containerColor = when {
                        session.isSubmitted && isCorrectOption -> Color(0xFF059669).copy(alpha = 0.16f)
                        session.isSubmitted && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                        isSelected -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !session.isSubmitted) {
                                onSelectOption(currentIndex, optIdx)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = ('A' + optIdx).toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (session.isSubmitted) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "AI Tutor Explanation",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = currentQuestion.explanation,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Bottom Quiz Navigation & Full-Screen Toggle Footer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isFullScreen) 4.dp else 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { if (currentIndex > 0) onSelectQuestionIndex(currentIndex - 1) },
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Previous")
                }

                if (!session.isSubmitted && currentIndex == questions.lastIndex) {
                    Button(
                        onClick = onSubmitQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("submit_mcq_exam_button")
                    ) {
                        Text("Submit Exam", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentIndex < questions.lastIndex) {
                                onSelectQuestionIndex(currentIndex + 1)
                            } else if (isFullScreen) {
                                onToggleFullScreen()
                            } else {
                                onResetQuiz()
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Text(
                            when {
                                currentIndex < questions.lastIndex -> "Next Question"
                                isFullScreen -> "Exit Full-Screen"
                                else -> "Retake Quiz"
                            }
                        )
                    }
                }
            }
        }
    }
}
