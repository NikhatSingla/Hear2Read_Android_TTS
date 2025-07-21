package com.example.ttsdemo

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStates
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.ktx.requestFetch
import com.google.android.play.core.ktx.requestRemovePack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

var manager: AssetPackManager? = null

enum class DownloadStatus {
    DOWNLOADED,
    DOWNLOADING,
    NOT_DOWNLOADED,
    CORRUPTED
}

data class Voice (
    val name: String, // e.g. Hindi
    val id: String, // e.g. hi-tdilv2mono-...
    val status: MutableState<DownloadStatus>,
    val iso3: String,
    var size: Long = 0,
    var assetPackState: AssetPackState? = null,
)

val voices = listOf(
    Voice("Hindi", "hi-tdilv2mono-1", mutableStateOf(DownloadStatus.NOT_DOWNLOADED), "hin"),
    Voice("Punjabi", "pa-tdif-1", mutableStateOf(DownloadStatus.NOT_DOWNLOADED), "pan"),
    Voice("English", "en-mono-us", mutableStateOf(DownloadStatus.DOWNLOADED), "eng"),
    Voice("Tamil", "ta-tdif-1", mutableStateOf(DownloadStatus.CORRUPTED), "tam"),
    Voice("Marathi", "ma-tdif-1", mutableStateOf(DownloadStatus.DOWNLOADING), "mar"),
    Voice("Assamese", "as-tdif-1", mutableStateOf(DownloadStatus.CORRUPTED), "asa"),
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
//            checkStatus()
            deleteVoice(voiceItem)
        }
    }
    else if (status == DownloadStatus.NOT_DOWNLOADED) {
        displayText = "Download"
        displayIcon = Icons.Default.Download
        onClickAction = { voiceItem ->
//            checkStatus()
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
//            checkStatus()
            deleteVoice(voiceItem)
            installVoice(voiceItem)
        }
    }

    Row (
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
//        Column (
//            modifier = Modifier
//                .padding(16.dp),
//            horizontalAlignment = Alignment.Start,
//            verticalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(
//                text = voice.name,
//                modifier = Modifier.fillMaxWidth(),
//                fontSize = MaterialTheme.typography.titleMedium.fontSize
//            )
//
//            if (status == DownloadStatus.DOWNLOADING) {
//                Spacer()
//                LinearProgressIndicator()
//            }
//        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp), // Adjusted padding
//                .fillMaxWidth(), // Make the column take available width in the Row
            horizontalAlignment = Alignment.Start
            // Removed Arrangement.SpaceBetween as we'll manually space for the progress bar
        ) {
            Text(
                text = voice.name,
//                 modifier = Modifier.fillMaxWidth(), // Not needed if column fills width
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                maxLines = 1 // Optional: if voice names can be very long
            )

            if (status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp)) // Add vertical space

                LinearProgressIndicator(
                    // progress = { currentProgress }, // Uncomment when you have determinate progress
                    modifier = Modifier
                        .fillMaxWidth(0.6f) // Make it take 80% of the column's width
                        // OR
                        // .widthIn(max = 200.dp) // Constrain to a max width
//                        .height(6.dp) // Optional: Make it a bit thicker if desired
                )
                // Optional: Show percentage text
                // Spacer(modifier = Modifier.height(4.dp))
                // Text(
                //    text = "${(currentProgress * 100).toInt()}% Downloading",
                //    style = MaterialTheme.typography.bodySmall,
                //    color = MaterialTheme.colorScheme.onSurfaceVariant
                // )
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