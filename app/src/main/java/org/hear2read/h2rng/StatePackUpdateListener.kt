/*
 * This file implements a custom AssetPackStateUpdateListener interface, which helps in monitoring the status of asset packs.
 *
 * StatePackUpdateListener is the class that implements the AssetPackStateUpdateListener interface.
 *
 * fun onStateUpdate(state: AssetPackState)
 *  The onStateUpdate method is called when the status of an asset pack changes.
 *  The "voices" list is traversed to find the corresponding Voice object for the asset pack whose status has changed. The downloadProgress and status values of the Voice object are updated accordingly.
 */

package org.hear2read.h2rng

import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus

object StatePackUpdateListener : AssetPackStateUpdateListener {
    override fun onStateUpdate(state: AssetPackState) {
        val name = state.name();
        for (voice in voices) {
            if (voice.iso3 == name) {
                val status = state.status()

                Log.d("PAD_Test", "Voice: ${state.name()}, Status: $status")

                if (status == AssetPackStatus.DOWNLOADING
                    || status == AssetPackStatus.PENDING
                    || status == AssetPackStatus.TRANSFERRING
                ) {
                    voice.downloadProgress.value = (state.transferProgressPercentage() * 1.0f) / 100.0f
                    Log.d("PAD_Test", "Voice: ${state.name()}, Percentage: ${state.transferProgressPercentage()}")
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