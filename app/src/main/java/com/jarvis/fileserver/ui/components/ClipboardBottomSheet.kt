package com.jarvis.fileserver.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.model.ClipboardItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardBottomSheet(
    items: List<ClipboardItem>,
    onSendText: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    val sheetBg = if (isDarkTheme) Slate900 else LightSurface
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary
    val accentColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7)
    val cardBorder = if (isDarkTheme) Slate800 else LightBorder

    fun copyToSystemClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Wormhole Clip", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun pasteFromSystemClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotBlank()) {
                inputText = text
                Toast.makeText(context, "Pasted from phone clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Phone clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Phone clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Shared Clipboard",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                        Text(
                            text = "Instant text sync with website",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }

                if (items.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearAll,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 11.sp, color = textSecondary)
                    }
                }
            }

            // Input / Editing Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp)),
                color = if (isDarkTheme) Slate800.copy(alpha = 0.5f) else LightSurfaceElevated
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SEND TEXT TO WEBSITE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = textSecondary
                        )

                        OutlinedButton(
                            onClick = { pasteFromSystemClipboard() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPasteGo,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = accentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste from Phone", fontSize = 11.sp, color = accentColor)
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Paste or type text to share with connected devices...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = accentColor
                        )
                    )

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendText(inputText)
                                inputText = ""
                                Toast.makeText(context, "Sent to website", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = if (isDarkTheme) Slate800 else Color(0xFFE2E8F0)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (inputText.isNotBlank()) (if (isDarkTheme) Slate900 else Color.White) else textSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send to Web",
                            fontWeight = FontWeight.Bold,
                            color = if (inputText.isNotBlank()) (if (isDarkTheme) Slate900 else Color.White) else textSecondary
                        )
                    }
                }
            }

            // Recent Clips Header
            Text(
                text = "SHARED CLIPS (${items.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = textSecondary
            )

            if (items.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                    color = if (isDarkTheme) Slate800.copy(alpha = 0.3f) else LightSurfaceElevated
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No clipboard items yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                        Text(
                            text = "Text sent from the web or from your phone will appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

                items.forEach { item ->
                    val isFromWeb = item.sender == "web"
                    val badgeColor = if (isFromWeb) CyanNeon else EmeraldLive
                    val senderLabel = if (isFromWeb) "FROM WEB" else "FROM PHONE"
                    val senderIcon = if (isFromWeb) Icons.Default.Public else Icons.Default.PhoneAndroid

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                        color = if (isDarkTheme) Slate800.copy(alpha = 0.6f) else LightSurfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top metadata row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = senderIcon,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = senderLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = badgeColor
                                            )
                                        }
                                    }

                                    Text(
                                        text = timeFormat.format(Date(item.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSecondary
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { copyToSystemClipboard(item.text) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteItem(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete text",
                                            tint = textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Text Content
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = if (isDarkTheme) Slate900.copy(alpha = 0.7f) else Color(0xFFF1F5F9)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        ),
                                        color = textPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
