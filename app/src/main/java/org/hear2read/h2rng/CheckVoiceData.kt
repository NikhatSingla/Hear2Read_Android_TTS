/*
 * This file handles the logic of checking the available voices in the TTS menu under Android Talkback.
 * CheckVoiceData is the activity that handles this task.
 * If any "Voice" is present in "voices" and it is downloaded, it will be shown in the menu.
 * Refer to InstallVoices.kt for more details on "Voice" and "voices".
 */

package org.hear2read.h2rng

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

class CheckVoiceData : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val availableVoices: ArrayList<String> = ArrayList()

        for (voice in voices) {
            if (voice.status.value == DownloadStatus.DOWNLOADED) {
                availableVoices.add(voice.iso3)
            }
        }

        val intent = Intent().apply {
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, availableVoices)
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, arrayListOf())
        }
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, intent)
        finish()
    }
}