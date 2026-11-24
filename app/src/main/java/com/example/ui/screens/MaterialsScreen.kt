package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DriveUrlParser
import com.example.data.MaterialProgress
import com.example.data.MaterialType
import com.example.data.StudyMaterial
import com.example.viewmodel.StreamUiState

private enum class MaterialFilter(val label: String) {
    ALL("All"),
    OM_BOOKS("OM Books & Papers"),
    FOLDERS("Drive Folders"),
    READING("Reading & Docs"),
    VIDEOS("Video Clips")
}

@Composable
fun MaterialsScreen(
    materialsState: StreamUiState<List<StudyMaterial>>,
    progressMapState: StreamUiState<Map<String, MaterialProgress>>,
    onOpenFullScreen: (StudyMaterial, Boolean) -> Unit,
    onAskTutor: (StudyMaterial) -> Unit,
    onGenerateFlashcards: (StudyMaterial) -> Unit,
    onToggleComplete: (String, Boolean, Long) -> Unit,
    onAddCustomMaterial: (String, String, MaterialType, String, String, String, Long) -> Unit,
    onDeleteCustomMaterial: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(MaterialFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val progressMap = (progressMapState as? StreamUiState.Success)?.data ?: emptyMap()

    Box(modifier = Modifier.fillMaxSize()) {
        when (materialsState) {
            is StreamUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is StreamUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = materialsState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is StreamUiState.Success -> {
                val filtered = remember(materialsState.data, selectedFilter) {
                    when (selectedFilter) {
                        MaterialFilter.ALL -> materialsState.data
                        MaterialFilter.OM_BOOKS -> materialsState.data.filter { it.isBundledPdf }
                        MaterialFilter.FOLDERS -> materialsState.data.filter {
                            it.resolvedType() == MaterialType.DRIVE_FOLDER
                        }
                        MaterialFilter.READING -> materialsState.data.filter {
                            it.resolvedType() in listOf(
                                MaterialType.READING,
                                MaterialType.GOOGLE_DOC,
                                MaterialType.NOTES
                            )
                        }
                        MaterialFilter.VIDEOS -> materialsState.data.filter {
                            it.resolvedType() == MaterialType.VIDEO_CLIP ||
                                it.id == "drive_file_2" ||
                                it.id == "drive_folder_4"
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "OM Books, Past Papers & Drive Vault",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Toggle Full-Screen Mode on any reading PDF, Google Doc, Drive folder, or video clip for distraction-free focus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MaterialFilter.entries) { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter.label) }
                                )
                            }
                        }
                    }

                    items(filtered, key = { it.id }) { material ->
                        val isCompleted = progressMap[material.id]?.completed == true
                        MaterialCard(
                            material = material,
                            isCompleted = isCompleted,
                            onOpenFullScreen = { asVideo -> onOpenFullScreen(material, asVideo) },
                            onAskTutor = { onAskTutor(material) },
                            onGenerateFlashcards = { onGenerateFlashcards(material) },
                            onToggleComplete = {
                                onToggleComplete(material.id, !isCompleted, if (!isCompleted) material.estimatedMinutes else 0L)
                            },
                            onOpenExternal = {
                                if (material.sourceUrl.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.sourceUrl))
                                    runCatching { context.startActivity(intent) }
                                }
                            },
                            onDelete = if (!material.preloaded) {
                                { onDeleteCustomMaterial(material.id) }
                            } else null
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_material_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add study material")
        }
    }

    if (showAddDialog) {
        AddMaterialDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, type, url, notes, category, mins ->
                onAddCustomMaterial(title, desc, type, url, notes, category, mins)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MaterialCard(
    material: StudyMaterial,
    isCompleted: Boolean,
    onOpenFullScreen: (Boolean) -> Unit,
    onAskTutor: () -> Unit,
    onGenerateFlashcards: () -> Unit,
    onToggleComplete: () -> Unit,
    onOpenExternal: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val type = material.resolvedType()
    val typeIcon: ImageVector = when {
        material.isBundledPdf -> Icons.Rounded.PictureAsPdf
        type == MaterialType.DRIVE_FOLDER -> Icons.Rounded.Folder
        type == MaterialType.VIDEO_CLIP -> Icons.Rounded.PlayCircle
        type == MaterialType.GOOGLE_DOC -> Icons.Rounded.Description
        else -> Icons.AutoMirrored.Rounded.MenuBook
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (type == MaterialType.VIDEO_CLIP) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = type.label,
                            tint = if (type == MaterialType.VIDEO_CLIP) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (material.isBundledPdf) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = if (material.isBundledPdf) "OFFLINE PDF" else type.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (material.isBundledPdf) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${material.estimatedMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = material.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isCompleted) "Completed" else "Mark completed",
                        tint = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (material.description.isNotBlank()) {
                Text(
                    text = material.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Primary Full-Screen Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onOpenFullScreen(type == MaterialType.VIDEO_CLIP) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("fullscreen_material_${material.id}")
                ) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            material.isBundledPdf -> "Open PDF Full-Screen"
                            type == MaterialType.VIDEO_CLIP -> "Full-Screen Clip"
                            else -> "Full-Screen Read"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (type != MaterialType.VIDEO_CLIP && material.sourceUrl.contains("/file/")) {
                    OutlinedButton(
                        onClick = { onOpenFullScreen(true) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = "Open as Video Clip", modifier = Modifier.size(18.dp))
                    }
                }

                OutlinedButton(
                    onClick = onAskTutor,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "Ask AI Tutor", modifier = Modifier.size(18.dp))
                }

                OutlinedButton(
                    onClick = onGenerateFlashcards,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("make_flashcards_${material.id}")
                ) {
                    Icon(Icons.Rounded.Style, contentDescription = "Generate flashcards from ${material.title}", modifier = Modifier.size(18.dp))
                }

                if (material.sourceUrl.isNotBlank()) {
                    IconButton(
                        onClick = onOpenExternal,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open external Drive link"
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete custom material")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, MaterialType, String, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MaterialType.READING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Study Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (it.isNotBlank()) {
                            selectedType = DriveUrlParser.inferTypeFromUrl(it)
                        }
                    },
                    label = { Text("Google Drive / Video / Doc URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MaterialType.entries) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Summary") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reading Notes (optional)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title.trim(),
                            description.trim(),
                            selectedType,
                            url.trim(),
                            notes.trim(),
                            "Custom",
                            20L
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
