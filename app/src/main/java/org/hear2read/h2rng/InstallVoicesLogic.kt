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

        for (voice in h2RVoices) {
            if (voice.locale.isO3Language in voicePackList) {
                if (manager!!.getPackLocation(voice.locale.isO3Language) != null) {
                    voice.status.value = DownloadStatus.DOWNLOADED

                    val assetPackLocation = manager!!.getPackLocation(voice.locale.isO3Language)
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

                val assetPackState = assetPackStates.packStates()[voice.locale.isO3Language]
                voice.size = assetPackState!!.totalBytesToDownload()
//                voice.assetPackState = assetPackState
                Log.i(LOG_TAG, "${voice.locale.isO3Language}: ${assetPackState.totalBytesToDownload()}")
            }
        }
    }
}

fun installVoice(h2RVoice: H2RVoice) {
    CoroutineScope(Dispatchers.IO).launch {
        manager!!.clearListeners()
        manager!!.registerListener(StatePackUpdateListener)
        manager!!.requestFetch(listOf(h2RVoice.locale.isO3Language))
    }
}

fun deleteVoice(h2RVoice: H2RVoice) {
    CoroutineScope(Dispatchers.IO).launch {
        manager!!.requestRemovePack(h2RVoice.locale.isO3Language)
        if (manager!!.getPackLocation(h2RVoice.locale.isO3Language) != null) {
            h2RVoice.status.value = DownloadStatus.CORRUPTED
        } else {
            h2RVoice.status.value = DownloadStatus.NOT_DOWNLOADED
        }
    }
}
