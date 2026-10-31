package com.jarvis.fileserver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.R
import com.jarvis.fileserver.model.TunnelState
import com.jarvis.fileserver.ui.theme.AmberWarn
import com.jarvis.fileserver.ui.theme.CloudflareOrange
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.EmeraldLive
import com.jarvis.fileserver.ui.theme.LightBorder
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightSurfaceElevated
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
import com.jarvis.fileserver.ui.theme.RoseError
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate600
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900

@Composable
fun TunnelControlsCard(
    tunnelState: TunnelState,
    isServerRunning: Boolean,
    onToggleTunnel: (Boolean) -> Unit,
    onCopyUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val isTunnelEnabled = tunnelState is TunnelState.Connected || tunnelState is TunnelState.Connecting
    val cardBg = if (isDarkTheme) Slate900 else LightSurface
    val cardBorder = if (tunnelState is TunnelState.Connected) {
        CloudflareOrange.copy(alpha = 0.6f)
    } else {
        if (isDarkTheme) Slate700 else LightBorder
    }
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
        color = cardBg
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(CloudflareOrange.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cloudflare),
                            contentDescription = "Cloudflare",
                            tint = CloudflareOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Cloudflare Quick Tunnel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                        Text(
                            text = "One-tap global HTTPS link",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary
                        )
                    }
                }

                Switch(
                    checked = isTunnelEnabled,
                    onCheckedChange = { enabled ->
                        onToggleTunnel(enabled)
                    },
                    enabled = isServerRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CloudflareOrange,
                        uncheckedThumbColor = if (isDarkTheme) Slate400 else Slate600,
                        uncheckedTrackColor = if (isDarkTheme) Slate800 else LightBorder
                    )
                )
            }

            // Status Bar & Output
            when (tunnelState) {
                is TunnelState.Connecting -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDarkTheme) Slate800.copy(alpha = 0.5f) else LightSurfaceElevated,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CloudflareOrange,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Negotiating secure edge tunnel...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CloudflareOrange
                        )
                    }
                }
                is TunnelState.Connected -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDarkTheme) Slate800.copy(alpha = 0.6f) else LightSurfaceElevated,
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, CloudflareOrange.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EmeraldLive, CircleShape)
                                )
                                Text(
                                    text = "GLOBAL ACCESS ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = EmeraldLive
                                )
                            }
                            IconButton(
                                onClick = { onCopyUrl(tunnelState.url) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy URL",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = tunnelState.url,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isDarkTheme) CyanNeon else Color(0xFF0284C7)
                        )
                    }
                }
                is TunnelState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RoseError.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .border(1.dp, RoseError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Tunnel Error: ${tunnelState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoseError
                        )
                    }
                }
                is TunnelState.Disabled -> {
                    Text(
                        text = if (!isServerRunning) "Start server to enable public tunnel." else "Ready to create secure tunnel via trycloudflare.com without port forwarding.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}
