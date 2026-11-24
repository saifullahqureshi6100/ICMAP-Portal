package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Style
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Flashcard

@Composable
fun FlashcardShuffleFlipArena(
    allCards: List<Flashcard>,
    selectedDeckFilter: String?,
    onSelectDeckFilter: (String?) -> Unit,
    onRecordReview: (Flashcard, Boolean) -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deckTitles = remember(allCards) {
        allCards.map { it.deckTitle }.filter { it.isNotBlank() }.distinct()
    }

    var unmasteredOnly by remember { mutableStateOf(false) }

    val baseDeck = remember(allCards, selectedDeckFilter, unmasteredOnly) {
        allCards.filter { card ->
            val deckMatch = selectedDeckFilter == null || card.deckTitle == selectedDeckFilter
            val masteryMatch = !unmasteredOnly || !card.mastered
            deckMatch && masteryMatch
        }
    }

    var shuffledIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var shuffleCount by remember { mutableIntStateOf(0) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var roundCompleted by remember { mutableStateOf(false) }
    var roundGotItCount by remember { mutableIntStateOf(0) }
    var roundTestedCount by remember { mutableIntStateOf(0) }

    // Keep display order synced with baseDeck and current shuffle permutation
    val activeCards = remember(baseDeck, shuffledIds) {
        if (shuffledIds.isEmpty()) {
            baseDeck
        } else {
            val byId = baseDeck.associateBy { it.id }
            val ordered = shuffledIds.mapNotNull { byId[it] }
            val extras = baseDeck.filter { it.id !in shuffledIds.toSet() }
            ordered + extras
        }
    }

    // Reset index smoothly if filter changes
    LaunchedEffect(selectedDeckFilter, unmasteredOnly) {
        shuffledIds = emptyList()
        currentIndex = 0
        isFlipped = false
        showHint = false
        roundCompleted = false
        roundGotItCount = 0
        roundTestedCount = 0
    }

    if (currentIndex >= activeCards.size && activeCards.isNotEmpty()) {
        currentIndex = activeCards.lastIndex
    }

    fun triggerShuffle() {
        if (baseDeck.isEmpty()) return
        shuffledIds = baseDeck.map { it.id }.shuffled()
        shuffleCount += 1
        currentIndex = 0
        isFlipped = false
        showHint = false
        roundCompleted = false
        roundGotItCount = 0
        roundTestedCount = 0
    }

    fun goToCard(targetIdx: Int) {
        if (activeCards.isEmpty()) return
        currentIndex = targetIdx.coerceIn(0, activeCards.lastIndex)
        isFlipped = false
        showHint = false
    }

    fun handleGradeAndAdvance(card: Flashcard, mastered: Boolean) {
        onRecordReview(card, mastered)
        roundTestedCount += 1
        if (mastered) roundGotItCount += 1

        if (currentIndex < activeCards.lastIndex) {
            currentIndex += 1
            isFlipped = false
            showHint = false
        } else {
            roundCompleted = true
            isFlipped = false
            showHint = false
        }
    }

    val masteredInFiltered = remember(allCards, selectedDeckFilter) {
        val deckCards = if (selectedDeckFilter == null) allCards else allCards.filter { it.deckTitle == selectedDeckFilter }
        deckCards.count { it.mastered } to deckCards.size
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(if (isFullScreen) 20.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Shuffle + Full-Screen Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        text = if (isFullScreen) "Full-Screen Flashcard Test" else "Shuffle & Flip Memory Deck",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${masteredInFiltered.first}/${masteredInFiltered.second} mastered" +
                            if (shuffleCount > 0) " • Shuffled ×$shuffleCount" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { triggerShuffle() },
                    enabled = baseDeck.size > 1,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("shuffle_flashcards_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle deck",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle", style = MaterialTheme.typography.labelLarge)
                }

                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("flashcard_fullscreen_toggle")
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        contentDescription = if (isFullScreen) "Exit full-screen flashcards" else "Open flashcards in full-screen"
                    )
                }
            }
        }

        // Deck & Mastery Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedDeckFilter == null,
                    onClick = { onSelectDeckFilter(null) },
                    label = { Text("All (${allCards.size})") }
                )
            }
            item {
                FilterChip(
                    selected = unmasteredOnly,
                    onClick = { unmasteredOnly = !unmasteredOnly },
                    label = { Text("To Review") }
                )
            }
            items(deckTitles, key = { it }) { title ->
                val count = allCards.count { it.deckTitle == title }
                FilterChip(
                    selected = selectedDeckFilter == title,
                    onClick = { onSelectDeckFilter(if (selectedDeckFilter == title) null else title) },
                    label = {
                        Text(
                            text = "${title.take(26)}${if (title.length > 26) "…" else ""} ($count)",
                            maxLines = 1
                        )
                    }
                )
            }
        }

        if (activeCards.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (unmasteredOnly) {
                            "All cards in this deck are mastered!"
                        } else {
                            "No flashcards in this deck yet."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (unmasteredOnly) {
                        Button(onClick = { unmasteredOnly = false }) {
                            Text("Show All Cards")
                        }
                    }
                }
            }
        } else if (roundCompleted) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_round_summary")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Memory Round Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "You recalled $roundGotItCount of $roundTestedCount cards cleanly in this round.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                roundCompleted = false
                                currentIndex = 0
                                isFlipped = false
                                roundGotItCount = 0
                                roundTestedCount = 0
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Restart Order")
                        }
                        Button(
                            onClick = { triggerShuffle() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("flashcard_shuffle_again_button")
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle & Retry")
                        }
                    }
                }
            }
        } else {
            val activeCard = activeCards[currentIndex]

            // Progress strip
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card ${currentIndex + 1} of ${activeCards.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeCard.deckTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / activeCards.size.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Flippable & Swipeable 3D Card
            var totalDragX by remember(activeCard.id, shuffleCount) { mutableFloatStateOf(0f) }

            AnimatedContent(
                targetState = "${activeCard.id}_$shuffleCount",
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it / 4 } + fadeOut()
                    )
                },
                label = "flashcard_card_transition",
                modifier = if (isFullScreen) {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                } else {
                    Modifier.fillMaxWidth()
                }
            ) { _ ->
                FlipFlashcardSurface(
                    card = activeCard,
                    isFlipped = isFlipped,
                    showHint = showHint,
                    onToggleFlip = { isFlipped = !isFlipped },
                    onToggleHint = { showHint = !showHint },
                    cardMinHeightDp = if (isFullScreen) 340 else 250,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(activeCard.id, activeCards.size, currentIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = { totalDragX = 0f },
                                onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                                onDragEnd = {
                                    when {
                                        totalDragX < -90f && currentIndex < activeCards.lastIndex -> {
                                            goToCard(currentIndex + 1)
                                        }
                                        totalDragX > 90f && currentIndex > 0 -> {
                                            goToCard(currentIndex - 1)
                                        }
                                    }
                                    totalDragX = 0f
                                }
                            )
                        }
                )
            }

            // Flip, Prev/Next & Self-Grading Memory Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { goToCard(currentIndex - 1) },
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_flashcard_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous card"
                    )
                }

                OutlinedButton(
                    onClick = { handleGradeAndAdvance(activeCard, false) },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("flashcard_mark_review_button")
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Still Learning", maxLines = 1)
                }

                Button(
                    onClick = { handleGradeAndAdvance(activeCard, true) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("flashcard_mark_mastered_button")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Got It!", maxLines = 1)
                }

                IconButton(
                    onClick = { goToCard(currentIndex + 1) },
                    enabled = currentIndex < activeCards.lastIndex,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_flashcard_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next card"
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipFlashcardSurface(
    card: Flashcard,
    isFlipped: Boolean,
    showHint: Boolean,
    onToggleFlip: () -> Unit,
    onToggleHint: () -> Unit,
    cardMinHeightDp: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "flashcard_flip_rotation"
    )
    val showingBack = rotation > 90f

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (showingBack) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (showingBack) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = modifier
            .heightIn(min = cardMinHeightDp.dp, max = (cardMinHeightDp + 120).dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(onClick = onToggleFlip)
            .testTag("flashcard_flip_surface")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showingBack) {
                        Modifier.graphicsLayer { rotationY = 180f }
                    } else {
                        Modifier
                    }
                )
                .padding(20.dp)
        ) {
            if (!showingBack) {
                // FRONT FACE: Question / Term
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "PROMPT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (card.mastered) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Mastered",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Mastered",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.front,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (card.hint.isNotBlank()) {
                            if (showHint) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Hint: ${card.hint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = onToggleHint,
                                    modifier = Modifier.testTag("flashcard_hint_button")
                                ) {
                                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Show Hint", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tap card to reveal answer • Swipe left/right",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // BACK FACE: Answer & Explanation
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            Text(
                                text = "ANSWER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = if (card.reviewCount > 0L) "Tested ${card.reviewCount}×" else "New Card",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.back,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tap to flip back • Grade yourself below",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}
