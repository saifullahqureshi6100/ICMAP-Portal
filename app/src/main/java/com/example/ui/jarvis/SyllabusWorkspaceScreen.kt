package com.example.ui.jarvis

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PdfChapterJump
import com.example.data.local.TopicProgressEntity
import com.example.data.local.VideoProgressEntity
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.StudyLanguageMode
import com.example.data.syllabus.SyllabusDatabaseBundle
import com.example.data.syllabus.SyllabusTopic
import com.example.data.video.VideoCatalogAudit
import com.example.ui.components.Media3VideoPlayerCard
import com.example.ui.components.PdfBookViewer
import com.example.ui.utils.MaleTutorTts
import kotlin.math.roundToInt

enum class WorkspaceMode(val label: String) {
    READ("READ"),
    WATCH("WATCH"),
    PRACTICE("PRACTICE")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyllabusWorkspaceScreen(
    bundle: SyllabusDatabaseBundle,
    settings: OmJarvisSettings,
    isOnline: Boolean,
    selectedPartFilter: String?,
    selectedTopicId: String,
    topicProgressMap: Map<String, TopicProgressEntity>,
    videos: List<VideoProgressEntity>,
    catalogAudit: VideoCatalogAudit?,
    isAuditingCatalog: Boolean,
    initialMode: WorkspaceMode = WorkspaceMode.READ,
    initialWorkspaceOpen: Boolean = false,
    onSelectPartFilter: (String?) -> Unit,
    onSelectTopic: (String) -> Unit,
    onMoveRelativeTopic: (Int) -> Unit,
    onRecordReading: (SyllabusTopic, Int, Int, Boolean?) -> Unit,
    onSaveNotesAndFlags: (SyllabusTopic, String?, String?, Boolean?, Boolean?, Boolean?) -> Unit,
    onVideoTick: (String, Long, Long, Boolean?) -> Unit,
    onStartDownload: (VideoProgressEntity) -> Unit,
    onPauseDownload: (VideoProgressEntity) -> Unit,
    onCancelDownload: (VideoProgressEntity) -> Unit,
    onDeleteCachedVideo: (VideoProgressEntity) -> Unit,
    onRefreshVideoCatalog: () -> Unit,
    onStartTopicMcq: (SyllabusTopic) -> Unit,
    onAskJarvisForTopic: (SyllabusTopic) -> Unit,
    onUpdateSettings: ((OmJarvisSettings) -> OmJarvisSettings) -> Unit
) {
    var workspaceOpen by remember { mutableStateOf(initialWorkspaceOpen) }
    var activeMode by remember(initialMode) { mutableStateOf(initialMode) }
    var showBookPdfDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialWorkspaceOpen) {
        workspaceOpen = initialWorkspaceOpen
    }

    val activeTopic = bundle.topics.firstOrNull { it.id == selectedTopicId } ?: bundle.topics.firstOrNull()
    val videoByTopic = remember(videos) { videos.associateBy { it.topicId } }

