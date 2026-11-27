package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.local.DownloadStatus
import com.example.data.local.VideoProgressEntity
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Media3VideoPlayerCard(
    video: VideoProgressEntity?,
    isOnline: Boolean,
    onProgressTick: (positionMs: Long, durationMs: Long) -> Unit,
    onMarkComplete: () -> Unit,
    onStartOrResumeDownload: (VideoProgressEntity) -> Unit,
    onPauseDownload: (VideoProgressEntity) -> Unit,
    onCancelDownload: (VideoProgressEntity) -> Unit,
    onDeleteCached: (VideoProgressEntity) -> Unit,
    onRefreshCatalog: () -> Unit,
    onPreviousTopic: () -> Unit,
    onNextTopic: () -> Unit,
    onAddQuickNote: (String) -> Unit,
    onAskJarvisAboutVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFullscreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playerErrorText by remember(video?.videoId) { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showNoteBox by remember { mutableStateOf(false) }
    var quickNoteInput by remember { mutableStateOf("") }

    if (video == null) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("No video mapped for this topic.", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = onRefreshCatalog) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh Video Catalog")
                }
            }
        }
        return
    }

    val playableUri: Uri? = remember(video.videoId, video.localFilePath, video.downloadStatus, video.catalogAvailable, isOnline) {
        when {
            video.hasLocalFile && File(video.localFilePath).exists() -> Uri.fromFile(File(video.localFilePath))
            video.catalogAvailable && isOnline && video.remoteUrl.isNotBlank() -> Uri.parse(video.remoteUrl)
            else -> null
        }
    }

    val exoPlayer = remember(video.videoId, playableUri) {
        if (playableUri == null) null
        else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playableUri))
            if (video.lastPositionMs > 1500L) {
                seekTo(video.lastPositionMs)
            }
            setPlaybackSpeed(playbackSpeed)
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlayerError(error: PlaybackException) {
                    playerErrorText = error.localizedMessage ?: "Video stream unavailable (${video.filename})"
                    isPlaying = false
                }
            }
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                    exoPlayer.pause()
                    onProgressTick(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
                }
            }
            exoPlayer.addListener(listener)
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            onDispose {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                if (pos > 0L) onProgressTick(pos, dur)
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
    }

    // Track watched progress every 2 seconds while playing
    LaunchedEffect(exoPlayer, isPlaying) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (isPlaying) {
            delay(2000L)
            onProgressTick(player.currentPosition, player.duration.coerceAtLeast(0L))
        }
    }

    val playerSurface: @Composable (Boolean) -> Unit = { fullscreenMode ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fullscreenMode) Modifier.fillMaxSize()
                    else Modifier.aspectRatio(16f / 9f)
                )
                .clip(RoundedCornerShape(if (fullscreenMode) 0.dp else 16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null && playerErrorText == null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player = exoPlayer
                        }
                    },
                    update = { pv ->
                        if (pv.player !== exoPlayer) pv.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { isFullscreen = !isFullscreen },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(48.dp)
                        .testTag("video_fullscreen_toggle")
                ) {
                    Icon(
                        imageVector = if (fullscreenMode) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        contentDescription = if (fullscreenMode) "Exit Fullscreen" else "Enter Fullscreen",
                        tint = Color.White
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Video unavailable",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "Video unavailable right now",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = playerErrorText ?: video.lastError.ifBlank {
                            if (!isOnline && !video.hasLocalFile) {
                                "Offline and not downloaded yet. Connect to download or study the full reading notes below."
                            } else {
                                "MP4 file (${video.filename}) is missing or empty (0 KB) in GitHub catalog."
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    OutlinedButton(
                        onClick = {
                            playerErrorText = null
                            onRefreshCatalog()
                        },
                        modifier = Modifier.testTag("video_retry_catalog_btn")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Check Catalog Again")
                    }
                }
            }
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                playerSurface(true)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${video.filename} • Watched ${video.watchedPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        video.hasLocalFile -> MaterialTheme.colorScheme.primaryContainer
                        video.catalogAvailable -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                video.hasLocalFile -> Icons.Rounded.DownloadDone
                                video.completed -> Icons.Rounded.CheckCircle
                                else -> Icons.Rounded.CloudDownload
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when {
                                video.hasLocalFile -> "Offline Ready"
                                video.catalogAvailable -> "Online MP4"
                                else -> "Unavailable"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Video Player Viewport
            playerSurface(false)

            // Playback controls (-10s, Play/Pause, +10s, Speed cycle)
            if (exoPlayer != null && playerErrorText == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier.size(48.dp).testTag("video_rewind_10s")
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10 seconds")
                    }
                    FilledTonalButton(
                        onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("video_play_pause_btn")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPlaying) "Pause" else if (video.lastPositionMs > 1500L) "Resume" else "Play")
                    }
                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                        modifier = Modifier.size(48.dp).testTag("video_forward_10s")
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10 seconds")
                    }
                    OutlinedButton(
                        onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                2.0f -> 0.75f
                                else -> 1.0f
                            }
                            exoPlayer.setPlaybackSpeed(playbackSpeed)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("video_speed_btn")
                    ) {
                        Text("${playbackSpeed}x", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Offline Download Progress / Actions
            val dlStatus = video.resolvedDownloadStatus()
            if (dlStatus == DownloadStatus.DOWNLOADING || dlStatus == DownloadStatus.PAUSED) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { (video.downloadProgress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${dlStatus.label}: ${video.downloadProgress}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkComplete,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("video_mark_complete_btn")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (video.completed) "Completed ✓" else "Mark Complete")
                }

                when (dlStatus) {
                    DownloadStatus.DOWNLOADED -> {
                        OutlinedButton(
                            onClick = { onDeleteCached(video) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("video_delete_cache_btn")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete Offline")
                        }
                    }
                    DownloadStatus.DOWNLOADING -> {
                        OutlinedButton(
                            onClick = { onPauseDownload(video) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pause DL")
                        }
                        OutlinedButton(
                            onClick = { onCancelDownload(video) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        FilledTonalButton(
                            onClick = { onStartOrResumeDownload(video) },
                            enabled = isOnline,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Resume DL (${video.downloadProgress}%)")
                        }
                        OutlinedButton(
                            onClick = { onCancelDownload(video) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = { onStartOrResumeDownload(video) },
                            enabled = isOnline && video.catalogAvailable,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("video_download_offline_btn")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (video.catalogAvailable) "Download Offline" else "Not on GitHub")
                        }
                    }
                }

                AssistChip(
                    onClick = { showNoteBox = !showNoteBox },
                    label = { Text("Add Note") },
                    leadingIcon = { Icon(Icons.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                AssistChip(
                    onClick = onAskJarvisAboutVideo,
                    label = { Text("Ask Jarvis") },
                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("video_ask_jarvis_btn")
                )
            }

            if (showNoteBox) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickNoteInput,
                        onValueChange = { quickNoteInput = it },
                        label = { Text("Timestamp / key takeaway note") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (quickNoteInput.isNotBlank()) {
                                onAddQuickNote(quickNoteInput.trim())
                                quickNoteInput = ""
                                showNoteBox = false
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Save")
                    }
                }
            }

            // Prev / Next Topic navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onPreviousTopic,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("video_prev_topic")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                    Text("Prev Topic")
                }
                OutlinedButton(
                    onClick = onNextTopic,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("video_next_topic")
                ) {
                    Text("Next Topic")
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
