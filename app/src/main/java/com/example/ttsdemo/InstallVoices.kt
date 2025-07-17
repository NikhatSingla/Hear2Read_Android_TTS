package com.example.ttsdemo

import android.graphics.drawable.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class DownloadStatus {
    DOWNLOADED,
    DOWNLOADING,
    NOT_DOWNLOADED,
    CORRUPTED
}

data class voice (
    val name: String, // e.g. Hindi
    val id: String, // e.g. hi-tdilv2mono-...
    val status: DownloadStatus,
)

val voices = listOf(
    voice("Hindi", "hi-tdilv2mono-1", DownloadStatus.DOWNLOADED),
    voice("Punjabi", "pa-tdif-1", DownloadStatus.DOWNLOADING),
    voice("English", "en-mono-us", DownloadStatus.NOT_DOWNLOADED),
    voice("Tamil", "ta-tdif-1", DownloadStatus.CORRUPTED)
)

@Composable
fun VoiceItem(voice: voice) {
    val interactionSource = remember { MutableInteractionSource() }

    val displayText: String
    val displayIcon: ImageVector
    val onClickAction: (voice) -> Unit
    var enabled: Boolean = true;

    if (voice.status == DownloadStatus.DOWNLOADED) {
        displayText = "Delete"
        displayIcon = Icons.Default.Delete
        onClickAction = {}
    } else if (voice.status == DownloadStatus.NOT_DOWNLOADED) {
        displayText = "Download"
        displayIcon = Icons.Default.Download
        onClickAction = {}
    } else if (voice.status == DownloadStatus.DOWNLOADING) {
        enabled = false
        displayText = "Downloading"
        displayIcon = Icons.Default.Download
        onClickAction = {}
    } else {
        displayIcon = Icons.Default.Refresh
        displayText = "Retry"
        onClickAction = {}
    }

    Row (
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = voice.name,
            modifier = Modifier
                .padding(16.dp),

            fontSize = MaterialTheme.typography.titleMedium.fontSize
        )

        Icon(displayIcon,
            contentDescription = displayText,
            modifier = Modifier
                .clickable (
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple()
                ) {
                    onClickAction(voice)
                }
                .padding(16.dp),
            tint = if (enabled)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
    HorizontalDivider()
}

@Composable
fun InstallVoicesScreen() {
    Scaffold (
        topBar = {
            GlobalTopBar("Voice Manager")
        },
        modifier = Modifier.fillMaxWidth()
    ){ innerPadding ->
        LazyColumn (modifier = Modifier.padding(innerPadding)) {
            items(voices) { voiceItem ->
                VoiceItem(voiceItem)
            }
        }
    }
}

@Preview
@Composable
fun InstallVoicesPreview() {
    InstallVoicesScreen()
}