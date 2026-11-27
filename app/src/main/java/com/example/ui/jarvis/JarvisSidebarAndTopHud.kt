package com.example.ui.jarvis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.OmJarvisSettings
import com.example.domain.AnalyticsSnapshot
import com.example.ui.theme.JarvisBlueBorder
import com.example.ui.theme.JarvisCardInnerDark
import com.example.ui.theme.JarvisCardNavy
import com.example.ui.theme.JarvisCoralRed
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisNeonGreen
import com.example.ui.theme.JarvisSidebarSurface
import com.example.ui.theme.JarvisSkyBlue
import com.example.ui.theme.JarvisTextCyan
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun JarvisSidebarPanel(
    currentRoute: String,
    meritModeEnabled: Boolean,
    onSelectRoute: (String) -> Unit,
    onToggleMeritMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarItems = remember { JarvisNavDest.entries.filter { it.inSidebar } }

    Surface(
        color = JarvisSidebarSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Glowing Robot Halo + Branding
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.sweepGradient(listOf(JarvisCyanGlow, JarvisElectricBlue, JarvisCyanGlow)), CircleShape)
                    .background(JarvisCardInnerDark),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_jarvis_robot),
                    contentDescription = "OM Jarvis AI Mascot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "OM Jarvis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = JarvisTextPrimary
            )
            Text(
                text = "ICMA M3 Smart Portal",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextCyan
            )
            Spacer(Modifier.height(10.dp))

            // 14 Menu Items matching exact screenshot list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                sidebarItems.forEach { dest ->
                    val isSelected = currentRoute == dest.route
                    val pillModifier = if (isSelected) {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(JarvisElectricBlue, JarvisSkyBlue)))
                            .border(1.dp, JarvisCyanGlow.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    }

                    Row(
                        modifier = pillModifier
                            .heightIn(min = 42.dp)
                            .clickable { onSelectRoute(dest.route) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("sidebar_${dest.route}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            tint = if (isSelected) Color.White else JarvisTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else JarvisTextPrimary.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bottom Certificate of Merit Mode Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = JarvisCardNavy,
                border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Certificate of Merit",
                            tint = JarvisGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Certificate\nof Merit Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = JarvisGold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (meritModeEnabled) "80%+ Target" else "70% Pass",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisTextSecondary
                        )
                        Switch(
                            checked = meritModeEnabled,
                            onCheckedChange = onToggleMeritMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = JarvisNeonGreen
                            ),
                            modifier = Modifier.testTag("sidebar_merit_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JarvisTopHudHeader(
    isCompactPhone: Boolean,
    settings: OmJarvisSettings,
    analytics: AnalyticsSnapshot,
    unreadAlertsCount: Int,
    onOpenDrawer: () -> Unit,
    onClickReadiness: () -> Unit,
    onClickNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTheme: () -> Unit
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isCompactPhone) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(JarvisCardNavy)
                    .border(1.dp, JarvisBlueBorder, CircleShape)
                    .testTag("open_sidebar_drawer_btn")
            ) {
                Icon(Icons.Rounded.Menu, contentDescription = "Open Menu", tint = JarvisCyanGlow)
            }
        }

        // Grouped 4-Cell Top HUD Stats Bar (Exam Date | Days Left | Study Streak | Readiness Score)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JarvisCardNavy.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, JarvisBlueBorder.copy(alpha = 0.85f)),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HudStatCell(
                    icon = Icons.Rounded.CalendarToday,
                    iconTint = JarvisTextCyan,
                    label = "Exam Date",
                    value = "7 Dec 2026",
                    valueColor = JarvisGold
                )
                HudVerticalDivider()
                HudStatCell(
                    icon = Icons.Rounded.Schedule,
                    iconTint = JarvisTextSecondary,
                    label = "Days Left",
                    value = "${analytics.daysRemaining}",
                    valueColor = Color.White
                )
                HudVerticalDivider()
                HudStatCell(
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = Color(0xFFFF7A18),
                    label = "Study Streak",
                    value = "${analytics.streakDays} Days",
                    valueColor = JarvisGold
                )
                HudVerticalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clickable(onClick = onClickReadiness)
                        .testTag("top_readiness_btn")
                ) {
                    MasteryRing(
                        percent = analytics.readiness.readinessScore,
                        size = 36.dp,
                        strokeWidth = 4.dp,
                        accentColor = JarvisNeonGreen,
                        centerLabel = "↗"
                    )
                    Column {
                        Text("Readiness Score", style = MaterialTheme.typography.labelSmall, color = JarvisTextSecondary)
                        Text(
                            text = "${analytics.readiness.readinessScore}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = JarvisNeonGreen
                        )
                    }
                }
            }
        }

        // Notification Bell with Red Counter Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(JarvisCardNavy)
                .border(1.dp, JarvisBlueBorder, CircleShape)
                .clickable(onClick = onClickNotifications)
                .testTag("top_notifications_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = "Daily Alerts", tint = JarvisTextPrimary)
            if (unreadAlertsCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(JarvisCoralRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadAlertsCount.coerceAtMost(9).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Saifullah Profile Avatar Pill + Menu
        Box {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = JarvisCardNavy,
                border = BorderStroke(1.dp, JarvisBlueBorder),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable { profileMenuExpanded = true }
                    .testTag("top_profile_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_saifullah_avatar),
                        contentDescription = "Saifullah Qureshi",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, JarvisCyanGlow, CircleShape)
                    )
                    if (!isCompactPhone) {
                        Text(
                            text = settings.studentName.substringBefore(' '),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTextPrimary
                        )
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = JarvisTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(
                expanded = profileMenuExpanded,
                onDismissRequest = { profileMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Global Search") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    onClick = { profileMenuExpanded = false; onOpenSearch() }
                )
                DropdownMenuItem(
                    text = { Text(if (settings.darkThemeEnabled) "Light Theme" else "Dark Jarvis HUD") },
                    leadingIcon = { Icon(if (settings.darkThemeEnabled) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = null) },
                    onClick = { profileMenuExpanded = false; onToggleTheme() }
                )
                DropdownMenuItem(
                    text = { Text("Settings & Backup") },
                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    onClick = { profileMenuExpanded = false; onOpenSettings() }
                )
            }
        }
    }
}

@Composable
private fun HudStatCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = JarvisTextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
private fun HudVerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(JarvisBlueBorder.copy(alpha = 0.55f))
    )
}
