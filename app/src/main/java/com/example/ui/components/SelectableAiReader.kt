package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReaderAiAction
import com.example.data.ReaderAiSessionState

@Composable
fun SelectableAiReaderArea(
    onAiAction: (ReaderAiAction, String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val delegateToolbar = LocalTextToolbar.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var activeCopyCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isSelectionActive by remember { mutableStateOf(false) }
    var recentCopiedText by remember { mutableStateOf<String?>(null) }

    fun readClipboardText(): String? {
        val clip = clipboardManager.primaryClip ?: return null
        if (clip.itemCount <= 0) return null
        return clip.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    val wrappedToolbar = remember(delegateToolbar, clipboardManager) {
        object : TextToolbar {
            override val status: TextToolbarStatus
                get() = delegateToolbar.status

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                activeCopyCallback = onCopyRequested
                isSelectionActive = onCopyRequested != null

                val wrappedCopy: (() -> Unit)? = if (onCopyRequested != null) {
                    {
                        onCopyRequested.invoke()
                        recentCopiedText = readClipboardText()
                    }
                } else {
                    null
                }

                delegateToolbar.showMenu(
                    rect = rect,
                    onCopyRequested = wrappedCopy,
                    onPasteRequested = onPasteRequested,
                    onCutRequested = onCutRequested,
                    onSelectAllRequested = onSelectAllRequested
                )
            }

            override fun hide() {
                isSelectionActive = false
                activeCopyCallback = null
                delegateToolbar.hide()
            }
        }
    }

    fun triggerActionFromSelection(action: ReaderAiAction) {
        if (isSelectionActive && activeCopyCallback != null) {
            activeCopyCallback?.invoke()
            wrappedToolbar.hide()
        }
        val selected = readClipboardText() ?: recentCopiedText
        if (!selected.isNullOrBlank()) {
            recentCopiedText = null
            onAiAction(action, selected)
        }
    }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalTextToolbar provides wrappedToolbar) {
            SelectionContainer {
                content()
            }
        }

        AnimatedVisibility(
            visible = isSelectionActive || !recentCopiedText.isNullOrBlank(),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSelectionActive) {
                                    "Selection ready for AI Tutor"
                                } else {
                                    "Copied: \"${recentCopiedText.orEmpty().take(36)}${if ((recentCopiedText?.length ?: 0) > 36) "..." else ""}\""
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!isSelectionActive) {
                            IconButton(
                                onClick = { recentCopiedText = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss selection actions",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { triggerActionFromSelection(ReaderAiAction.SUMMARIZE_BULLETS) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("selection_summarize_bullets_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Bullet Notes", style = MaterialTheme.typography.labelLarge)
                        }

                        FilledTonalButton(
                            onClick = { triggerActionFromSelection(ReaderAiAction.EXPLAIN_CONCEPT) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("selection_explain_concept_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Explain Concept", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderAiAssistantSheet(
    state: ReaderAiSessionState,
    onSwitchAction: (ReaderAiAction) -> Unit,
    onRetry: () -> Unit,
    onSaveToNotes: () -> Unit,
    onContinueInTutor: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            shadowElevation = 14.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = if (state.action == ReaderAiAction.SUMMARIZE_BULLETS) {
                                    Icons.AutoMirrored.Rounded.FormatListBulleted
                                } else {
                                    Icons.Rounded.Psychology
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.action.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append("AI Tutor • ")
                                    append(state.pageLabel ?: state.materialTitle)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dismiss_reader_ai_sheet")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close AI explanation")
                    }
                }

                // Quick mode switcher (Summarize <-> Explain on same selection)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.action == ReaderAiAction.SUMMARIZE_BULLETS,
                        onClick = {
                            if (state.action != ReaderAiAction.SUMMARIZE_BULLETS || state.errorMessage != null) {
                                onSwitchAction(ReaderAiAction.SUMMARIZE_BULLETS)
                            }
                        },
                        label = { Text("Bullet-Point Notes") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Rounded.FormatListBulleted,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("reader_ai_switch_bullets")
                    )
                    FilterChip(
                        selected = state.action == ReaderAiAction.EXPLAIN_CONCEPT,
                        onClick = {
                            if (state.action != ReaderAiAction.EXPLAIN_CONCEPT || state.errorMessage != null) {
                                onSwitchAction(ReaderAiAction.EXPLAIN_CONCEPT)
                            }
                        },
                        label = { Text("Explain Concept") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("reader_ai_switch_explain")
                    )
                }

                // Selected passage / PDF page indicator
                if (state.selectedSnippet.isNotBlank() || state.hasPageImage) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.selectedSnippet.isNotBlank()) {
                                "\"${state.selectedSnippet}\""
                            } else {
                                "Analyzing visual & text content from ${state.pageLabel ?: "current PDF page"}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (state.errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier.testTag("retry_reader_ai_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                } else {
                    val accent = MaterialTheme.colorScheme.secondary
                    val styledResponse = remember(state.responseText, accent) {
                        buildStyledStudyText(state.responseText, accent)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (state.responseText.isBlank()) {
                            Text(
                                text = "Reading selection and preparing ${state.action.shortLabel.lowercase()}...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = styledResponse,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (state.responseText.isNotBlank() && !state.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSaveToNotes,
                            enabled = !state.savedToNotes,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("save_ai_to_notes_button")
                        ) {
                            Icon(
                                imageVector = if (state.savedToNotes) Icons.Rounded.CheckCircle else Icons.Rounded.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.savedToNotes) "Saved to Notes" else "Save to Notes")
                        }

                        Button(
                            onClick = onContinueInTutor,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("continue_in_tutor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Follow-Up")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomPassageAiDialog(
    initialText: String,
    onRunAction: (ReaderAiAction, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var passageText by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analyze Selected or Copied Text") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste text copied from a Drive file, Google Doc, or type any complex concept to get instant bullet-point notes or a clear breakdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passageText,
                    onValueChange = { passageText = it },
                    label = { Text("Passage or Concept") },
                    placeholder = { Text("Paste or enter concept (e.g., OPT Drum-Buffer-Rope, Balanced Scorecard)...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_passage_ai_input")
                )
                TextButton(
                    onClick = {
                        val clip = clipboardManager.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val pasted = clip.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().trim()
                            if (pasted.isNotEmpty()) {
                                passageText = pasted
                            }
                        }
                    }
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paste from Clipboard")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { onRunAction(ReaderAiAction.EXPLAIN_CONCEPT, passageText) },
                    enabled = passageText.isNotBlank(),
                    modifier = Modifier.testTag("dialog_explain_concept_button")
                ) {
                    Text("Explain")
                }
                Button(
                    onClick = { onRunAction(ReaderAiAction.SUMMARIZE_BULLETS, passageText) },
                    enabled = passageText.isNotBlank(),
                    modifier = Modifier.testTag("dialog_summarize_bullets_button")
                ) {
                    Text("Bullet Notes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun buildStyledStudyText(markdown: String, accentColor: Color): AnnotatedString {
    val lines = markdown.lines()
    return buildAnnotatedString {
        lines.forEachIndexed { lineIdx, rawLine ->
            val trimmedStart = rawLine.trimStart()
            when {
                trimmedStart.startsWith("#### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accentColor, fontSize = 15.sp)) {
                        appendInlineFormatted(trimmedStart.removePrefix("#### ").trim())
                    }
                }
                trimmedStart.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accentColor, fontSize = 17.sp)) {
                        appendInlineFormatted(trimmedStart.removePrefix("### ").trim())
                    }
                }
                trimmedStart.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accentColor, fontSize = 18.sp)) {
                        appendInlineFormatted(trimmedStart.removePrefix("## ").trim())
                    }
                }
                trimmedStart.startsWith("- ") || trimmedStart.startsWith("* ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accentColor)) {
                        append("• ")
                    }
                    appendInlineFormatted(trimmedStart.drop(2))
                }
                else -> appendInlineFormatted(rawLine)
            }
            if (lineIdx < lines.lastIndex) append('\n')
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatted(text: String) {
    var i = 0
    while (i < text.length) {
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", startIndex = i + 2)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        if (text[i] == '*' && (i + 1 >= text.length || text[i + 1] != ' ')) {
            val end = text.indexOf('*', startIndex = i + 1)
            if (end != -1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