    if (workspaceOpen && activeTopic != null) {
        TopicStudyWorkspaceView(
            topic = activeTopic,
            progress = topicProgressMap[activeTopic.id],
            video = videoByTopic[activeTopic.id],
            settings = settings,
            isOnline = isOnline,
            activeMode = activeMode,
            onSwitchMode = { activeMode = it },
            onBackToCatalog = { workspaceOpen = false },
            onMoveRelativeTopic = onMoveRelativeTopic,
            onRecordReading = onRecordReading,
            onSaveNotesAndFlags = onSaveNotesAndFlags,
            onVideoTick = onVideoTick,
            onStartDownload = onStartDownload,
            onPauseDownload = onPauseDownload,
            onCancelDownload = onCancelDownload,
            onDeleteCachedVideo = onDeleteCachedVideo,
            onRefreshVideoCatalog = onRefreshVideoCatalog,
            onStartTopicMcq = { onStartTopicMcq(activeTopic) },
            onAskJarvis = { onAskJarvisForTopic(activeTopic) },
            onOpenPdfBook = { showBookPdfDialog = true },
            onUpdateSettings = onUpdateSettings
        )
    } else {
        val filteredTopics = bundle.topics.filter { t ->
            selectedPartFilter == null || t.partCode == selectedPartFilter
        }
        val threshold = if (settings.meritModeEnabled) 80 else 70

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master OM Syllabus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "${bundle.topics.size} Topics • Part A 35% • B 20% • C 25% • D 20%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showBookPdfDialog = true },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_m3_book_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("254p Book")
                    }
                }
            }

            // Video Catalog Status Banner + Refresh Button
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Video Catalog Audit",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (catalogAudit != null) {
                                    "${catalogAudit.availableCount}/${catalogAudit.configuredCount} available • ${catalogAudit.downloadedCount} offline (${catalogAudit.missingOrUnavailable.size} unavailable)"
                                } else {
                                    "${videos.count { it.catalogAvailable }}/${videos.size} playable"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onRefreshVideoCatalog,
                            enabled = !isAuditingCatalog,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("refresh_video_catalog_btn")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isAuditingCatalog) "Checking..." else "Refresh")
                        }
                    }
                }
            }

            // Part Filters
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedPartFilter == null,
                        onClick = { onSelectPartFilter(null) },
                        label = { Text("All (${bundle.topics.size})") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                    bundle.parts.forEach { p ->
                        FilterChip(
                            selected = selectedPartFilter == p.code,
                            onClick = { onSelectPartFilter(p.code) },
                            label = { Text("${p.code.replace('_', ' ')} (${p.weightPercent}%)") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            items(filteredTopics, key = { it.id }) { topic ->
                val prog = topicProgressMap[topic.id]
                val vid = videoByTopic[topic.id]
                val status = prog?.computeMasterStatus(threshold)
                    ?: com.example.data.local.TopicMasterStatus.NOT_STARTED
                val pct = ((prog?.masteryProgressFraction(threshold) ?: 0f) * 100f).roundToInt()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectTopic(topic.id)
                            workspaceOpen = true
                        }
                        .testTag("syllabus_topic_card_${topic.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MasteryRing(percent = pct, size = 48.dp, strokeWidth = 5.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Unit ${topic.unit} • ${topic.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${topic.partTitle} (${topic.partWeight}%) • PDF p.${topic.pdfPage} • ${topic.importance}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TopicStatusBadge(status)
                                val vidLabel = when {
                                    vid == null -> "No Video"
                                    vid.hasLocalFile -> "Offline MP4"
                                    vid.catalogAvailable -> "Online MP4"
                                    else -> "⚠️ Video unavailable"
                                }
                                Text(vidLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBookPdfDialog) {
        val chapters = remember(bundle.topics) {
            bundle.topics.map { PdfChapterJump("Unit ${it.unit}: ${it.name}", it.pdfPage) }
        }
        Dialog(
            onDismissRequest = { showBookPdfDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showBookPdfDialog = false }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close PDF")
                            }
                            Text("M3 Organizational Management (254p)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    PdfBookViewer(
                        assetPath = "M3_Organizational_Management.pdf",
                        chapters = chapters,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicStudyWorkspaceView(
    topic: SyllabusTopic,
    progress: TopicProgressEntity?,
    video: VideoProgressEntity?,
    settings: OmJarvisSettings,
    isOnline: Boolean,
    activeMode: WorkspaceMode,
    onSwitchMode: (WorkspaceMode) -> Unit,
    onBackToCatalog: () -> Unit,
    onMoveRelativeTopic: (Int) -> Unit,
    onRecordReading: (SyllabusTopic, Int, Int, Boolean?) -> Unit,
    onSaveNotesAndFlags: (SyllabusTopic, String?, String?, Boolean?, Boolean?, Boolean?) -> Unit,
    onVideoTick: (String, Long, Long, Boolean?) -> Unit,
    onStartDownload: (VideoProgressEntity) -> Unit,
    onPauseDownload: (VideoProgressEntity) -> Unit,
    onCancelDownload: (VideoProgressEntity) -> Unit,
    onDeleteCachedVideo: (VideoProgressEntity) -> Unit,
    onRefreshVideoCatalog: () -> Unit,
    onStartTopicMcq: () -> Unit,
    onAskJarvis: () -> Unit,
    onOpenPdfBook: () -> Unit,
    onUpdateSettings: ((OmJarvisSettings) -> OmJarvisSettings) -> Unit
) {
    val context = LocalContext.current
    var noteDraft by remember(topic.id, progress?.personalNotes) {
        mutableStateOf(progress?.personalNotes.orEmpty())
    }
    var isSpeaking by remember(topic.id) { mutableStateOf(false) }

    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                MaleTutorTts.configureMaleVoice(
                    engine = instance,
                    preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                    speechSpeed = settings.ttsSpeed,
                    pitchScale = settings.ttsPitch
                ) {
                    isSpeaking = false
                }
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

    val readContentBlock: @Composable (Modifier) -> Unit = { mod ->
        Column(
            modifier = mod
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Controls bar: Male TTS, Language switcher, Font size, Mark Complete
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (isSpeaking) {
                                    tts.stop()
                                    isSpeaking = false
                                } else {
                                    val spoken = when (settings.languageMode) {
                                        StudyLanguageMode.ENGLISH -> "${topic.name}. ${topic.description}. Formula: ${topic.formulae}. Key points: ${topic.keyPoints.joinToString(". ")}"
                                        StudyLanguageMode.ROMAN_URDU -> "${topic.name}. ${topic.romanUrduSummary}. Example: ${topic.pakistaniExample}"
                                        StudyLanguageMode.BILINGUAL -> "${topic.name}. ${topic.description}. ${topic.romanUrduSummary}. Formula: ${topic.formulae}"
                                    }
                                    MaleTutorTts.configureMaleVoice(
                                        engine = tts,
                                        preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                                        speechSpeed = settings.ttsSpeed,
                                        pitchScale = settings.ttsPitch
                                    ) { isSpeaking = false }
                                    tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "topic_${topic.id}")
                                    isSpeaking = true
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("reader_tts_btn")
                        ) {
                            Icon(if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSpeaking) "Stop Voice" else "Male Jarvis Read")
                        }

                        Button(
                            onClick = { onRecordReading(topic, 100, topic.keyPoints.size, true) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("reader_mark_complete_btn")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (progress?.readingCompleted == true) "Read Complete ✓" else "Mark Read Complete")
                        }

                        OutlinedButton(
                            onClick = onOpenPdfBook,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Book p.${topic.pdfPage}")
                        }
                    }

                    // Language mode quick toggle + Font size slider
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudyLanguageMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.languageMode == mode,
                                onClick = { onUpdateSettings { it.copy(languageMode = mode) } },
                                label = { Text(mode.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Text ${settings.readerFontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = settings.readerFontSizeSp,
                            onValueChange = { sz -> onUpdateSettings { it.copy(readerFontSizeSp = sz) } },
                            valueRange = 13f..22f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Reading Body
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (settings.languageMode != StudyLanguageMode.ROMAN_URDU) {
                        Text("Core Exam Explanation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            text = topic.description,
                            fontSize = settings.readerFontSizeSp.sp,
                            lineHeight = (settings.readerFontSizeSp * settings.readerLineSpacing).sp
                        )
                    }
                    if (settings.languageMode != StudyLanguageMode.ENGLISH) {
                        Text("Roman Urdu Mentor Breakdown", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text(
                            text = topic.romanUrduSummary,
                            fontSize = settings.readerFontSizeSp.sp,
                            lineHeight = (settings.readerFontSizeSp * settings.readerLineSpacing).sp
                        )
                    }
                    if (topic.formulae.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Formula / Anchor: ${topic.formulae}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Text("High-Yield Exam Bullet Points", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    topic.keyPoints.forEachIndexed { i, point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val pct = (((i + 1) * 100f) / topic.keyPoints.size.coerceAtLeast(1)).roundToInt()
                                    onRecordReading(topic, pct, i + 1, null)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${i + 1}. $point",
                                fontSize = settings.readerFontSizeSp.sp,
                                lineHeight = (settings.readerFontSizeSp * settings.readerLineSpacing).sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onSaveNotesAndFlags(topic, null, point, null, null, null) }) {
                                Text("Highlight", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pakistani Example: ${topic.pakistaniExample}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Personal Notes & Flags
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Personal Study Notes & Flags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { onSaveNotesAndFlags(topic, null, null, !(progress?.bookmarked ?: false), null, null) }) {
                                Icon(
                                    if (progress?.bookmarked == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = "Bookmark topic"
                                )
                            }
                            IconButton(onClick = { onSaveNotesAndFlags(topic, null, null, null, !(progress?.flaggedImportant ?: false), null) }) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = "Important",
                                    tint = if (progress?.flaggedImportant == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onSaveNotesAndFlags(topic, null, null, null, null, !(progress?.flaggedRevision ?: false)) }) {
                                Icon(
                                    Icons.Rounded.Flag,
                                    contentDescription = "Flag for revision",
                                    tint = if (progress?.flaggedRevision == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        label = { Text("Write your exam points, mnemonics, or doubts") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSaveNotesAndFlags(topic, noteDraft, null, null, null, null) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("save_topic_note_btn")
                    ) {
                        Text("Save Note Offline")
                    }
                    if (!progress?.highlightedSnippets.isNullOrBlank()) {
                        Text("Highlighted Snippets:\n• ${progress?.highlightedSnippets}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Bottom Action Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onStartTopicMcq,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("workspace_topic_mcq_btn")
                ) {
                    Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Practice MCQs")
                }
                Button(
                    onClick = onAskJarvis,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("workspace_ask_jarvis_btn")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ask Jarvis")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Workspace Header
        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBackToCatalog, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Syllabus")
                        }
                        Column {
                            Text("Unit ${topic.unit}: ${topic.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${topic.partTitle} (${topic.partWeight}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TopicStatusBadge(
                        progress?.computeMasterStatus(if (settings.meritModeEnabled) 80 else 70)
                            ?: com.example.data.local.TopicMasterStatus.NOT_STARTED
                    )
                }

                TabRow(selectedTabIndex = activeMode.ordinal) {
                    WorkspaceMode.entries.forEach { mode ->
                        Tab(
                            selected = activeMode == mode,
                            onClick = { onSwitchMode(mode) },
                            text = { Text(mode.label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        // Responsive Split-Screen for wide devices, clean vertical workspace for phones
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideSplit = maxWidth >= 760.dp
            if (isWideSplit) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Media3VideoPlayerCard(
                            video = video,
                            isOnline = isOnline,
                            onProgressTick = { pos, dur -> video?.let { onVideoTick(it.videoId, pos, dur, null) } },
                            onMarkComplete = { video?.let { onVideoTick(it.videoId, it.durationMs, it.durationMs, true) } },
                            onStartOrResumeDownload = onStartDownload,
                            onPauseDownload = onPauseDownload,
                            onCancelDownload = onCancelDownload,
                            onDeleteCached = onDeleteCachedVideo,
                            onRefreshCatalog = onRefreshVideoCatalog,
                            onPreviousTopic = { onMoveRelativeTopic(-1) },
                            onNextTopic = { onMoveRelativeTopic(1) },
                            onAddQuickNote = { txt ->
                                val combined = listOf(progress?.personalNotes.orEmpty(), txt).filter { it.isNotBlank() }.joinToString("\n")
                                onSaveNotesAndFlags(topic, combined, null, null, null, null)
                            },
                            onAskJarvisAboutVideo = onAskJarvis
                        )
                    }
                    readContentBlock(Modifier.weight(1f))
                }
            } else {
                when (activeMode) {
                    WorkspaceMode.WATCH -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Media3VideoPlayerCard(
                                video = video,
                                isOnline = isOnline,
                                onProgressTick = { pos, dur -> video?.let { onVideoTick(it.videoId, pos, dur, null) } },
                                onMarkComplete = { video?.let { onVideoTick(it.videoId, it.durationMs, it.durationMs, true) } },
                                onStartOrResumeDownload = onStartDownload,
                                onPauseDownload = onPauseDownload,
                                onCancelDownload = onCancelDownload,
                                onDeleteCached = onDeleteCachedVideo,
                                onRefreshCatalog = onRefreshVideoCatalog,
                                onPreviousTopic = { onMoveRelativeTopic(-1) },
                                onNextTopic = { onMoveRelativeTopic(1) },
                                onAddQuickNote = { txt ->
                                    val combined = listOf(progress?.personalNotes.orEmpty(), txt).filter { it.isNotBlank() }.joinToString("\n")
                                    onSaveNotesAndFlags(topic, combined, null, null, null, null)
                                },
                                onAskJarvisAboutVideo = onAskJarvis
                            )
                        }
                    }
                    WorkspaceMode.READ -> readContentBlock(Modifier.fillMaxSize())
                    WorkspaceMode.PRACTICE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Topic Mastery Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("• Reading Complete: ${if (progress?.readingCompleted == true) "Yes ✓" else "Pending"}")
                            Text("• Video Complete: ${if (progress?.videoCompleted == true) "Yes ✓" else "Pending"}")
                            Text("• Best MCQ Accuracy: ${progress?.bestMcqAccuracy ?: 0}% (Target ≥ ${if (settings.meritModeEnabled) 80 else 70}%)")
                            Text("• Flashcards Reviewed: ${progress?.flashcardsReviewed ?: 0}")
                            Text("• Descriptive Practiced: ${progress?.descriptivePracticed ?: 0}")
                            Button(onClick = onStartTopicMcq, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Launch Adaptive Topic MCQ Drill")
                            }
                            OutlinedButton(onClick = onAskJarvis, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Viva & 5-Mark Exam Coaching with Jarvis")
                            }
                        }
                    }
                }
            }
        }
    }
}
