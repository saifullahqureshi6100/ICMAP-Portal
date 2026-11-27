package com.example.ui.jarvis

import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FlashcardEntity
import com.example.data.local.SpacedRepRating
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.StudyLanguageMode
import com.example.data.syllabus.SyllabusTopic
import com.example.domain.SmartSpacedRepetition
import com.example.ui.utils.MaleTutorTts

private enum class FlashcardFilter(val label: String) {
    SMART_QUEUE("Smart Queue"),
    DUE("Due Now"),
    WEAK_MISSED("Missed / Hard"),
    MASTERED("Mastered")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartFlashcardsScreen(
    flashcards: List<FlashcardEntity>,
    topics: List<SyllabusTopic>,
    weakTopicIds: Set<String>,
    settings: OmJarvisSettings,
    onReviewCard: (FlashcardEntity, SpacedRepRating) -> Unit,
    onAddCustomCard: (SyllabusTopic, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(FlashcardFilter.SMART_QUEUE) }
    var partFilter by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val deck = remember(flashcards, filter, partFilter, weakTopicIds) {
        val partFiltered = flashcards.filter { c -> partFilter == null || c.partCode == partFilter }
        val filtered = when (filter) {
            FlashcardFilter.SMART_QUEUE -> partFiltered
            FlashcardFilter.DUE -> partFiltered.filter { it.nextReviewAt <= now }
            FlashcardFilter.WEAK_MISSED -> partFiltered.filter { it.lapses > 0 || it.topicId in weakTopicIds || it.lastRating == SpacedRepRating.AGAIN.name }
            FlashcardFilter.MASTERED -> partFiltered.filter { it.mastered }
        }
        SmartSpacedRepetition.prioritizeDeck(filtered, weakTopicIds, now)
    }

    val safeIndex = if (deck.isEmpty()) 0 else currentIndex.coerceIn(0, deck.lastIndex)
    val activeCard = deck.getOrNull(safeIndex)

    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                MaleTutorTts.configureMaleVoice(
                    engine = instance,
                    preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                    speechSpeed = settings.ttsSpeed,
                    pitchScale = settings.ttsPitch
                )
            }
        }
        instance
    }

    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
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
                Column {
                    Text("SM-2 Smart Flashcards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "${flashcards.count { it.mastered }}/${flashcards.size} mastered • ${flashcards.count { it.nextReviewAt <= now }} due",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("add_custom_flashcard_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlashcardFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = {
                            filter = f
                            currentIndex = 0
                            isFlipped = false
                            showHint = false
                        },
                        label = { Text(f.label) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
                listOf(null to "All Parts", "PART_A" to "Part A", "PART_B" to "Part B", "PART_C" to "Part C", "PART_D" to "Part D").forEach { (code, lbl) ->
                    FilterChip(
                        selected = partFilter == code,
                        onClick = {
                            partFilter = code
                            currentIndex = 0
                            isFlipped = false
                        },
                        label = { Text(lbl) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        item {
            if (activeCard == null) {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No flashcards match this filter yet.", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { filter = FlashcardFilter.SMART_QUEUE; partFilter = null }) {
                            Text("Show Full Smart Queue")
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFlipped = !isFlipped }
                        .testTag("flashcard_flip_surface")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeCard.deckTitle} • Card ${safeIndex + 1}/${deck.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        val txt = if (isFlipped) activeCard.back else activeCard.front
                                        MaleTutorTts.configureMaleVoice(
                                            tts,
                                            preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                                            speechSpeed = settings.ttsSpeed,
                                            pitchScale = settings.ttsPitch
                                        )
                                        tts.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "fc_${activeCard.id}")
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak card")
                                }
                                IconButton(
                                    onClick = { isFlipped = !isFlipped },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Flip, contentDescription = "Flip flashcard")
                                }
                            }
                        }

                        Text(
                            text = if (isFlipped) "MODEL ANSWER" else "RECALL PROMPT (Tap card to flip)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = if (isFlipped) activeCard.back else activeCard.front,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (activeCard.hint.isNotBlank()) {
                            TextButton(
                                onClick = { showHint = !showHint },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (showHint) activeCard.hint else "Reveal Hint")
                            }
                        }

                        Text(
                            text = "SM-2 Stats: Reviews ${activeCard.reviewCount} • Interval ${activeCard.intervalDays}d • Lapses ${activeCard.lapses}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text("Rate Recall Difficulty (SM-2 Scheduler)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val advance = { rating: SpacedRepRating ->
                        onReviewCard(activeCard, rating)
                        isFlipped = false
                        showHint = false
                        currentIndex = if (safeIndex + 1 < deck.size) safeIndex + 1 else 0
                    }
                    OutlinedButton(
                        onClick = { advance(SpacedRepRating.AGAIN) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sm2_again_btn")
                    ) { Text("Again\n10m", style = MaterialTheme.typography.labelSmall) }

                    OutlinedButton(
                        onClick = { advance(SpacedRepRating.HARD) },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sm2_hard_btn")
                    ) { Text("Hard\n1d", style = MaterialTheme.typography.labelSmall) }

                    FilledTonalButton(
                        onClick = { advance(SpacedRepRating.GOOD) },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sm2_good_btn")
                    ) { Text("Good\n3d+", style = MaterialTheme.typography.labelSmall) }

                    Button(
                        onClick = { advance(SpacedRepRating.EASY) },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sm2_easy_btn")
                    ) { Text("Easy\n6d+", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }

    if (showCreateDialog && topics.isNotEmpty()) {
        var selectedTopic by remember { mutableStateOf(topics.first()) }
        var front by remember { mutableStateOf("") }
        var back by remember { mutableStateOf("") }
        var hint by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create SM-2 Flashcard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().clickable {
                            val idx = (topics.indexOf(selectedTopic) + 1) % topics.size
                            selectedTopic = topics[idx]
                        }
                    ) {
                        Text(
                            text = "Topic (tap to cycle): Unit ${selectedTopic.unit} — ${selectedTopic.name}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(value = front, onValueChange = { front = it }, label = { Text("Question / Front") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = back, onValueChange = { back = it }, label = { Text("Answer / Back") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hint, onValueChange = { hint = it }, label = { Text("Hint (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (front.isNotBlank() && back.isNotBlank()) {
                            onAddCustomCard(selectedTopic, front, back, hint)
                            showCreateDialog = false
                        }
                    }
                ) { Text("Save Card") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
