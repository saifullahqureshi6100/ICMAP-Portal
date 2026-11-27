package com.example.ui.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.preferences.AiProviderType
import com.example.data.preferences.MotivationFrequency
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.StudyLanguageMode
import com.example.data.video.VideoCatalogAudit
import com.example.domain.AnalyticsSnapshot
import com.example.ui.utils.MaleTutorTts
import com.example.viewmodel.GlobalSearchResults
import com.example.work.StudyReminderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    analytics: AnalyticsSnapshot,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Study Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("All figures computed live from stored activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Weekly Consistency Chart (Real StudySessionEntity data)
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Weekly Study Consistency (${analytics.weeklyMinutes} min total)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val series = analytics.dailySeriesLast7Days
                    val maxMins = max(60, series.maxOfOrNull { it.second } ?: 60)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        val n = series.size.coerceAtLeast(1)
                        val stepX = size.width / n
                        val barWidth = stepX * 0.44f
                        val points = mutableListOf<Offset>()

                        series.forEachIndexed { idx, (_, mins) ->
                            val cx = stepX * idx + stepX / 2f
                            val ratio = mins.toFloat() / maxMins.toFloat()
                            val barH = size.height * ratio
                            drawLine(
                                color = primaryColor.copy(alpha = 0.25f),
                                start = Offset(cx, size.height),
                                end = Offset(cx, size.height - barH),
                                strokeWidth = barWidth,
                                cap = StrokeCap.Round
                            )
                            points += Offset(cx, size.height - barH)
                        }
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = secondaryColor,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }
                        points.forEach { pt ->
                            drawCircle(color = primaryColor, radius = 8f, center = pt)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        series.forEach { (day, mins) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${mins}m", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Performance Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("• Readiness Score: ${analytics.readiness.readinessScore}/100")
                    Text("• Topics Completed: ${analytics.topicsCompleted} • Remaining: ${analytics.topicsRemaining}")
                    Text("• Videos Completed: ${analytics.videosCompleted}/${analytics.availableVideosCount}")
                    Text("• Overall MCQ Accuracy: ${analytics.overallMcqAccuracy}%")
                    Text("• Descriptive Practice Average: ${analytics.descriptiveAveragePct}%")
                    Text("• Flashcard Mastery: ${analytics.flashcardMasteryPct}%")
                    Text("• Mock Exam Average: ${analytics.mockAveragePct?.let { "$it%" } ?: "No mock submitted yet"}")
                }
            }
        }

        item { SectionHeaderRow("Weakest 5 Topics", "Tap any topic to open study workspace") }
        items(analytics.weakest5, key = { "w_${it.topic.id}" }) { w ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().clickable { onOpenTopic(w.topic.id) }
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Unit ${w.topic.unit}: ${w.topic.name}", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${w.masteryPercent}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (analytics.strongest5.isNotEmpty()) {
            item { SectionHeaderRow("Strongest Topics") }
            items(analytics.strongest5, key = { "s_${it.topic.id}" }) { s ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTopic(s.topic.id) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Unit ${s.topic.unit}: ${s.topic.name}", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("${s.masteryPercent}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchScreen(
    onSearch: (String) -> GlobalSearchResults,
    onOpenTopic: (String) -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenPastPapers: () -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { onSearch(query) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search OM Syllabus, Notes, Videos, MCQs, Cards...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("global_search_input")
            )
        }

        if (query.isBlank()) {
            Text("Try searching: Mintzberg, OEE, Balanced Scorecard, MRP, Herzberg, BCG, ESG...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("${results.totalHits} matches found", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (results.topics.isNotEmpty()) {
                    item { Text("Topics (${results.topics.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(results.topics, key = { "st_${it.id}" }) { t ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenTopic(t.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Unit ${t.unit}: ${t.name}", fontWeight = FontWeight.Bold)
                                Text(t.romanUrduSummary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
                if (results.notesMatches.isNotEmpty()) {
                    item { Text("Personal Notes (${results.notesMatches.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(results.notesMatches, key = { "sn_${it.first.id}" }) { (t, p) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenTopic(t.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Note on ${t.name}", fontWeight = FontWeight.Bold)
                                Text(p.personalNotes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
                if (results.flashcards.isNotEmpty()) {
                    item { Text("Flashcards (${results.flashcards.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(results.flashcards.take(10), key = { "sf_${it.id}" }) { fc ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFlashcards)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(fc.front, fontWeight = FontWeight.SemiBold)
                                Text(fc.deckTitle, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (results.mcqs.isNotEmpty()) {
                    item { Text("MCQs (${results.mcqs.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(results.mcqs.take(10), key = { "sm_${it.mcq.id}" }) { mq ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenTopic(mq.topicId) }
                        ) {
                            Text(mq.mcq.question, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                if (results.pastPapers.isNotEmpty()) {
                    item { Text("Past Papers (${results.pastPapers.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(results.pastPapers, key = { "sp_${it.id}" }) { pp ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPastPapers)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(pp.sourceTag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(pp.typingQuestion.question, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsBackupScreen(
    settings: OmJarvisSettings,
    catalogAudit: VideoCatalogAudit?,
    videoCacheMb: String,
    getMaskedKey: (AiProviderType) -> String,
    onUpdateSettings: ((OmJarvisSettings) -> OmJarvisSettings) -> Unit,
    onSaveApiKey: (AiProviderType, String, String, String) -> Unit,
    onClearApiKey: (AiProviderType) -> Unit,
    onRefreshVideoCatalog: () -> Unit,
    onClearVideoCache: () -> Unit,
    onTriggerTestReminder: (StudyReminderType) -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: (String) -> Unit,
    onResetProgress: () -> Unit,
    onShowFeedback: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedProvider by remember(settings.aiProviderType) { mutableStateOf(settings.aiProviderType) }
    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember(settings.aiModelName) { mutableStateOf(settings.aiModelName) }
    var endpointInput by remember(settings.aiCustomEndpoint) { mutableStateOf(settings.aiCustomEndpoint) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                MaleTutorTts.configureMaleVoice(instance, preferUrdu = true, speechSpeed = settings.ttsSpeed, pitchScale = settings.ttsPitch)
            }
        }
        instance
    }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onUpdateSettings { it.copy(notificationsEnabled = granted) }
        onShowFeedback(if (granted) "Study reminders enabled!" else "Notification permission denied — in-app schedule still active.")
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val json = onExportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                onShowFeedback("Exported OM Jarvis JSON backup safely!")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                if (raw.isNotBlank()) onImportJson(raw)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text("OM Jarvis Settings & Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Core Study Preferences
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Study Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark Jarvis Theme")
                        Switch(checked = settings.darkThemeEnabled, onCheckedChange = { v -> onUpdateSettings { it.copy(darkThemeEnabled = v) } })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Certificate of Merit Target (80%+)")
                        Switch(checked = settings.meritModeEnabled, onCheckedChange = { v -> onUpdateSettings { it.copy(meritModeEnabled = v) } })
                    }
                    Text("Language Style", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudyLanguageMode.entries.forEach { m ->
                            FilterChip(selected = settings.languageMode == m, onClick = { onUpdateSettings { it.copy(languageMode = m) } }, label = { Text(m.label) })
                        }
                    }
                    Text("Motivation Level", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MotivationFrequency.entries.forEach { f ->
                            FilterChip(selected = settings.motivationFrequency == f, onClick = { onUpdateSettings { it.copy(motivationFrequency = f) } }, label = { Text(f.label) })
                        }
                    }
                    Text("Male Voice Speed: ${String.format("%.2f", settings.ttsSpeed)}x", style = MaterialTheme.typography.labelMedium)
                    Slider(value = settings.ttsSpeed, onValueChange = { sp -> onUpdateSettings { it.copy(ttsSpeed = sp) } }, valueRange = 0.7f..1.4f)
                    OutlinedButton(
                        onClick = {
                            MaleTutorTts.configureMaleVoice(tts, preferUrdu = true, speechSpeed = settings.ttsSpeed, pitchScale = settings.ttsPitch)
                            tts.speak("Assalam-o-Alaikum Saifullah! OM Jarvis male mentor voice calibrated for your Dec 7 Merit Exam.", TextToSpeech.QUEUE_FLUSH, null, "test_male_voice")
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Male Mentor Voice")
                    }
                }
            }
        }

        // WorkManager Reminders
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("WorkManager Daily Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateSettings { it.copy(notificationsEnabled = enable) }
                                }
                            }
                        )
                    }
                    Text(
                        text = "Morning ${settings.morningReminderTime} • Lunch ${settings.lunchReminderTime} • Cards ${settings.flashcardReminderTime} • Evening ${settings.eveningReminderTime} • Night ${settings.nightReminderTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = { onTriggerTestReminder(StudyReminderType.MORNING_BRIEF) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test WorkManager Deep-Link Notification")
                    }
                }
            }
        }

        // Encrypted AI Provider Vault
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Encrypted AI Provider Vault", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Keys are encrypted locally via Android Keystore AES-256-GCM. Never stored in Git or APK. App works fully offline even without a key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AiProviderType.entries.forEach { prov ->
                            FilterChip(
                                selected = selectedProvider == prov,
                                onClick = {
                                    selectedProvider = prov
                                    modelInput = prov.defaultModel
                                    if (prov == AiProviderType.OFFLINE_ONLY) {
                                        onUpdateSettings { it.copy(aiProviderType = prov, aiModelName = prov.defaultModel) }
                                    }
                                },
                                label = { Text(prov.label) }
                            )
                        }
                    }
                    if (selectedProvider != AiProviderType.OFFLINE_ONLY) {
                        val masked = getMaskedKey(selectedProvider)
                        if (masked.isNotBlank()) {
                            Text("Saved Key: $masked", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Enter ${selectedProvider.label} API Key") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { modelInput = it },
                            label = { Text("Model ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (selectedProvider == AiProviderType.CUSTOM_OPENAI) {
                            OutlinedTextField(
                                value = endpointInput,
                                onValueChange = { endpointInput = it },
                                label = { Text("HTTPS Chat Completions Endpoint") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (apiKeyInput.isNotBlank()) {
                                        onSaveApiKey(selectedProvider, apiKeyInput, modelInput, endpointInput)
                                        apiKeyInput = ""
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) { Text("Encrypt & Save") }
                            OutlinedButton(
                                onClick = { onClearApiKey(selectedProvider) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("Remove Key") }
                        }
                    }
                }
            }
        }

        // Offline Video Storage & JSON Backup/Reset
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Storage, Backup & Reset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Offline Video Cache: $videoCacheMb • Available: ${catalogAudit?.availableCount ?: 0}/${catalogAudit?.configuredCount ?: 28}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRefreshVideoCatalog, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Audit Videos")
                        }
                        OutlinedButton(onClick = onClearVideoCache, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text("Clear Cache")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { exportLauncher.launch("om_jarvis_backup.json") },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_backup_btn")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export JSON")
                        }
                        FilledTonalButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("import_backup_btn")
                        ) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore JSON")
                        }
                    }
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reset_progress_btn")
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset All Study Progress")
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All OM Jarvis Data?") },
            text = { Text("This clears topic mastery, test history, descriptive grades, and study sessions. Consider exporting a JSON backup first.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetProgress()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
