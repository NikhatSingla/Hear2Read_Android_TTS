package com.example.ttsdemo

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
                    voice.status.value = DownloadStatus.DOWNLOADING
                } else if (status == AssetPackStatus.COMPLETED) {
                    voice.status.value = DownloadStatus.DOWNLOADED
                } else if (status == AssetPackStatus.NOT_INSTALLED) {
                    voice.status.value = DownloadStatus.NOT_DOWNLOADED
                } else if (status == AssetPackStatus.CANCELED
                    || status == AssetPackStatus.FAILED
                    || status == AssetPackStatus.UNKNOWN
                ) {
                    voice.status.value = DownloadStatus.CORRUPTED
                }

                break;
            }
        }
    }
}