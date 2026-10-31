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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.model.NetworkInterfaceInfo
import com.jarvis.fileserver.model.NetworkType
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
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900

@Composable
fun ConnectionAddressesCard(
    localInterfaces: List<NetworkInterfaceInfo>,
    tunnelState: TunnelState,
    onCopyUrl: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowQr: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val cardBg = if (isDarkTheme) Slate900 else LightSurface
    val cardBorder = if (isDarkTheme) Slate700 else LightBorder
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
            Text(
                text = "ACCESS ENDPOINTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = textSecondary
            )

            // Cloudflare Public Tunnel Section
            if (tunnelState is TunnelState.Connected) {
                AddressItemRow(
                    title = "Public Tunnel (Cloudflare)",
                    url = tunnelState.url,
                    icon = Icons.Default.Language,
                    badgeText = "HTTPS PUBLIC",
                    badgeColor = CloudflareOrange,
                    onCopy = { onCopyUrl(tunnelState.url) },
                    onOpen = { onOpenUrl(tunnelState.url) },
                    onQr = { onShowQr("Cloudflare Quick Tunnel", tunnelState.url) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Local Network Interfaces
            if (localInterfaces.isEmpty()) {
                Text(
                    text = "No active network interface detected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )
            } else {
                localInterfaces.forEach { item ->
                    val (icon, badgeText, badgeColor) = when (item.type) {
                        NetworkType.WIFI -> Triple(Icons.Default.Wifi, "WIFI", EmeraldLive)
                        NetworkType.HOTSPOT -> Triple(Icons.Default.WifiTethering, "HOTSPOT", AmberWarn)
                        NetworkType.USB -> Triple(Icons.Default.Usb, "USB", CyanNeon)
                        NetworkType.ETHERNET -> Triple(Icons.Default.Router, "LAN", EmeraldLive)
                        NetworkType.CELLULAR -> Triple(Icons.Default.Language, "CELLULAR", Slate400)
                        NetworkType.LOCALHOST -> Triple(Icons.Default.Language, "LOCAL", Slate400)
                        NetworkType.OTHER -> Triple(Icons.Default.Language, "NET", Slate400)
                    }

                    AddressItemRow(
                        title = item.displayName,
                        url = item.fullUrl,
                        icon = icon,
                        badgeText = badgeText,
                        badgeColor = badgeColor,
                        onCopy = { onCopyUrl(item.fullUrl) },
                        onOpen = { onOpenUrl(item.fullUrl) },
                        onQr = { onShowQr(item.displayName, item.fullUrl) },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressItemRow(
    title: String,
    url: String,
    icon: ImageVector,
    badgeText: String,
    badgeColor: Color,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onQr: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val rowBg = if (isDarkTheme) Slate800.copy(alpha = 0.5f) else LightSurfaceElevated
    val rowBorder = if (isDarkTheme) Slate800 else LightBorder
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, rowBorder, RoundedCornerShape(12.dp)),
        color = rowBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Row 1: Icon, Title, and Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Monospace URL Pill & One-Tap Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (isDarkTheme) Slate900.copy(alpha = 0.7f) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = if (isDarkTheme) CyanNeon else Color(0xFF0284C7),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpen,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in Browser",
                            tint = textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onQr,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Show QR Code",
                            tint = textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
