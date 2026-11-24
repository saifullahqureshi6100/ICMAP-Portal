package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Flashcard
import com.example.data.PreloadedFlashcards
import com.example.data.PreloadedMaterials
import com.example.data.StudyMaterial
import com.example.ui.components.FlashcardShuffleFlipArena
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.StreamUiState
import kotlin.math.roundToInt

@Composable
fun FlashcardsScreen(
    flashcards: List<Flashcard>,
    selectedDeckFilter: String?,
    materialsState: StreamUiState<List<StudyMaterial>>,
    flashcardGenState: AiTaskState,
    onSelectDeckFilter: (String?) -> Unit,
    onRecordReview: (Flashcard, Boolean) -> Unit,
    onDeleteCard: (Flashcard) -> Unit,
    onGenerateFromMaterial: (StudyMaterial, String, Int, Boolean) -> Unit,
    onAddCustomCard: (String, String, String, String, String) -> Unit,
    onOpenFullScreen: () -> Unit
) {
    val materials = (materialsState as? StreamUiState.Success)?.data ?: PreloadedMaterials.items
    var selectedMaterialId by remember(materials) {
        mutableStateOf(
            materials.firstOrNull { it.id == "om_past_papers_ml1" }?.id
                ?: materials.firstOrNull()?.id
                ?: ""
        )
    }
    val selectedMaterial = remember(materials, selectedMaterialId) {
        materials.firstOrNull { it.id == selectedMaterialId } ?: materials.first()
    }

    var customExcerpt by remember { mutableStateOf("") }
    var cardCountSlider by remember { mutableFloatStateOf(6f) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    val visibleDeckCards = remember(flashcards, selectedDeckFilter) {
        if (selectedDeckFilter == null) flashcards
        else flashcards.filter { it.deckTitle == selectedDeckFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Digital Flashcards",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Generate active-recall flashcards from your reading materials and test your memory with shuffle & 3D flip.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Interactive Shuffle-and-Flip Deck Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_arena_card")
            ) {
                FlashcardShuffleFlipArena(
                    allCards = flashcards,
                    selectedDeckFilter = selectedDeckFilter,
                    onSelectDeckFilter = onSelectDeckFilter,
                    onRecordReview = onRecordReview,
                    isFullScreen = false,
                    onToggleFullScreen = onOpenFullScreen
                )
            }
        }

        // 2. AI Flashcard Generator from Reading Materials
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_generator_card")
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
                        Column {
                            Text(
                                text = "Generate Flashcards from Reading Material",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select a study module or paste notes to build a new deck",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Source Material",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(materials, key = { it.id }) { mat ->
                            FilterChip(
                                selected = mat.id == selectedMaterial.id,
                                onClick = { selectedMaterialId = mat.id },
                                label = {
                                    Text(
                                        text = mat.title.take(28) + if (mat.title.length > 28) "…" else "",
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("flashcard_source_${mat.id}")
                            )
                        }
                    }

                    if (selectedMaterial.description.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedMaterial.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customExcerpt,
                        onValueChange = { customExcerpt = it },
                        label = { Text("Focus Excerpt or Specific Chapter Notes (optional)") },
                        placeholder = { Text("Paste key paragraphs or leave blank to use full material context...") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flashcard_excerpt_input")
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deck Size", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "${cardCountSlider.roundToInt()} flashcards",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = cardCountSlider,
                            onValueChange = { cardCountSlider = it },
                            valueRange = 3f..12f,
                            steps = 8
                        )
                    }

                    AnimatedVisibility(visible = flashcardGenState is AiTaskState.Error) {
                        val msg = (flashcardGenState as? AiTaskState.Error)?.message.orEmpty()
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
                                onGenerateFromMaterial(
                                    selectedMaterial,
                                    customExcerpt,
                                    cardCountSlider.roundToInt(),
                                    false
                                )
                            },
                            enabled = flashcardGenState !is AiTaskState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("generate_flashcards_button")
                        ) {
                            Text("Generate & Save")
                        }

                        Button(
                            onClick = {
                                onGenerateFromMaterial(
                                    selectedMaterial,
                                    customExcerpt,
                                    cardCountSlider.roundToInt(),
                                    true
                                )
                            },
                            enabled = flashcardGenState !is AiTaskState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(50.dp)
                                .testTag("generate_flashcards_fullscreen_button")
                        ) {
                            if (flashcardGenState is AiTaskState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Creating...")
                            } else {
                                Icon(Icons.Rounded.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Generate & Flip")
                            }
                        }
                    }
                }
            }
        }

        // 3. Deck Card Browser Header
        item {
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
                    Icon(Icons.Rounded.Style, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Column {
                        Text(
                            text = selectedDeckFilter ?: "All Flashcards (${visibleDeckCards.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${visibleDeckCards.count { it.mastered }} mastered • ${visibleDeckCards.count { !it.mastered }} to review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showAddCardDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_flashcard_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Card")
                }
            }
        }

        items(visibleDeckCards, key = { it.id }) { card ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
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
                            if (card.mastered) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Mastered",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = card.deckTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = card.front,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = card.back,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (card.userId.isNotBlank()) {
                        IconButton(
                            onClick = { onDeleteCard(card) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete flashcard",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCardDialog) {
        AddCustomFlashcardDialog(
            defaultDeckTitle = selectedDeckFilter ?: selectedMaterial.title,
            defaultMaterialId = selectedMaterial.id,
            onSave = { deckTitle, matId, front, back, hint ->
                onAddCustomCard(deckTitle, matId, front, back, hint)
                showAddCardDialog = false
            },
            onDismiss = { showAddCardDialog = false }
        )
    }
}

@Composable
fun FullScreenFlashcardScreen(
    flashcards: List<Flashcard>,
    selectedDeckFilter: String?,
    onSelectDeckFilter: (String?) -> Unit,
    onRecordReview: (Flashcard, Boolean) -> Unit,
    onAddCustomCard: (String, String, String, String, String) -> Unit,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler { onExit() }

    var showAddCardDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exit_fullscreen_flashcards_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Exit full-screen flashcards"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Full-Screen Flashcards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedDeckFilter ?: "All Study Decks (${flashcards.size} cards)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddCardDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Card")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            FlashcardShuffleFlipArena(
                allCards = flashcards,
                selectedDeckFilter = selectedDeckFilter,
                onSelectDeckFilter = onSelectDeckFilter,
                onRecordReview = onRecordReview,
                isFullScreen = true,
                onToggleFullScreen = onExit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showAddCardDialog) {
        AddCustomFlashcardDialog(
            defaultDeckTitle = selectedDeckFilter ?: PreloadedFlashcards.STARTER_DECK_TITLE,
            defaultMaterialId = "",
            onSave = { deckTitle, matId, front, back, hint ->
                onAddCustomCard(deckTitle, matId, front, back, hint)
                showAddCardDialog = false
            },
            onDismiss = { showAddCardDialog = false }
        )
    }
}

@Composable
private fun AddCustomFlashcardDialog(
    defaultDeckTitle: String,
    defaultMaterialId: String,
    onSave: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var deckTitle by remember(defaultDeckTitle) { mutableStateOf(defaultDeckTitle) }
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Flashcard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = deckTitle,
                    onValueChange = { deckTitle = it },
                    label = { Text("Deck Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (Question / Term)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (Answer / Key Points)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Memory Hint (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        onSave(
                            deckTitle.trim().ifBlank { "Custom Flashcards" },
                            defaultMaterialId,
                            front.trim(),
                            back.trim(),
                            hint.trim()
                        )
                    }
                },
                enabled = front.isNotBlank() && back.isNotBlank()
            ) {
                Text("Save Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
