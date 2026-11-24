package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.DriveUrlParser
import com.example.data.FreeReferenceResult
import com.example.data.MaterialProgress
import com.example.data.McqBank
import com.example.data.PomodoroPhase
import com.example.data.PomodoroState
import com.example.data.PreloadedMaterials
import com.example.data.QuizAttempt
import com.example.data.QuizSessionState
import com.example.data.ReaderAiAction
import com.example.data.ReaderAiSessionState
import com.example.data.StudyMaterial
import com.example.data.StudyPlanItem
import com.example.data.TutorEngineMode
import com.example.data.TutorMessage
import com.example.ui.components.CustomPassageAiDialog
import com.example.ui.components.McqQuizArenaContent
import com.example.ui.components.PdfBookViewer
import com.example.ui.components.PomodoroConfigDialog
import com.example.ui.components.PomodoroCountdownRing
import com.example.ui.components.ReaderAiAssistantSheet
import com.example.ui.components.SelectableAiReaderArea
import com.example.ui.components.StudyRoadmapCard
import com.example.ui.components.buildStyledStudyText
import com.example.viewmodel.AiTaskState

@Composable
fun ImmersiveSystemBarsEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullScreenReadingViewer(
    material: StudyMaterial,
    openAsVideo: Boolean,
    progress: MaterialProgress?,
    readerAiState: ReaderAiSessionState,
    onRunReaderAiAction: (ReaderAiAction, String, String?, Bitmap?) -> Unit,
    onSwitchReaderAiAction: (ReaderAiAction) -> Unit,
    onRetryReaderAi: () -> Unit,
    onSaveReaderAiToNotes: () -> Unit,
    onContinueReaderAiInTutor: () -> Unit,
    onDismissReaderAi: () -> Unit,
    onToggleComplete: (Boolean, Long) -> Unit,
    onSaveNotes: (String) -> Unit,
    onAskTutor: (StudyMaterial) -> Unit,
    onGenerateFlashcards: (StudyMaterial) -> Unit,
    isGeneratingFlashcards: Boolean = false,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler {
        if (readerAiState.isVisible) {
            onDismissReaderAi()
        } else {
            onExit()
        }
    }

    val context = LocalContext.current
    val hasUrl = material.sourceUrl.isNotBlank()
    val hasBundledPdf = material.isBundledPdf
    val embedUrl = remember(material.sourceUrl) { DriveUrlParser.toEmbedUrl(material.sourceUrl) }
    val chapters = remember(material.assetPath) { PreloadedMaterials.chapterJumpsFor(material.assetPath) }

    var showPdfView by remember(material.id, hasBundledPdf) { mutableStateOf(hasBundledPdf) }
    var showWebPreview by remember(material.id, hasUrl) { mutableStateOf(hasUrl && !hasBundledPdf) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showCustomPassageDialog by remember { mutableStateOf(false) }
    var customPassageSeed by remember { mutableStateOf("") }
    var hideChrome by remember { mutableStateOf(false) }
    var isLandscapeMode by remember(material.id) { mutableStateOf(false) }
    var readerFontSize by remember { mutableFloatStateOf(17f) }
    var webLoadProgress by remember { mutableIntStateOf(0) }
    var notesDraft by remember(progress?.personalNotes) {
        mutableStateOf(progress?.personalNotes.orEmpty())
    }
    var customVideoView by remember { mutableStateOf<View?>(null) }

    fun openPassageAiHelper() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()
        customPassageSeed = clipText
        showCustomPassageDialog = true
    }

    val activity = context as? Activity
    DisposableEffect(activity, isLandscapeMode) {
        if (activity != null) {
            activity.requestedOrientation = if (isLandscapeMode) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val isCompleted = progress?.completed == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (openAsVideo) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = !hideChrome) {
                Surface(
                    color = if (openAsVideo) Color(0xFF0B1526) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onExit,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("exit_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Exit Full-Screen",
                                    tint = if (openAsVideo) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = material.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (openAsVideo) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        hasBundledPdf -> "Full-Screen Offline PDF Book"
                                        openAsVideo -> "Full-Screen Video Clip Mode"
                                        else -> "Full-Screen Reading Mode"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (openAsVideo) Color(0xFF2DD4BF) else MaterialTheme.colorScheme.secondary
                                )
                            }

                            IconButton(
                                onClick = { onAskTutor(material) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_ask_tutor_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "Ask AI Tutor about this material",
                                    tint = if (openAsVideo) Color(0xFF2DD4BF) else MaterialTheme.colorScheme.secondary
                                )
                            }

                            IconButton(
                                onClick = { isLandscapeMode = !isLandscapeMode },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_rotate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ScreenRotation,
                                    contentDescription = if (isLandscapeMode) "Switch to Portrait" else "Rotate to Landscape",
                                    tint = if (isLandscapeMode) {
                                        if (openAsVideo) Color(0xFF2DD4BF) else MaterialTheme.colorScheme.secondary
                                    } else if (openAsVideo) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                            IconButton(
                                onClick = { showNotesSheet = !showNotesSheet },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = "Toggle study notes",
                                    tint = if (openAsVideo) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    onToggleComplete(!isCompleted, if (!isCompleted) material.estimatedMinutes else 0L)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Mark completed",
                                    tint = if (isCompleted) Color(0xFF34D399) else if (openAsVideo) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { hideChrome = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("maximize_pure_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fullscreen,
                                    contentDescription = "Maximize edge-to-edge full-screen",
                                    tint = if (openAsVideo) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (hasBundledPdf) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = showPdfView,
                                    onClick = { showPdfView = true },
                                    label = { Text("PDF Book") }
                                )
                                FilterChip(
                                    selected = !showPdfView,
                                    onClick = { showPdfView = false },
                                    label = { Text("Index & Solutions") }
                                )
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { onGenerateFlashcards(material) },
                                    enabled = !isGeneratingFlashcards,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("pdf_make_flashcards_button")
                                ) {
                                    if (isGeneratingFlashcards) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Creating...", style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Cards", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onAskTutor(material) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ask AI", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else if (hasUrl) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = showWebPreview,
                                    onClick = { showWebPreview = true },
                                    label = { Text(if (openAsVideo) "Drive Clip Player" else "Drive / Doc View") }
                                )
                                FilterChip(
                                    selected = !showWebPreview,
                                    onClick = { showWebPreview = false },
                                    label = { Text("Reader & Summary") }
                                )
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { openPassageAiHelper() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("webview_selection_ai_button")
                                ) {
                                    Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Selection AI", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.sourceUrl))
                                        runCatching { context.startActivity(intent) }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Drive", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (!hasBundledPdf && showWebPreview && webLoadProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { webLoadProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Main Full-Screen Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (hasBundledPdf && showPdfView) {
                    PdfBookViewer(
                        assetPath = material.assetPath,
                        chapters = chapters,
                        onAiAnalyzePage = { action, pageNum, bmp ->
                            onRunReaderAiAction(action, "", "Page $pageNum", bmp)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (showWebPreview && embedUrl.isNotBlank()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT

                                webViewClient = object : WebViewClient() {}
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        webLoadProgress = newProgress
                                    }

                                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                        customVideoView = view
                                    }

                                    override fun onHideCustomView() {
                                        customVideoView = null
                                    }
                                }
                                loadUrl(embedUrl)
                            }
                        },
                        update = { webView ->
                            if (webView.url != embedUrl && webView.tag != embedUrl) {
                                webView.tag = embedUrl
                                webView.loadUrl(embedUrl)
                            }
                        }
                    )
                } else {
                    val accentColor = MaterialTheme.colorScheme.secondary
                    val styledMarkdown = remember(material.contentMarkdown, accentColor) {
                        buildStyledStudyText(material.contentMarkdown, accentColor)
                    }

                    SelectableAiReaderArea(
                        onAiAction = { action, selectedText ->
                            onRunReaderAiAction(action, selectedText, null, null)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 96.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Long-press & drag any text to get AI Bullet Notes or Explanations",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    TextButton(
                                        onClick = { openPassageAiHelper() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("open_custom_passage_ai_button")
                                    ) {
                                        Text("Paste/Type", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = material.category.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { readerFontSize = (readerFontSize - 2f).coerceAtLeast(14f) }
                                    ) {
                                        Text("A-", style = MaterialTheme.typography.titleSmall)
                                    }
                                    Icon(Icons.Rounded.FormatSize, contentDescription = "Font size", modifier = Modifier.size(18.dp))
                                    IconButton(
                                        onClick = { readerFontSize = (readerFontSize + 2f).coerceAtMost(26f) }
                                    ) {
                                        Text("A+", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }

                            Text(
                                text = material.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            if (material.description.isNotBlank()) {
                                Text(
                                    text = material.description,
                                    fontSize = readerFontSize.sp,
                                    lineHeight = (readerFontSize * 1.45f).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (material.contentMarkdown.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = styledMarkdown,
                                        fontSize = readerFontSize.sp,
                                        lineHeight = (readerFontSize * 1.55f).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { onAskTutor(material) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Full Study Breakdown in AI Tutor")
                            }

                            OutlinedButton(
                                onClick = { onGenerateFlashcards(material) },
                                enabled = !isGeneratingFlashcards,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reader_make_flashcards_button")
                            ) {
                                if (isGeneratingFlashcards) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generating Flashcards...")
                                } else {
                                    Icon(Icons.Rounded.Style, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generate Flashcards from This Reading")
                                }
                            }
                        }
                    }
                }

                // Inline HTML5 fullscreen video overlay if triggered by WebView video player
                customVideoView?.let { videoView ->
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                setBackgroundColor(android.graphics.Color.BLACK)
                                addView(
                                    videoView,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Personal Notes Bottom Sheet Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showNotesSheet,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Personal Study Notes", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { showNotesSheet = false }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Close notes")
                                }
                            }
                            OutlinedTextField(
                                value = notesDraft,
                                onValueChange = { notesDraft = it },
                                placeholder = { Text("Jot key takeaways, formulas, or timestamps...") },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onSaveNotes(notesDraft)
                                    showNotesSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Notes to Cloud")
                            }
                        }
                    }
                }

                // Inline AI Selection Summary / Concept Explanation Sheet
                ReaderAiAssistantSheet(
                    state = readerAiState,
                    onSwitchAction = onSwitchReaderAiAction,
                    onRetry = onRetryReaderAi,
                    onSaveToNotes = onSaveReaderAiToNotes,
                    onContinueInTutor = onContinueReaderAiInTutor,
                    onDismiss = onDismissReaderAi,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (showCustomPassageDialog) {
            CustomPassageAiDialog(
                initialText = customPassageSeed,
                onRunAction = { action, passage ->
                    showCustomPassageDialog = false
                    onRunReaderAiAction(action, passage, null, null)
                },
                onDismiss = { showCustomPassageDialog = false }
            )
        }

        // Floating restore button when pure full-screen chrome is hidden
        if (hideChrome) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.clickable { hideChrome = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = "Show controls", tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Controls", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.clickable { onExit() }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FullscreenExit,
                        contentDescription = "Exit Full-Screen",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenMcqExam(
    session: QuizSessionState,
    onSelectOption: (Int, Int) -> Unit,
    onSelectQuestionIndex: (Int) -> Unit,
    onResetQuiz: () -> Unit,
    onSubmitQuiz: () -> Unit,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler { onExit() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        McqQuizArenaContent(
            session = session,
            isFullScreen = true,
            onToggleFullScreen = onExit,
            onSelectOption = onSelectOption,
            onSelectQuestionIndex = onSelectQuestionIndex,
            onResetQuiz = onResetQuiz,
            onSubmitQuiz = onSubmitQuiz,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        )
    }
}

@Composable
fun FullScreenRoadmapScreen(
    materials: List<StudyMaterial>,
    progressMap: Map<String, MaterialProgress>,
    plans: List<StudyPlanItem>,
    attempts: List<QuizAttempt>,
    onOpenMaterial: (StudyMaterial, Boolean) -> Unit,
    onToggleMaterialComplete: (String, Boolean, Long) -> Unit,
    onLaunchMcqBank: (McqBank) -> Unit,
    onTogglePlanItem: (String, Boolean) -> Unit,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler { onExit() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exit_fullscreen_roadmap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FullscreenExit,
                            contentDescription = "Exit Full-Screen Roadmap"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Study Roadmap & Milestones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FULL-SCREEN PROGRESS TRACKER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                StudyRoadmapCard(
                    materials = materials,
                    progressMap = progressMap,
                    plans = plans,
                    attempts = attempts,
                    onOpenMaterial = onOpenMaterial,
                    onToggleMaterialComplete = onToggleMaterialComplete,
                    onLaunchMcqBank = onLaunchMcqBank,
                    onTogglePlanItem = onTogglePlanItem,
                    isFullScreen = true,
                    onToggleFullScreen = onExit
                )
            }
        }
    }
}

@Composable
fun FullScreenTutorStudio(
    messages: List<TutorMessage>,
    streamingText: String?,
    tutorState: AiTaskState,
    engineMode: TutorEngineMode = TutorEngineMode.GEMINI_BUILTIN,
    freeRefState: AiTaskState = AiTaskState.Idle,
    freeRefResult: FreeReferenceResult? = null,
    onSelectEngine: (TutorEngineMode) -> Unit = {},
    onLookupWiki: (String) -> Unit = {},
    onLookupDictionary: (String) -> Unit = {},
    onDismissReference: () -> Unit = {},
    onAskTutorAboutReference: (FreeReferenceResult) -> Unit = {},
    onSendMessage: (String, Bitmap?) -> Unit,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler { onExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TutorScreen(
            messages = messages,
            streamingText = streamingText,
            tutorState = tutorState,
            engineMode = engineMode,
            freeRefState = freeRefState,
            freeRefResult = freeRefResult,
            isFullScreen = true,
            onToggleFullScreen = onExit,
            onSelectEngine = onSelectEngine,
            onLookupWiki = onLookupWiki,
            onLookupDictionary = onLookupDictionary,
            onDismissReference = onDismissReference,
            onAskTutorAboutReference = onAskTutorAboutReference,
            onSendMessage = onSendMessage
        )
    }
}

@Composable
fun FullScreenPomodoroScreen(
    state: PomodoroState,
    onToggleRunPause: () -> Unit,
    onReset: () -> Unit,
    onSkipPhase: () -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onUpdateConfig: (Int, Int, Int, Int, Boolean) -> Unit,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler { onExit() }

    var showConfigDialog by remember { mutableStateOf(false) }

    val accentColor = when (state.phase) {
        PomodoroPhase.WORK -> MaterialTheme.colorScheme.secondary
        PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
        PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("exit_fullscreen_pomodoro_button")
                ) {
                    Icon(Icons.Rounded.FullscreenExit, contentDescription = "Exit Full-Screen Pomodoro")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FOCUS TIMER",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    Text(
                        text = "${state.completedWorkSessions} focus blocks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showConfigDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Configure Pomodoro intervals")
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PomodoroPhase.entries.forEach { phase ->
                        FilterChip(
                            selected = state.phase == phase,
                            onClick = { onSelectPhase(phase) },
                            label = { Text(phase.label) }
                        )
                    }
                }

                PomodoroCountdownRing(
                    state = state,
                    accentColor = accentColor,
                    ringSize = 240.dp,
                    strokeWidth = 14.dp
                )

                Text(
                    text = when (state.phase) {
                        PomodoroPhase.WORK -> "Deep focus mode • ${state.workMinutes} min interval"
                        PomodoroPhase.SHORT_BREAK -> "Recharge • ${state.shortBreakMinutes} min short break"
                        PomodoroPhase.LONG_BREAK -> "Well earned • ${state.longBreakMinutes} min long break"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = onToggleRunPause,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.isRunning) "Pause" else "Start Focus",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSkipPhase,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Skip")
                }
            }
        }
    }

    if (showConfigDialog) {
        PomodoroConfigDialog(
            state = state,
            onDismiss = { showConfigDialog = false },
            onSave = { work, shortBrk, longBrk, cycles, auto ->
                onUpdateConfig(work, shortBrk, longBrk, cycles, auto)
                showConfigDialog = false
            }
        )
    }
}
