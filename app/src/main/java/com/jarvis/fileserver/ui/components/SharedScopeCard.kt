package com.jarvis.fileserver.ui.components

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.io.File

@Composable
fun SharedScopeCard(
    currentRootPath: String,
    onSelectPath: (String) -> Unit,
    onPickFolder: () -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val cardBg = if (isDarkTheme) Slate900 else LightSurface
    val cardBorder = if (isDarkTheme) Slate700 else LightBorder
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary

    val defaultRoot = Environment.getExternalStorageDirectory().absolutePath
    val targetFile = File(currentRootPath)
    val isSingleFile = targetFile.exists() && targetFile.isFile
    val isDefaultAll = currentRootPath.trimEnd('/') == defaultRoot.trimEnd('/')

    val scopeTitle = when {
        isSingleFile -> "Specific File: ${targetFile.name}"
        isDefaultAll -> "Entire Internal Storage"
        else -> "Folder: ${targetFile.name}"
    }

    val scopeSubtitle = when {
        isSingleFile -> "${formatSize(targetFile.length())} • ${targetFile.absolutePath}"
        isDefaultAll -> "All phone files and folders are accessible"
        else -> targetFile.absolutePath
    }

    val icon = when {
        isSingleFile -> Icons.Default.Description
        isDefaultAll -> Icons.Default.PhoneAndroid
        else -> Icons.Default.FolderSpecial
    }

    val badgeColor = when {
        isSingleFile -> EmeraldLive
        isDefaultAll -> CyanNeon
        else -> Color(0xFFFBBF24) // Amber
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
        color = cardBg
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHARED SCOPE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = textSecondary
                )

                if (!isDefaultAll) {
                    OutlinedButton(
                        onClick = { onSelectPath(defaultRoot) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CyanNeon
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset All",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = CyanNeon
                        )
                    }
                }
            }

            // Current Shared Item Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, if (isDarkTheme) Slate800 else LightBorder, RoundedCornerShape(12.dp)),
                color = if (isDarkTheme) Slate800.copy(alpha = 0.5f) else LightSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scopeTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = scopeSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Folder & File Picker Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickFolder,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Slate800 else LightSurfaceElevated,
                        contentColor = textPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = CyanNeon
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pick Folder",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = onPickFile,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Slate800 else LightSurfaceElevated,
                        contentColor = textPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = EmeraldLive
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pick File",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Quick Preset Folder Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "All Storage" to defaultRoot,
                    "Downloads" to "$defaultRoot/Download",
                    "DCIM / Photos" to "$defaultRoot/DCIM",
                    "Documents" to "$defaultRoot/Documents",
                    "Movies" to "$defaultRoot/Movies",
                    "Music" to "$defaultRoot/Music"
                )

                presets.forEach { (label, path) ->
                    val isSelected = currentRootPath.trimEnd('/') == path.trimEnd('/')
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPath(path) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                            selectedLabelColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7),
                            containerColor = if (isDarkTheme) Slate800 else LightSurfaceElevated,
                            labelColor = textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isDarkTheme) Slate700 else LightBorder,
                            selectedBorderColor = CyanNeon
                        )
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
