package com.example.ui.jarvis

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.StudyLanguageMode
import com.example.data.security.TutorStyleRequest
import com.example.data.syllabus.SyllabusTopic
import com.example.domain.AnalyticsSnapshot
import com.example.ui.theme.EmeraldSuccessBright
import com.example.ui.utils.MaleTutorTts
import com.example.viewmodel.JarvisChatMessage

@Composable
fun JarvisTeacherScreen(
    settings: OmJarvisSettings,
    isOnline: Boolean,
    activeTopic: SyllabusTopic?,
    messages: List<JarvisChatMessage>,
    isLoading: Boolean,
    onAskJarvis: (String, TutorStyleRequest) -> Unit,
    onClearChat: () -> Unit,
    onLaunchCallJarvis: () -> Unit,
    onShowFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var inputDraft by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(TutorStyleRequest.DEFAULT_BILINGUAL) }
    var autoSpeakMale by remember { mutableStateOf(false) }

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

    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull()
        if (autoSpeakMale && last != null && !last.isUser) {
            MaleTutorTts.configureMaleVoice(
                engine = tts,
                preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                speechSpeed = settings.ttsSpeed,
                pitchScale = settings.ttsPitch
            )
            tts.speak(last.text.take(900), TextToSpeech.QUEUE_FLUSH, null, "jarvis_${last.id}")
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onAskJarvis(spoken, selectedStyle)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tutor Header
        Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Jarvis Teacher (Male Mentor)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = if (activeTopic != null) "Context: Unit ${activeTopic.unit} — ${activeTopic.name}" else "ICMA M3 Organizational Management",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = onLaunchCallJarvis,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("teacher_call_jarvis_btn")
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Call")
                        }
                        IconButton(onClick = onClearChat, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Reset chat")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOnline && settings.aiProviderType != com.example.data.preferences.AiProviderType.OFFLINE_ONLY) {
                            "Provider: ${settings.aiProviderType.label} (Encrypted Key)"
                        } else {
                            "Offline Jarvis Engine Active"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Male Auto-Voice", style = MaterialTheme.typography.labelSmall)
                        Switch(checked = autoSpeakMale, onCheckedChange = { autoSpeakMale = it })
                    }
                }

                // 9 Coaching Style Quick Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TutorStyleRequest.entries.forEach { style ->
                        AssistChip(
                            onClick = {
                                selectedStyle = style
                                val prompt = activeTopic?.let { "${style.label} for Unit ${it.unit}: ${it.name}" } ?: style.label
                                onAskJarvis(prompt, style)
                            },
                            label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                }
            }
        }

        // Chat Stream
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!msg.isUser) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${msg.providerLabel} • ${msg.styleLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            MaleTutorTts.configureMaleVoice(
                                                engine = tts,
                                                preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                                                speechSpeed = settings.ttsSpeed,
                                                pitchScale = settings.ttsPitch
                                            )
                                            tts.speak(msg.text.take(900), TextToSpeech.QUEUE_FLUSH, null, "msg_${msg.id}")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak answer in Male Voice", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Text(text = msg.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("OM Jarvis is preparing exam explanation...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Bottom Input Bar
        Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask OM Jarvis a question...")
                        }
                        runCatching { speechLauncher.launch(intent) }
                            .onFailure { onShowFeedback("Voice input service unavailable on this device — type your question.") }
                    },
                    modifier = Modifier.size(48.dp).testTag("jarvis_mic_btn")
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Voice question")
                }

                OutlinedTextField(
                    value = inputDraft,
                    onValueChange = { inputDraft = it },
                    placeholder = { Text("Ask any OM concept, formula, or Urdu...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("jarvis_chat_input")
                )

                IconButton(
                    onClick = {
                        if (inputDraft.isNotBlank()) {
                            onAskJarvis(inputDraft, selectedStyle)
                            inputDraft = ""
                        }
                    },
                    modifier = Modifier.size(48.dp).testTag("jarvis_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send question")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallJarvisScreen(
    settings: OmJarvisSettings,
    analytics: AnalyticsSnapshot,
    topics: List<SyllabusTopic>,
    onExitCall: () -> Unit,
    onLogSessionMinutes: (Int) -> Unit,
    onShowFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var isRinging by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(true) }
    var showAnswer by remember { mutableStateOf(false) }
    var questionIdx by remember { mutableIntStateOf(0) }
    var voiceCommandLog by remember { mutableStateOf("Ready for oral viva check-in.") }

    val vivaDeck = remember(topics, analytics.weakest5) {
        val priority = analytics.weakest5.map { it.topic }.ifEmpty { topics }
        priority.ifEmpty { topics }
    }
    val currentTopic = vivaDeck.getOrNull(questionIdx % vivaDeck.size.coerceAtLeast(1))

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

    val speakText: (String) -> Unit = { txt ->
        if (speakerOn) {
            MaleTutorTts.configureMaleVoice(
                engine = tts,
                preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                speechSpeed = settings.ttsSpeed,
                pitchScale = settings.ttsPitch
            )
            tts.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "call_viva")
        }
    }

    val voiceCommandLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            val lower = spoken.lowercase()
            voiceCommandLog = "Heard: \"$spoken\""
            when {
                lower.contains("repeat") -> currentTopic?.let { speakText("Repeating. Unit ${it.unit}: ${it.name}. Explain the core formula and points.") }
                lower.contains("next") -> {
                    showAnswer = false
                    questionIdx += 1
                    vivaDeck.getOrNull(questionIdx % vivaDeck.size.coerceAtLeast(1))?.let {
                        speakText("Next viva question. Unit ${it.unit}: ${it.name}. ${it.description}")
                    }
                }
                lower.contains("stop") || lower.contains("end") -> {
                    tts.stop()
                    onLogSessionMinutes(10)
                    onExitCall()
                }
                else -> {
                    showAnswer = true
                    currentTopic?.let { speakText("Good effort Saifullah! Key answer: ${it.formulae}. ${it.romanUrduSummary}") }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { tts.stop(); onExitCall() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Jarvis Mentor Call",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Text(
                    text = if (isRinging) "INCOMING JARVIS STUDY CALL" else "ACTIVE MENTOR CALL",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldSuccessBright
                )
                Text(
                    text = "OM Jarvis (Male Mentor)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRinging) {
                        "Assalam-o-Alaikum Saifullah! Daily Study Plan & Viva Check-In (${analytics.daysRemaining}d to Dec 7)."
                    } else {
                        voiceCommandLog
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isRinging && currentTopic != null) {
                    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Viva Prompt • Unit ${currentTopic.unit}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text("Explain ${currentTopic.name} and give its key formula / Pakistani business example.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (showAnswer) {
                                Text("Formula: ${currentTopic.formulae}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(currentTopic.romanUrduSummary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak answer or say Repeat / Next / Stop")
                                }
                                runCatching { voiceCommandLauncher.launch(intent) }
                                    .onFailure { onShowFeedback("Voice recognizer unavailable — use the Repeat/Next/Answer buttons below.") }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Speak Reply")
                        }
                        OutlinedButton(
                            onClick = {
                                currentTopic.let {
                                    speakText("Unit ${it.unit}: ${it.name}. ${it.description}. Formula: ${it.formulae}")
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Repeat")
                        }
                        OutlinedButton(
                            onClick = {
                                showAnswer = !showAnswer
                                if (showAnswer) speakText("Key formula: ${currentTopic.formulae}. ${currentTopic.romanUrduSummary}")
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(if (showAnswer) "Hide Answer" else "Show Answer") }
                        OutlinedButton(
                            onClick = {
                                showAnswer = false
                                questionIdx += 1
                                vivaDeck.getOrNull(questionIdx % vivaDeck.size.coerceAtLeast(1))?.let {
                                    speakText("Next question. Unit ${it.unit}: ${it.name}.")
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Next")
                        }
                    }
                }
            }

            // Bottom Call Controls
            if (isRinging) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { tts.stop(); onExitCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.heightIn(min = 56.dp).testTag("call_decline_btn")
                    ) {
                        Icon(Icons.Rounded.CallEnd, contentDescription = "Decline Call")
                        Spacer(Modifier.width(6.dp))
                        Text("Decline")
                    }
                    Button(
                        onClick = {
                            isRinging = false
                            currentTopic?.let {
                                speakText("Assalam-o-Alaikum Saifullah! ${analytics.daysRemaining} days left to Dec 7. Your readiness is ${analytics.readiness.readinessScore} out of 100. First viva question: Unit ${it.unit}, ${it.name}. What is its core formula and purpose?")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccessBright, contentColor = Color.Black),
                        modifier = Modifier.heightIn(min = 56.dp).testTag("call_answer_btn")
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = "Answer Call")
                        Spacer(Modifier.width(6.dp))
                        Text("Answer", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { isMuted = !isMuted }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic, contentDescription = "Toggle Mute")
                        Spacer(Modifier.width(4.dp))
                        Text(if (isMuted) "Muted" else "Mute")
                    }
                    OutlinedButton(
                        onClick = {
                            speakerOn = !speakerOn
                            if (!speakerOn) tts.stop()
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(if (speakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.Stop, contentDescription = "Toggle Speaker")
                        Spacer(Modifier.width(4.dp))
                        Text(if (speakerOn) "Speaker" else "Muted TTS")
                    }
                    Button(
                        onClick = {
                            tts.stop()
                            onLogSessionMinutes(10)
                            onExitCall()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("call_end_btn")
                    ) {
                        Icon(Icons.Rounded.CallEnd, contentDescription = "End Call")
                        Spacer(Modifier.width(4.dp))
                        Text("End Call")
                    }
                }
            }
        }
    }
}
