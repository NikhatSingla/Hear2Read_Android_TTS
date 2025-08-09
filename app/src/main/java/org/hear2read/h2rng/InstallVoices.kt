/*
 * This file defines ONLY the UI layout for Voice Manager screen.
 * All the business logic is present in InstallVoicesLogic.kt
 *
 * Important Variables/Classes:
 * Voice: Data Class representing a voice's properties like name, download status, iso3 code, etc
 * voices: List of "Voice" rows displayed in the "Voice Manager" screen. This list needs to be updated if a new language is added or removed.
 *
 * Functions:
 *
 * @Composable
 * VoiceItem(voice: Voice):
 *  Renders a row per voice item in the "Voice Manager" screen.
 *
 * @Composable
 * InstallVoicesScreen():
 *  Main function that renders the "Voice Manager" screen.
 *  Calls VoiceItem to render a row for each voice present in the "voices" list
 */

package org.hear2read.h2rng

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.play.core.assetpacks.AssetPackManager

var manager: AssetPackManager? = null

enum class DownloadStatus {
    DOWNLOADED,
    DOWNLOADING,
    NOT_DOWNLOADED,
    CORRUPTED
}

data class Voice(
    val name: String, // e.g. Hindi
    val id: String, // e.g. hi-tdilv2mono-... TODO: is this required?
    val status: MutableState<DownloadStatus>,
    val iso3: String,
    val isMultiSpeaker: Boolean = false, //TODO: get this from json instead of hardcoding?
    val sampleRate: Int = 16000,
    var size: Long = 0,
    val downloadProgress: MutableState<Float> = mutableFloatStateOf(0.0f),
)

//TODO populate this dynamically?
val voices = listOf(
    Voice("Hindi", "hi-tdilv2mono-1", mutableStateOf(DownloadStatus.NOT_DOWNLOADED), "hin", sampleRate = 22050),
//    Voice("Punjabi", "pa-tdif-1", mutableStateOf(DownloadStatus.NOT_DOWNLOADED), "pan"),
    Voice(
        "Kannada",
        "kn-v2-tdilh2radditional-1987val-low",
        mutableStateOf(DownloadStatus.NOT_DOWNLOADED),
        "kan",
        isMultiSpeaker = true
    ),
    Voice("English", "en-mono-us", mutableStateOf(DownloadStatus.NOT_DOWNLOADED), "eng",),
//    Voice("Tamil", "ta-tdif-1", mutableStateOf(DownloadStatus.CORRUPTED), "tam"),
//    Voice("Marathi", "ma-tdif-1", mutableStateOf(DownloadStatus.DOWNLOADING), "mar"),
//    Voice("Assamese", "as-tdif-1", mutableStateOf(DownloadStatus.CORRUPTED), "asm"),
)

@Composable
fun VoiceItem(voice: Voice) {
    val status by voice.status
    val interactionSource = remember { MutableInteractionSource() }

    val displayText: String
    val displayIcon: ImageVector
    val onClickAction: (Voice) -> Unit
    var enabled = true

    if (status == DownloadStatus.DOWNLOADED) {
        displayText = "Delete"
        displayIcon = Icons.Default.Delete
        onClickAction = { voiceItem ->
            deleteVoice(voiceItem)
        }
    }
    else if (status == DownloadStatus.NOT_DOWNLOADED) {
        displayText = "Download"
        displayIcon = Icons.Default.Download
        onClickAction = { voiceItem ->
            installVoice(voiceItem)
        }
    }
    else if (status == DownloadStatus.DOWNLOADING) {
        enabled = false
        displayText = "Downloading"
        displayIcon = Icons.Default.Download
        onClickAction = { }
    }
    else {
        displayIcon = Icons.Default.Refresh
        displayText = "Retry"
        onClickAction = { voiceItem ->
            deleteVoice(voiceItem)
            installVoice(voiceItem)
        }
    }

    Row (
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Text(
                text = voice.name,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                maxLines = 1 // Optional: if voice names can be very long
            )

            if (status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp)) // Add vertical space

                LinearProgressIndicator(
                     progress = { voice.downloadProgress.value },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                )
            }
        }

        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%.1f", (voice.size / 1e6)) + " MB",
                modifier = Modifier.padding(8.dp),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            )

            Icon(
                displayIcon,
                contentDescription = displayText,
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = ripple()
                    ) {
                        onClickAction(voice)
                    }
                    .padding(top = 16.dp, start = 0.dp, bottom = 16.dp, end = 16.dp),
                tint = if (enabled)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
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