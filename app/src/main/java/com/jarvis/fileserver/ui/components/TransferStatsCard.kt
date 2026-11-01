package com.jarvis.fileserver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.analytics.TrafficSnapshot
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.EmeraldOnline
import com.jarvis.fileserver.ui.theme.LightBorder
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900

@Composable
fun TransferStatsCard(
    snapshot: TrafficSnapshot,
    onViewActivity: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val cardBg = if (isDarkTheme) Slate900 else LightSurface
    val borderCol = if (isDarkTheme) Slate800 else LightBorder
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, borderCol, RoundedCornerShape(20.dp)),
        color = cardBg,
        shadowElevation = if (isDarkTheme) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LIVE BANDWIDTH",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = textSecondary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                // Active Visitors Badge
                Surface(
                    color = EmeraldOnline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = EmeraldOnline,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${snapshot.activeVisitorsCount} active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldOnline,
                            maxLines = 1
                        )
                    }
                }
            }

            // Real-time Upload & Download Speed Gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Upload Speed Box (Host -> Visitors)
                SpeedBox(
                    label = "Outbound (Streaming/Download)",
                    speedBytes = snapshot.uploadSpeedBps,
                    totalBytes = snapshot.totalBytesSent,
                    isUpload = true,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.weight(1f)
                )

                // Download Speed Box (Visitors -> Host Uploads)
                SpeedBox(
                    label = "Inbound (Incoming Uploads)",
                    speedBytes = snapshot.downloadSpeedBps,
                    totalBytes = snapshot.totalBytesReceived,
                    isUpload = false,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.weight(1f)
                )
            }

            // In-flight connection indicator and View Activity Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${snapshot.activeConnections} concurrent connection(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )

                OutlinedButton(
                    onClick = onViewActivity,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanNeon
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Activity Log", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedBox(
    label: String,
    speedBytes: Long,
    totalBytes: Long,
    isUpload: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val boxBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val tintColor = if (isUpload) CyanNeon else Color(0xFF10B981)
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(boxBg)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isUpload) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isUpload) "Upload Speed" else "Download Speed",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = textSecondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatSpeed(speedBytes),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = tintColor
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Total: ${formatBytes(totalBytes)}",
            style = MaterialTheme.typography.labelSmall,
            color = textSecondary
        )
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec < 1024) return "$bytesPerSec B/s"
    val kb = bytesPerSec / 1024.0
    if (kb < 1024) return "%.1f KB/s".format(kb)
    val mb = kb / 1024.0
    return "%.2f MB/s".format(mb)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
