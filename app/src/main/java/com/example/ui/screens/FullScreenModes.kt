package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
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
import com.example.data.MaterialProgress
import com.example.data.McqQuestion
import com.example.data.PomodoroPhase
import com.example.data.PomodoroState
import com.example.data.PreloadedMaterials
import com.example.data.StudyMaterial
import com.example.data.TutorMessage
import com.example.ui.components.PdfBookViewer
import com.example.ui.components.PomodoroConfigDialog
import com.example.ui.components.PomodoroCountdownRing
import com.example.viewmodel.AiTaskState
import kotlinx.coroutines.delay

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
    onToggleComplete: (Boolean, Long) -> Unit,
    onSaveNotes: (String) -> Unit,
    onAskTutor: (StudyMaterial) -> Unit,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler { onExit() }

    val context = LocalContext.current
    val hasUrl = material.sourceUrl.isNotBlank()
    val hasBundledPdf = material.isBundledPdf
    val embedUrl = remember(material.sourceUrl) { DriveUrlParser.toEmbedUrl(material.sourceUrl) }
    val chapters = remember(material.assetPath) { PreloadedMaterials.chapterJumpsFor(material.assetPath) }

    var showPdfView by remember(material.id, hasBundledPdf) { mutableStateOf(hasBundledPdf) }
    var showWebPreview by remember(material.id, hasUrl) { mutableStateOf(hasUrl && !hasBundledPdf) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var hideChrome by remember { mutableStateOf(false) }
    var readerFontSize by remember { mutableFloatStateOf(17f) }
    var webLoadProgress by remember { mutableIntStateOf(0) }
    var notesDraft by remember(progress?.personalNotes) {
        mutableStateOf(progress?.personalNotes.orEmpty())
    }
    var customVideoView by remember { mutableStateOf<View?>(null) }

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
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = "Hide top bar for pure full-screen",
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(22.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                    text = material.contentMarkdown,
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
                            Text("Ask AI Tutor to Summarize or Quiz This")
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
            }
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
    topic: String,
    questions: List<McqQuestion>,
    onSubmitScore: (Int, Int, Long) -> Unit,
    onExit: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    BackHandler { onExit() }

    var currentIndex by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var isSubmitted by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isSubmitted) {
        while (!isSubmitted) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    val currentQuestion = questions.getOrNull(currentIndex)
    val correctCount = remember(isSubmitted, selectedAnswers.size) {
        questions.indices.count { idx ->
            selectedAnswers[idx] == questions[idx].correctIndex
        }
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
        ) {
            // Top Full-Screen Exam Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exit_fullscreen_mcq_button")
                    ) {
                        Icon(Icons.Rounded.FullscreenExit, contentDescription = "Exit Full-Screen Exam")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "FULL-SCREEN MCQ ARENA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        val mins = elapsedSeconds / 60
                        val secs = elapsedSeconds % 60
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Question Navigator Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(questions) { index, q ->
                    val answered = selectedAnswers.containsKey(index)
                    val isCurrent = index == currentIndex
                    val pillColor = when {
                        isSubmitted && selectedAnswers[index] == q.correctIndex -> Color(0xFF059669)
                        isSubmitted -> MaterialTheme.colorScheme.error
                        isCurrent -> MaterialTheme.colorScheme.primary
                        answered -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val textColor = when {
                        isSubmitted || isCurrent -> Color.White
                        answered -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = CircleShape,
                        color = pillColor,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { currentIndex = index }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor
                            )
                        }
                    }
                }
            }

            if (currentQuestion != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isSubmitted) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Exam Completed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Score: $correctCount / ${questions.size} (${(correctCount * 100) / questions.size.coerceAtLeast(1)}%)",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(onClick = onExit) {
                                    Text("Done")
                                }
                            }
                        }
                    }

                    Text(
                        text = "Question ${currentIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = currentQuestion.question,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    currentQuestion.options.forEachIndexed { optIdx, optionText ->
                        val isSelected = selectedAnswers[currentIndex] == optIdx
                        val isCorrectOption = optIdx == currentQuestion.correctIndex

                        val containerColor = when {
                            isSubmitted && isCorrectOption -> Color(0xFF059669).copy(alpha = 0.16f)
                            isSubmitted && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !isSubmitted) {
                                    selectedAnswers[currentIndex] = optIdx
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ('A' + optIdx).toString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (isSubmitted) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "AI Tutor Explanation",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = currentQuestion.explanation,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Exam Navigation Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex -= 1 },
                        enabled = currentIndex > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Previous")
                    }

                    if (!isSubmitted && currentIndex == questions.lastIndex) {
                        Button(
                            onClick = {
                                isSubmitted = true
                                onSubmitScore(questions.size, correctCount, elapsedSeconds.toLong())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("submit_mcq_exam_button")
                        ) {
                            Text("Submit Exam", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentIndex < questions.lastIndex) {
                                    currentIndex += 1
                                } else {
                                    onExit()
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text(if (currentIndex < questions.lastIndex) "Next Question" else "Finish")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenTutorStudio(
    messages: List<TutorMessage>,
    streamingText: String?,
    tutorState: AiTaskState,
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
            isFullScreen = true,
            onToggleFullScreen = onExit,
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
