package com.example.ui.jarvis

import android.speech.tts.TextToSpeech
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.OmJarvisSettings
import com.example.data.preferences.StudyLanguageMode
import com.example.data.syllabus.OmGlossaryCatalog
import com.example.ui.theme.JarvisCardInnerDark
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisTextCyan
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.utils.MaleTutorTts

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmGlossaryScreen(
    settings: OmJarvisSettings
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var partFilter by remember { mutableStateOf<String?>(null) }
    var speakingTermId by remember { mutableStateOf<String?>(null) }

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
                    speakingTermId = null
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

    val filtered = remember(query, partFilter) {
        val q = query.trim().lowercase()
        OmGlossaryCatalog.terms.filter { term ->
            val matchesPart = partFilter == null || term.partCode == partFilter
            val matchesQuery = q.isEmpty() ||
                term.term.lowercase().contains(q) ||
                term.definition.lowercase().contains(q) ||
                term.romanUrduExplanation.lowercase().contains(q) ||
                term.pakistaniExample.lowercase().contains(q)
            matchesPart && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            JarvisGlassCard(glowCyan = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OM Audio Glossary & Terminology",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = JarvisTextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${OmGlossaryCatalog.terms.size} High-Yield Exam Terms with Roman Urdu & Male Voice Pronunciation",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("Search terms (e.g. MRP, JIT, Balanced Scorecard)...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("glossary_search_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = partFilter == null,
                        onClick = { partFilter = null },
                        label = { Text("All Terms (${OmGlossaryCatalog.terms.size})") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                    listOf("PART_A" to "Part A (35%)", "PART_C" to "Part C (25%)", "PART_D" to "Part D (20%)").forEach { (code, lbl) ->
                        FilterChip(
                            selected = partFilter == code,
                            onClick = { partFilter = code },
                            label = { Text(lbl) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { term ->
            val isSpeaking = speakingTermId == term.id
            JarvisGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = term.term,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTextPrimary
                            )
                        }
                        Text(
                            text = "Definition: ${term.definition}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = JarvisCardInnerDark
                        ) {
                            Text(
                                text = "Roman Urdu: ${term.romanUrduExplanation}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = JarvisTextCyan,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Text(
                            text = "Pakistani Example: ${term.pakistaniExample}",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextPrimary
                        )
                        Text(
                            text = "Exam Tip: ${term.examTip}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = JarvisGold
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = {
                        if (isSpeaking) {
                            tts.stop()
                            speakingTermId = null
                        } else {
                            val spoken = "${term.term}. Definition: ${term.definition}. Explanation: ${term.romanUrduExplanation}. Pakistani Example: ${term.pakistaniExample}."
                            MaleTutorTts.configureMaleVoice(
                                engine = tts,
                                preferUrdu = settings.languageMode != StudyLanguageMode.ENGLISH,
                                speechSpeed = settings.ttsSpeed,
                                pitchScale = settings.ttsPitch
                            ) { speakingTermId = null }
                            tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, term.id)
                            speakingTermId = term.id
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("glossary_speak_${term.id}")
                ) {
                    Icon(if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSpeaking) "Stop Audio Pronunciation" else "Male Jarvis Pronounce & Explain")
                }
            }
        }
    }
}
