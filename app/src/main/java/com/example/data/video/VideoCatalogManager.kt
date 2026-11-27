package com.example.data.video

import android.content.Context
import com.example.data.local.DownloadStatus
import com.example.data.local.VideoProgressDao
import com.example.data.local.VideoProgressEntity
import com.example.data.syllabus.SyllabusDatabaseBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class VideoCatalogAudit(
    val configuredCount: Int,
    val availableCount: Int,
    val downloadedCount: Int,
    val duplicateFilenames: List<String>,
    val missingOrUnavailable: List<String>,
    val totalCachedBytes: Long
)

class VideoCatalogManager(
    context: Context,
    private val videoDao: VideoProgressDao
) {
    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "offline_videos").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun seedAndAuditCatalog(
        bundle: SyllabusDatabaseBundle,
        performRemoteCheck: Boolean
    ): VideoCatalogAudit = withContext(Dispatchers.IO) {
        val existingMap = videoDao.getAllOnce().associateBy { it.videoId }
        val seenFilenames = mutableSetOf<String>()
        val duplicates = mutableListOf<String>()
        val missingOrUnavailable = mutableListOf<String>()
        val updatedList = mutableListOf<VideoProgressEntity>()

        for (topic in bundle.topics) {
            val fname = topic.videoFilename.trim()
            if (fname.isEmpty()) continue
            if (!seenFilenames.add(fname.lowercase())) {
                duplicates.add(fname)
                continue
            }

            val videoId = "vid_${topic.id}"
            val remoteUrl = topic.videoUrl
            val prev = existingMap[videoId]
            val localFile = File(cacheDir, fname)
            val validLocal = localFile.exists() && localFile.length() > 1024L

            var available = validLocal || (prev?.catalogAvailable == true)
            var sizeBytes = if (validLocal) localFile.length() else (prev?.fileSizeBytes ?: 0L)
            var errorNote = prev?.lastError.orEmpty()
            var checkedAt = prev?.catalogCheckedAt ?: 0L

            if (performRemoteCheck && remoteUrl.isNotBlank()) {
                val probe = probeRemoteVideo(remoteUrl)
                available = probe.available || validLocal
                if (probe.sizeBytes > 0L) sizeBytes = probe.sizeBytes
                errorNote = if (probe.available || validLocal) "" else probe.reason
                checkedAt = System.currentTimeMillis()
            }

            if (!available) {
                missingOrUnavailable.add("${topic.name} ($fname)")
            }

            val resolvedStatus = when {
                validLocal -> DownloadStatus.DOWNLOADED.name
                prev?.resolvedDownloadStatus() == DownloadStatus.PAUSED -> DownloadStatus.PAUSED.name
                else -> DownloadStatus.NOT_DOWNLOADED.name
            }

            updatedList.add(
                VideoProgressEntity(
                    videoId = videoId,
                    filename = fname,
                    title = topic.name,
                    topicId = topic.id,
                    partCode = topic.partCode,
                    unitNumber = topic.unit,
                    remoteUrl = remoteUrl,
                    localFilePath = if (validLocal) localFile.absolutePath else "",
                    downloadStatus = resolvedStatus,
                    downloadProgress = if (validLocal) 100 else (prev?.downloadProgress ?: 0),
                    fileSizeBytes = sizeBytes,
                    lastPositionMs = prev?.lastPositionMs ?: 0L,
                    durationMs = prev?.durationMs ?: 0L,
                    watchedPercent = prev?.watchedPercent ?: 0,
                    completed = prev?.completed ?: false,
                    catalogAvailable = available,
                    catalogCheckedAt = checkedAt,
                    lastError = errorNote,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        videoDao.upsertAll(updatedList)

        val downloadedCount = updatedList.count { it.hasLocalFile }
        val totalCachedBytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

        VideoCatalogAudit(
            configuredCount = updatedList.size,
            availableCount = updatedList.count { it.catalogAvailable },
            downloadedCount = downloadedCount,
            duplicateFilenames = duplicates,
            missingOrUnavailable = missingOrUnavailable,
            totalCachedBytes = totalCachedBytes
        )
    }

    private data class RemoteProbeResult(
        val available: Boolean,
        val sizeBytes: Long,
        val reason: String
    )

    private fun probeRemoteVideo(url: String): RemoteProbeResult = try {
        val req = Request.Builder().url(url).head().build()
        client.newCall(req).execute().use { resp ->
            val len = resp.header("Content-Length")?.toLongOrNull() ?: 0L
            when {
                !resp.isSuccessful -> RemoteProbeResult(false, 0L, "HTTP ${resp.code} — unavailable")
                len in 0L..1024L && resp.header("Content-Length") != null ->
                    RemoteProbeResult(false, len, "Video file empty (0 KB) in repository")
                else -> RemoteProbeResult(true, len, "")
            }
        }
    } catch (e: Exception) {
        RemoteProbeResult(false, 0L, "No Internet / unreachable")
    }

    fun startOrResumeDownload(video: VideoProgressEntity) {
        if (downloadJobs[video.videoId]?.isActive == true) return
        val job = scope.launch {
            val targetFile = File(cacheDir, video.filename)
            val partFile = File(cacheDir, "${video.filename}.part")
            try {
                val existingBytes = if (partFile.exists()) partFile.length() else 0L
                val reqBuilder = Request.Builder().url(video.remoteUrl)
                if (existingBytes > 0L) {
                    reqBuilder.addHeader("Range", "bytes=$existingBytes-")
                }

                videoDao.upsert(
                    video.copy(
                        downloadStatus = DownloadStatus.DOWNLOADING.name,
                        lastError = "",
                        updatedAt = System.currentTimeMillis()
                    )
                )

                client.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        videoDao.upsert(
                            video.copy(
                                downloadStatus = DownloadStatus.FAILED.name,
                                catalogAvailable = false,
                                lastError = "Video unavailable (HTTP ${resp.code})",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        return@launch
                    }

                    val body = resp.body ?: error("Empty response stream")
                    val appendMode = resp.code == 206 && existingBytes > 0L
                    val contentLength = body.contentLength()
                    val totalBytes = if (appendMode && contentLength > 0) existingBytes + contentLength else contentLength

                    if (totalBytes in 0L..1024L && totalBytes != -1L) {
                        partFile.delete()
                        videoDao.upsert(
                            video.copy(
                                downloadStatus = DownloadStatus.FAILED.name,
                                catalogAvailable = false,
                                lastError = "Repository file is empty (0 bytes)",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        return@launch
                    }

                    var written = if (appendMode) existingBytes else 0L
                    val buffer = ByteArray(16 * 1024)
                    var lastDbUpdate = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(partFile, appendMode).use { output ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                written += read
                                val now = System.currentTimeMillis()
                                if (now - lastDbUpdate > 350L) {
                                    lastDbUpdate = now
                                    val pct = if (totalBytes > 0L) {
                                        ((written * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                    } else 50
                                    videoDao.upsert(
                                        video.copy(
                                            downloadStatus = DownloadStatus.DOWNLOADING.name,
                                            downloadProgress = pct,
                                            fileSizeBytes = written,
                                            updatedAt = now
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (partFile.length() <= 1024L) {
                        partFile.delete()
                        videoDao.upsert(
                            video.copy(
                                downloadStatus = DownloadStatus.FAILED.name,
                                catalogAvailable = false,
                                lastError = "Video file unavailable (0 KB)",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        if (targetFile.exists()) targetFile.delete()
                        partFile.renameTo(targetFile)
                        videoDao.upsert(
                            video.copy(
                                localFilePath = targetFile.absolutePath,
                                downloadStatus = DownloadStatus.DOWNLOADED.name,
                                downloadProgress = 100,
                                fileSizeBytes = targetFile.length(),
                                catalogAvailable = true,
                                lastError = "",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } catch (_: CancellationException) {
                val latest = videoDao.getById(video.videoId) ?: video
                if (latest.resolvedDownloadStatus() == DownloadStatus.DOWNLOADING) {
                    videoDao.upsert(
                        latest.copy(
                            downloadStatus = DownloadStatus.PAUSED.name,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                videoDao.upsert(
                    video.copy(
                        downloadStatus = DownloadStatus.FAILED.name,
                        lastError = e.message ?: "Download failed",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } finally {
                downloadJobs.remove(video.videoId)
            }
        }
        downloadJobs[video.videoId] = job
    }

    suspend fun pauseDownload(video: VideoProgressEntity) {
        downloadJobs[video.videoId]?.cancel()
        downloadJobs.remove(video.videoId)
        videoDao.upsert(
            video.copy(
                downloadStatus = DownloadStatus.PAUSED.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun cancelDownload(video: VideoProgressEntity) {
        downloadJobs[video.videoId]?.cancel()
        downloadJobs.remove(video.videoId)
        File(cacheDir, "${video.filename}.part").delete()
        videoDao.upsert(
            video.copy(
                downloadStatus = DownloadStatus.NOT_DOWNLOADED.name,
                downloadProgress = 0,
                lastError = "",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCachedVideo(video: VideoProgressEntity) {
        downloadJobs[video.videoId]?.cancel()
        downloadJobs.remove(video.videoId)
        File(cacheDir, video.filename).delete()
        File(cacheDir, "${video.filename}.part").delete()
        videoDao.upsert(
            video.copy(
                localFilePath = "",
                downloadStatus = DownloadStatus.NOT_DOWNLOADED.name,
                downloadProgress = 0,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun getTotalStorageUsedBytes(): Long =
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    suspend fun clearAllCachedVideos() = withContext(Dispatchers.IO) {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        cacheDir.listFiles()?.forEach { it.delete() }
        val all = videoDao.getAllOnce()
        videoDao.upsertAll(
            all.map {
                it.copy(
                    localFilePath = "",
                    downloadStatus = DownloadStatus.NOT_DOWNLOADED.name,
                    downloadProgress = 0
                )
            }
        )
    }
}
