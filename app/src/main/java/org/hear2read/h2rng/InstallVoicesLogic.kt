/*
 * This file contains the logic for managing the voices.
 *
 * Important Variables/Classes:
 * voicePackList: List of Play Asset Delivery (PAD) voices supported by the app. It must be mandatorily updated if any language is added or removed from the PAD.
 *
 * Functions:
 * populateSizes():
 *  Populates the size of each voice's PlayAssetPack into its Voice object.
 *  Alongside it is also responsible for updating the initial download status of each voice.
 *  Also sets the sample rate of each voice by analyzing the file name.
 *  This function is just called once in the onCreate() of the MainActivity.
 *
 * installVoice(voice: Voice):
 *  Wrapper around the Play Asset Delivery (PAD) manager's requestFetch() function.
 *
 * deleteVoice(voice: Voice):
 *  Wrapper around the Play Asset Delivery (PAD) manager's requestRemovePack() function.
 *  Also checks if the corresponding PlayAssetPack have been removed correctly, otherwise sets the corresponding Voice.status to CORRUPTED.
 */

package org.hear2read.h2rng

import android.util.Log
import com.google.android.play.core.ktx.requestFetch
import com.google.android.play.core.ktx.requestPackStates
import com.google.android.play.core.ktx.requestRemovePack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val voicePackList = listOf(
    "hin",
    "kan",
    "eng",
)

fun populateSizes() {
    val LOG_TAG = "H2RNG_" + (object{}.javaClass.enclosingMethod?.name ?: "")
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
                        //TODO : do this from json?
//                    val tempParsed = fileName.name.replace("-", ".").split(".")
//                    var synthFreq = 16000
//
//                    if (tempParsed.contains("med")) {
//                        Log.d("PAD_Test", "This is a medium voice ${voice.iso3}")
//                        synthFreq = 22050
//                    }
//
//                    voice.sampleRate = synthFreq
                } else {
                    voice.status.value = DownloadStatus.NOT_DOWNLOADED
                }

                val assetPackState = assetPackStates.packStates()[voice.iso3]
                voice.size = assetPackState!!.totalBytesToDownload()
//                voice.assetPackState = assetPackState
                Log.i(LOG_TAG, "${voice.iso3}: ${assetPackState.totalBytesToDownload()}")
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
