package com.example.ttsdemo

import android.util.Log
import com.google.android.play.core.ktx.requestFetch
import com.google.android.play.core.ktx.requestPackStates
import com.google.android.play.core.ktx.requestRemovePack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// PLAN
// 1. Iterate through voicePackList and update voice.status based on it
//      This will ensure we get the proper list of languages available
// 2. Register Listeners for every voice
//      This will ensure that the status of the voice is updated consistently, whenever the status changes
// 3. Register

val voicePackList = listOf(
    "hin",
    "pan",
    "eng",
    "asm"
)

fun populateSizes() {
    manager!!.registerListener(StatePackUpdateListener)

    CoroutineScope(Dispatchers.IO).launch {
        val assetPackStates = manager!!.requestPackStates(voicePackList)

        for (voice in voices) {
            if (voice.iso3 in voicePackList) {
                if (manager!!.getPackLocation(voice.iso3) != null) {
                    voice.status.value = DownloadStatus.DOWNLOADED

                    val assetPackLocation = manager!!.getPackLocation(voice.iso3)
                    val assetPackPath = assetPackLocation?.assetsPath()!!
                    val fileName = getFileWithExtension(assetPackPath, "onnx") ?: return@launch

                    val tempParsed = fileName.name.replace("-", ".").split(".")
                    var synthFreq = 16000

                    if (tempParsed.contains("med")) {
                        Log.d("PAD_Test", "This is a medium voice ${voice.iso3}")
                        synthFreq = 22050
                    }

                    voice.sampleRate = synthFreq
                } else {
                    voice.status.value = DownloadStatus.NOT_DOWNLOADED
                }

                val assetPackState = assetPackStates.packStates()[voice.iso3]
                voice.size = assetPackState!!.totalBytesToDownload()
//                voice.assetPackState = assetPackState
                Log.i("PAD_Test", "${voice.iso3}: ${assetPackState.totalBytesToDownload()}")
            }
        }
    }
}

fun installVoice(voice: Voice) {
    CoroutineScope(Dispatchers.IO).launch {
        manager!!.requestFetch(listOf(voice.iso3))
    }
}

fun deleteVoice(voice: Voice) {
    CoroutineScope(Dispatchers.IO).launch {
        manager!!.requestRemovePack(voice.iso3)
        if (manager!!.getPackLocation(voice.iso3) != null) {
            voice.status.value = DownloadStatus.CORRUPTED
        } else {
            voice.status.value = DownloadStatus.NOT_DOWNLOADED
        }
    }
}
