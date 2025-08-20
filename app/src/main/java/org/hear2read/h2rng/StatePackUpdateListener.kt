package org.hear2read.h2rng

import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus

object StatePackUpdateListener : AssetPackStateUpdateListener {

    val LOG_TAG = "H2RNG_" + StatePackUpdateListener::class.simpleName
    override fun onStateUpdate(state: AssetPackState) {
        val name = state.name();
        for (voice in h2RVoices) {
            if (voice.locale.isO3Language == name) {
                val status = state.status()

                Log.d(LOG_TAG, "Voice: ${state.name()}, Status: $status")

                if (status == AssetPackStatus.DOWNLOADING
                    || status == AssetPackStatus.PENDING
                    || status == AssetPackStatus.TRANSFERRING
                ) {
                    voice.downloadProgress.value = (state.transferProgressPercentage() * 1.0f) / 100.0f
                    Log.d(LOG_TAG, "Voice: ${state.name()}, Percentage: ${state.transferProgressPercentage()}")
                    voice.status.value = DownloadStatus.DOWNLOADING
                } else if (status == AssetPackStatus.COMPLETED) {
                    voice.downloadProgress.value = 1.0f
                    voice.status.value = DownloadStatus.DOWNLOADED
                } else if (status == AssetPackStatus.NOT_INSTALLED) {
                    voice.downloadProgress.value = 0.0f
                    voice.status.value = DownloadStatus.NOT_DOWNLOADED
                } else if (status == AssetPackStatus.CANCELED
                    || status == AssetPackStatus.FAILED
                    || status == AssetPackStatus.UNKNOWN
                ) {
                    voice.downloadProgress.value = 0.0f
                    voice.status.value = DownloadStatus.CORRUPTED
                }

                break
            }
        }
    }
}