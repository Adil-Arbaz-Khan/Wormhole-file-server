package com.jarvis.fileserver.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900
import com.jarvis.fileserver.util.QrCodeGenerator

@Composable
fun QrCodeDialog(
    title: String,
    url: String,
    onCopyUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(url) {
        QrCodeGenerator.generateQrCodeBitmap(
            content = url,
            width = 600,
            height = 600,
            darkColor = android.graphics.Color.BLACK,
            lightColor = android.graphics.Color.WHITE
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    text = "Scan with your phone, tablet, or PC to connect instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    textAlign = TextAlign.Center
                )

                // White Container for QR Code Bitmap
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for $url",
                            modifier = Modifier.size(216.dp)
                        )
                    } else {
                        Text(
                            text = "Failed to render QR Code",
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // URL Display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Slate800
                ) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = CyanNeon,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                    ) {
                        Text("Close", color = Slate400)
                    }

                    Button(
                        onClick = {
                            onCopyUrl(url)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Slate900,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Copy", color = Slate900, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
