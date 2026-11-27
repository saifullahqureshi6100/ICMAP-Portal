package com.example.ui.jarvis

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.OmJarvisSettings
import com.example.data.video.VideoCatalogAudit
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisTextCyan
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlin.math.roundToInt

@Composable
fun OmJarvisOnboardingScreen(
    settings: OmJarvisSettings,
    daysRemaining: Int,
    catalogAudit: VideoCatalogAudit?,
    isOnline: Boolean,
    onRefreshVideoCatalog: () -> Unit,
    onCompleteOnboarding: (meritMode: Boolean, darkTheme: Boolean, dailyMinutes: Int, notifications: Boolean) -> Unit
) {
    var meritMode by remember { mutableStateOf(settings.meritModeEnabled) }
    var darkTheme by remember { mutableStateOf(settings.darkThemeEnabled) }
    var dailyMinutes by remember { mutableIntStateOf(settings.dailyTargetMinutes) }
    var notificationsEnabled by remember { mutableStateOf(settings.notificationsEnabled) }

    val freeStorageMb = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024L)
        }.getOrDefault(0L)
    }

    JarvisSpaceBackdrop(darkMode = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_jarvis_robot),
                            contentDescription = "OM Jarvis Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    Brush.sweepGradient(listOf(JarvisCyanGlow, JarvisElectricBlue, JarvisCyanGlow)),
                                    CircleShape
                                )
                        )
                        Column {
                            Text(
                                text = "OM JARVIS",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = JarvisTextPrimary
                            )
                            Text(
                                text = "ICMA M3 Smart Portal",
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisTextCyan
                            )
                        }
                    }
                    OnlineStatusPill(isOnline = isOnline)
                }

                JarvisGlassCard(glowCyan = true) {
                    Text(
                        text = "Assalam-o-Alaikum, ${settings.studentName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = JarvisTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Exam Target: 7 December 2026 ($daysRemaining days left) • Goal: CERTIFICATE OF MERIT",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisGold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "100% Offline-First architecture: Full Syllabus (Part A 35%, B 20%, C 25%, D 20%), 254-page M3 Textbook PDF, Past Papers, SM-2 Flashcards, Adaptive MCQs & Male Mentor Voice ready without login or API keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )
                }

                JarvisGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = JarvisCyanGlow)
                        Text("System & Video Catalog Readiness", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Storage, contentDescription = null, tint = JarvisTextSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Available Device Storage: ${freeStorageMb} MB (Offline video cache ready)",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextPrimary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val auditText = if (catalogAudit != null) {
                        "Configured: ${catalogAudit.configuredCount} videos • Playable/Available: ${catalogAudit.availableCount} • Cached Offline: ${catalogAudit.downloadedCount}"
                    } else {
                        "Manifest ready (28 mapped lectures). Tap audit if online."
                    }
                    Text(auditText, style = MaterialTheme.typography.bodySmall, color = JarvisTextSecondary)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRefreshVideoCatalog,
                        enabled = isOnline,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(if (isOnline) "Probe GitHub Video Catalog Now" else "Offline — Probe Skipped Safely")
                    }
                }

                JarvisGlassCard {
                    Text("Configure Your Merit Study Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Certificate of Merit Target (80%+)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = JarvisTextPrimary)
                            Text("Requires 80%+ MCQ & full topic mastery", style = MaterialTheme.typography.bodySmall, color = JarvisTextSecondary)
                        }
                        Switch(
                            checked = meritMode,
                            onCheckedChange = { meritMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = JarvisNeonGreen)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Jarvis Cyber-Navy HUD Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = JarvisTextPrimary)
                            Text("Toggle off for High-Contrast Daylight mode", style = MaterialTheme.typography.bodySmall, color = JarvisTextSecondary)
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { darkTheme = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = JarvisNeonGreen)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WorkManager Routine Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = JarvisTextPrimary)
                            Text("08:00 Morning Brief, 12:45 Lunch Test, 19:00 Study, 20:15 Night Drill", style = MaterialTheme.typography.bodySmall, color = JarvisTextSecondary)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = JarvisNeonGreen)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Daily Study Target: $dailyMinutes minutes (${String.format("%.1f", dailyMinutes / 60f)} hrs)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisGold
                    )
                    Slider(
                        value = dailyMinutes.toFloat(),
                        onValueChange = { dailyMinutes = (it / 15f).roundToInt() * 15 },
                        valueRange = 60f..360f
                    )
                }

                JarvisGradientButton(
                    text = "Launch OM Jarvis Command Center",
                    icon = Icons.Rounded.RocketLaunch,
                    tone = JarvisButtonTone.BLUE,
                    onClick = { onCompleteOnboarding(meritMode, darkTheme, dailyMinutes, notificationsEnabled) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("launch_jarvis_wizard_btn")
                )
            }
        }
    }
}
