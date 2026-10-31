package com.jarvis.fileserver.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.model.ServerState
import com.jarvis.fileserver.ui.theme.AmberWarn
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.EmeraldLive
import com.jarvis.fileserver.ui.theme.LightBorder
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightSurfaceElevated
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
import com.jarvis.fileserver.ui.theme.RoseError
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900

@Composable
fun HeroServerCard(
    serverState: ServerState,
    uptimeSeconds: Long,
    onToggleServer: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val isRunning = serverState is ServerState.Running
    val isStarting = serverState is ServerState.Starting
    val cardBg = if (isDarkTheme) Slate900 else LightSurface
    val cardBorder = if (isRunning) EmeraldLive.copy(alpha = 0.5f) else (if (isDarkTheme) Slate700 else LightBorder)
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(20.dp)
            ),
        color = cardBg
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Status Badge & Port Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator with Pulse
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusDot(isRunning = isRunning, isStarting = isStarting)

                    val statusText = when (serverState) {
                        is ServerState.Running -> "SYSTEM ONLINE"
                        is ServerState.Starting -> "STARTING..."
                        is ServerState.Idle -> "SERVER OFFLINE"
                        is ServerState.Error -> "SYSTEM FAULT"
                    }
                    val statusColor = when (serverState) {
                        is ServerState.Running -> EmeraldLive
                        is ServerState.Starting -> AmberWarn
                        is ServerState.Idle -> Slate400
                        is ServerState.Error -> RoseError
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusColor
                    )
                }

                // Port / Root Chip
                if (isRunning) {
                    val port = (serverState as ServerState.Running).port
                    Box(
                        modifier = Modifier
                            .background(if (isDarkTheme) Slate800 else LightSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, CyanNeon.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PORT $port",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isDarkTheme) CyanNeon else Color(0xFF0284C7)
                        )
                    }
                }
            }

            // Middle: Large Uptime / Ready State
            Column {
                Text(
                    text = if (isRunning) "ACTIVE RUNTIME" else "SERVER STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRunning) formatUptime(uptimeSeconds) else "Ready to initialize",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = if (isRunning) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isRunning) textPrimary else textSecondary
                )
            }

            // Bottom: Prominent Start / Stop Button
            Button(
                onClick = onToggleServer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) RoseError.copy(alpha = 0.15f) else CyanNeon,
                    contentColor = if (isRunning) RoseError else Slate900
                ),
                border = if (isRunning) androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.6f)) else null
            ) {
                if (isStarting) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Slate900,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isStarting -> "Starting Server Engine..."
                        isRunning -> "Stop Server Service"
                        else -> "Start HTTP File Server"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun StatusDot(isRunning: Boolean, isStarting: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val color = when {
        isRunning -> EmeraldLive
        isStarting -> AmberWarn
        else -> Slate400
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(16.dp)
    ) {
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .background(color.copy(alpha = alpha), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
    }
}

private fun formatUptime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}
