package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FreeReferenceResult
import com.example.data.TutorEngineMode
import com.example.data.TutorMessage
import com.example.viewmodel.AiTaskState
import java.util.Locale
import kotlin.math.max

private val TutorQuickPrompts = listOf(
    "Explain Simply" to "Explain this concept step-by-step in simple terms with a real-world analogy: ",
    "OM 5-Mark Answer" to "Write an ICMA M3 Organizational Management 5-mark exam answer with key bullet points for: ",
    "Worked Example" to "Walk me through a detailed worked example step-by-step for: ",
    "5 Flashcards" to "Create 5 crisp Q&A flashcards for active recall on: ",
    "Quiz Me (Socratic)" to "Ask me one challenging conceptual question first and wait for my answer before revealing the solution on: ",
    "Summarize & Key Formulas" to "Summarize the core principles, definitions, and formulas I must remember for: ",
    "Past Paper Drill" to "Give me a 5-mark ICMA OM ML-1 past-paper style question on Marketing, HRM, BSC, Mintzberg, Quality Circles, or MRP and grade my answer."
)

@Composable
fun TutorScreen(
    messages: List<TutorMessage>,
    streamingText: String?,
    tutorState: AiTaskState,
    engineMode: TutorEngineMode = TutorEngineMode.GEMINI_BUILTIN,
    freeRefState: AiTaskState = AiTaskState.Idle,
    freeRefResult: FreeReferenceResult? = null,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onSelectEngine: (TutorEngineMode) -> Unit = {},
    onLookupWiki: (String) -> Unit = {},
    onLookupDictionary: (String) -> Unit = {},
    onDismissReference: () -> Unit = {},
    onAskTutorAboutReference: (FreeReferenceResult) -> Unit = {},
    onSendMessage: (String, Bitmap?) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFreeLookupBar by remember { mutableStateOf(false) }
    var lookupQuery by remember { mutableStateOf("") }
    var autoSpeakReplies by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var lastSpokenMsgId by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val tts = remember(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                ttsReady = true
            }
        }
        engine
    }

    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val speakExplanation: (String) -> Unit = { rawText ->
        if (ttsReady) {
            val clean = rawText
                .replace(Regex("[#*`]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(3500)
            if (clean.isNotEmpty()) {
                tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tutor_utterance")
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotEmpty()) {
                onSendMessage(spoken, attachedBitmap)
                attachedBitmap = null
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedBitmap = loadScaledBitmap(context, uri, 1024)
        }
    }

    LaunchedEffect(messages.size, streamingText) {
        val total = messages.size + if (streamingText != null) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    LaunchedEffect(messages.size, autoSpeakReplies, ttsReady) {
        if (autoSpeakReplies && ttsReady) {
            val latest = messages.lastOrNull()
            if (latest != null && !latest.isUser && latest.id != lastSpokenMsgId) {
                lastSpokenMsgId = latest.id
                speakExplanation(latest.text)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tutor Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Socratic AI Study Tutor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = engineMode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val next = !autoSpeakReplies
                                autoSpeakReplies = next
                                if (!next) tts.stop()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("toggle_tutor_voice_button")
                        ) {
                            Icon(
                                imageVector = if (autoSpeakReplies) {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                },
                                contentDescription = if (autoSpeakReplies) {
                                    "Mute spoken tutor answers"
                                } else {
                                    "Speak tutor answers aloud"
                                },
                                tint = if (autoSpeakReplies) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        IconButton(
                            onClick = onToggleFullScreen,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("toggle_fullscreen_tutor_button")
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = if (isFullScreen) "Exit Full-Screen Tutor" else "Full-Screen Tutor"
                            )
                        }
                    }
                }

                // $0 Zero-Key AI Engine Switcher & Free Public Lookup Triggers
                LazyRow(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(TutorEngineMode.entries) { mode ->
                        FilterChip(
                            selected = engineMode == mode,
                            onClick = { onSelectEngine(mode) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (mode == TutorEngineMode.GEMINI_BUILTIN) {
                                        Icons.Rounded.AutoAwesome
                                    } else {
                                        Icons.Rounded.Bolt
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("engine_chip_${mode.name.lowercase()}")
                        )
                    }

                    item {
                        AssistChip(
                            onClick = {
                                showFreeLookupBar = !showFreeLookupBar
                                if (showFreeLookupBar && lookupQuery.isBlank() && inputText.isNotBlank()) {
                                    lookupQuery = inputText.trim().take(60)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Free Wiki / Dict ($0)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_free_reference_button")
                        )
                    }
                }
            }
        }

        // Collapsible Free Public API Lookup Bar (Wikipedia REST + Free Dictionary, $0, Zero Keys)
        AnimatedVisibility(visible = showFreeLookupBar) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lookupQuery,
                        onValueChange = { lookupQuery = it },
                        placeholder = { Text("Search topic (e.g. Balanced Scorecard) or word (e.g. kaizen)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("free_reference_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onLookupWiki(lookupQuery) },
                            enabled = lookupQuery.isNotBlank() && freeRefState !is AiTaskState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("wiki_lookup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Wikipedia Concept", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = { onLookupDictionary(lookupQuery) },
                            enabled = lookupQuery.isNotBlank() && freeRefState !is AiTaskState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dictionary_lookup_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Dictionary Word", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (freeRefState is AiTaskState.Loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Querying free public API ($0, no key)...", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (freeRefState is AiTaskState.Error) {
                        Text(
                            text = freeRefState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Instant Wikipedia / Dictionary Result Card
        AnimatedVisibility(visible = freeRefResult != null) {
            freeRefResult?.let { ref ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("free_reference_result_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ref.sourceName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = ref.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (ref.subtitle.isNotBlank()) {
                                    Text(
                                        text = ref.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismissReference,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close reference result")
                            }
                        }
                        Text(
                            text = ref.summary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = { onAskTutorAboutReference(ref) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("ask_tutor_from_reference_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Turn into Exam Notes & Practice Q")
                        }
                    }
                }
            }
        }

        // Quick Tutor Capability Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TutorQuickPrompts) { (label, prefix) ->
                AssistChip(
                    onClick = { inputText = prefix },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        // Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                TutorChatBubble(
                    isUser = msg.isUser,
                    text = msg.text,
                    hasAttachment = msg.hasAttachment,
                    onSpeakClick = if (!msg.isUser) {
                        { speakExplanation(msg.text) }
                    } else null
                )
            }

            if (streamingText != null) {
                item(key = "streaming_bubble") {
                    TutorChatBubble(
                        isUser = false,
                        text = streamingText.ifEmpty { "Thinking..." },
                        hasAttachment = false,
                        isStreaming = true
                    )
                }
            }
        }

        AnimatedVisibility(visible = tutorState is AiTaskState.Error) {
            val errMsg = (tutorState as? AiTaskState.Error)?.message.orEmpty()
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = errMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Attached Photo Preview
        AnimatedVisibility(visible = attachedBitmap != null) {
            attachedBitmap?.let { bmp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Attached study note image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = "Note image attached for AI Vision Q&A",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { attachedBitmap = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove attached image")
                    }
                }
            }
        }

        // Message Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Attach photo of notes or problem"
                    )
                }

                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Ask your Study Tutor a question by voice..."
                            )
                        }
                        runCatching { voiceLauncher.launch(intent) }.onFailure {
                            Toast.makeText(
                                context,
                                "Voice speech recognizer not installed on this device.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = tutorState !is AiTaskState.Loading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("tutor_voice_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Ask question using voice message",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask or tap mic...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tutor_prompt_input")
                )

                FilledIconButton(
                    onClick = {
                        val promptToSend = inputText
                        val imgToSend = attachedBitmap
                        inputText = ""
                        attachedBitmap = null
                        onSendMessage(promptToSend, imgToSend)
                    },
                    enabled = (inputText.isNotBlank() || attachedBitmap != null) && tutorState !is AiTaskState.Loading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("tutor_send_button")
                ) {
                    if (tutorState is AiTaskState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send to AI Tutor")
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorChatBubble(
    isUser: Boolean,
    text: String,
    hasAttachment: Boolean,
    isStreaming: Boolean = false,
    onSpeakClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasAttachment) {
                    Text(
                        text = "[Image attached]",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (onSpeakClick != null && !isStreaming) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Listen to tutor explanation",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (isStreaming) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Streaming...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

private fun loadScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val original = BitmapFactory.decodeStream(stream) ?: return null
            val largest = max(original.width, original.height)
            if (largest <= maxDim) return original
            val scale = maxDim.toFloat() / largest.toFloat()
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
    }.getOrNull()
}
