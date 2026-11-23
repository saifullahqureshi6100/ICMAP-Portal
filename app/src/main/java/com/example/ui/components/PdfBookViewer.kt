package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PdfChapterJump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfBookViewer(
    assetPath: String,
    chapters: List<PdfChapterJump>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var cachedFile by remember(assetPath) { mutableStateOf<File?>(null) }
    var loadError by remember(assetPath) { mutableStateOf<String?>(null) }
    var currentPageIndex by remember(assetPath) { mutableIntStateOf(0) }
    var pageCount by remember(assetPath) { mutableIntStateOf(0) }
    var zoomScale by remember(assetPath) { mutableFloatStateOf(1f) }
    var renderedBitmap by remember(assetPath) { mutableStateOf<Bitmap?>(null) }
    var isRenderingPage by remember(assetPath) { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }

    // Copy bundled PDF from assets to cacheDir once on IO dispatcher
    LaunchedEffect(assetPath) {
        isRenderingPage = true
        loadError = null
        val res = withContext(Dispatchers.IO) {
            runCatching {
                val outFile = File(context.cacheDir, assetPath)
                if (!outFile.exists() || outFile.length() == 0L) {
                    context.assets.open(assetPath).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                outFile
            }
        }
        res.onSuccess { file ->
            cachedFile = file
        }.onFailure { err ->
            loadError = err.localizedMessage ?: "Failed to open PDF asset."
            isRenderingPage = false
        }
    }

    val rendererHolder = remember(cachedFile) {
        val file = cachedFile ?: return@remember null
        runCatching {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pfd to renderer
        }.onFailure { err ->
            loadError = err.localizedMessage ?: "Unable to initialize PDF reader."
        }.getOrNull()
    }

    DisposableEffect(rendererHolder) {
        if (rendererHolder != null) {
            pageCount = rendererHolder.second.pageCount
            if (currentPageIndex >= pageCount && pageCount > 0) {
                currentPageIndex = 0
            }
        }
        onDispose {
            runCatching { rendererHolder?.second?.close() }
            runCatching { rendererHolder?.first?.close() }
        }
    }

    // Render active page to high-res crisp white-backed Bitmap on Dispatchers.IO
    LaunchedEffect(rendererHolder, currentPageIndex, zoomScale) {
        val renderer = rendererHolder?.second ?: return@LaunchedEffect
        if (renderer.pageCount <= 0) {
            isRenderingPage = false
            return@LaunchedEffect
        }
        val safeIdx = currentPageIndex.coerceIn(0, renderer.pageCount - 1)
        isRenderingPage = true
        val bitmapResult = withContext(Dispatchers.IO) {
            runCatching {
                val page = renderer.openPage(safeIdx)
                try {
                    val targetWidth = (1150f * zoomScale).toInt().coerceIn(900, 2300)
                    val aspect = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
                    val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    Canvas(bmp).drawColor(AndroidColor.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                } finally {
                    page.close()
                }
            }
        }
        bitmapResult.onSuccess { newBmp ->
            renderedBitmap = newBmp
            isRenderingPage = false
        }.onFailure { err ->
            loadError = err.localizedMessage ?: "Failed to render PDF page."
            isRenderingPage = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        // Main PDF Page Viewport
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val viewportWidthDp = maxWidth
            val density = LocalDensity.current

            if (loadError != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val bmp = renderedBitmap
                if (bmp != null) {
                    val vScroll = rememberScrollState()
                    val hScroll = rememberScrollState()
                    LaunchedEffect(currentPageIndex) {
                        vScroll.scrollTo(0)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vScroll)
                            .then(
                                if (zoomScale > 1.05f) Modifier.horizontalScroll(hScroll)
                                else Modifier
                            )
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val displayWidth = with(density) {
                            ((viewportWidthDp - 20.dp) * zoomScale).coerceAtLeast(240.dp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            color = Color.White
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Page ${currentPageIndex + 1} of $pageCount",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.width(displayWidth)
                            )
                        }
                    }
                }

                if (isRenderingPage) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Rendering page...", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Bottom PDF Pagination & Chapter Controls Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (currentPageIndex > 0) currentPageIndex -= 1 },
                    enabled = currentPageIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("pdf_prev_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous page"
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(enabled = pageCount > 0) { showJumpDialog = true }
                        .testTag("pdf_page_jump_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (chapters.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (pageCount > 0) {
                                "Page ${currentPageIndex + 1} / $pageCount"
                            } else {
                                "Loading..."
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        zoomScale = when {
                            zoomScale < 1.3f -> 1.5f
                            zoomScale < 1.8f -> 2.0f
                            else -> 1.0f
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("pdf_zoom_button")
                ) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Cycle zoom", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${zoomScale}x", style = MaterialTheme.typography.labelMedium)
                }

                IconButton(
                    onClick = { if (currentPageIndex < pageCount - 1) currentPageIndex += 1 },
                    enabled = pageCount > 0 && currentPageIndex < pageCount - 1,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("pdf_next_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next page"
                    )
                }
            }
        }
    }

    if (showJumpDialog && pageCount > 0) {
        PdfJumpDialog(
            currentPage = currentPageIndex + 1,
            totalPages = pageCount,
            chapters = chapters,
            onJumpToPage = { page1Based ->
                currentPageIndex = (page1Based - 1).coerceIn(0, pageCount - 1)
                showJumpDialog = false
            },
            onDismiss = { showJumpDialog = false }
        )
    }
}

@Composable
private fun PdfJumpDialog(
    currentPage: Int,
    totalPages: Int,
    chapters: List<PdfChapterJump>,
    onJumpToPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pageInput by remember(currentPage) { mutableStateOf(currentPage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to Page or Chapter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Page (1–$totalPages)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val target = pageInput.toIntOrNull()?.coerceIn(1, totalPages) ?: currentPage
                            onJumpToPage(target)
                        },
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Go")
                    }
                }

                if (chapters.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Syllabus Chapters & Quick Jumps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chapters) { chapter ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (chapter.pageNumber == currentPage) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onJumpToPage(chapter.pageNumber) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "p.${chapter.pageNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
