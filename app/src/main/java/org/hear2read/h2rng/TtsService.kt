/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*
* Description:
* This file contains the implementation of the custom TextToSpeechService.
* TtsService is the name of the actual class that inherits it.
*
* Functions:
* override fun onCreate()
*   Called when an object of the class is created.
*   Calls populateSizes() (see InstallVoicesLogic.kt) and Synthesizer.initeSpeak (see Infer.kt)
*
* override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int
*   Called by Android Talkback to query if a language is available.
*   Returns LANG_AVAILABLE if the language is properly downloded and LANG_NOT_SUPPORTED otherwise
*
* override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int
*   This function gets called by the Android Talkback atleast once before the actual request is received for TTS
*   The function calls Synthesizer.getOrLoadModel (see Infer.kt) on the requested language if it is downloaded and return LANG_AVAILABLE
*   Returns LANG_NOT_SUPPORTED otherwise
*
* override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback)
*   Queries onIsLanguageAvaiable to check for language availability
*   If language is not available, returns error
*   Otherwise calls Synthesizer.speak (see Infer.kt) if text is not empty
*   Note that Synthesizer.speak uses ShortArray internally but 'callback' (parameter) requires a ByteArray
*   So the ShortArray is converted into ByteArray using shortArrayToByteArray (see below) and then fed into 'callback'
*
* override fun onDestroy()
*   Runs super.onDestroy() of the base class.
*
* override fun onGetLanguage(): Array<String>
*   Legacy function, not used. Meant for very old Android versions. To be removed/modified in the next app update.
*
* override fun onStop()
*   To be updated on next app release. Currently doesn't perform any task.
*
* private fun shortArrayToByteArray(audio: ShortArray): ByteArray
*   Converts a given short array (parameter 'audio') to a byte array using Little-Endian format.
*   New ByteArray is twice the length of the ShortArray (since 1 Short requires 2 Bytes)
*/

package org.hear2read.h2rng

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech.LANG_AVAILABLE
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.speech.tts.TextToSpeechService
import android.util.Log
import androidx.compose.runtime.getValue
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import kotlinx.coroutines.runBlocking

class TtsService : TextToSpeechService() {
    private val TAG = "TtsService"
//    private val _timesource = TimeSource.Monotonic

    override fun onCreate() {
        Log.i(TAG, "onCreate: Hear2Read TTS Service")
        super.onCreate()

        manager = AssetPackManagerFactory.getInstance(application)
        populateSizes()

        Synthesizer.initeSpeak(copyDataDir(this, "espeak-ng-data"))
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
//        Log.i(TAG, "onLoadLanguage: Hear2Read TTS Service")
//
//        CoroutineScope(Dispatchers.IO).launch {
//            Synthesizer.getOrLoadModel(application, Language.valueOf(lang!!))
//
//        }

        // TODO: check properly what documentation meant by onLoadLanguage not ALWAYS called
        Log.i(TAG, "onLoadCalled: $lang, $country, $variant")

        for (voice in voices) {
            val status by voice.status

            if (voice.iso3 == lang && status == DownloadStatus.DOWNLOADED) {
                Log.i(TAG, "Known lang $lang called inside onLoadLanguage")

                // TODO: remove hardcoded hi
                Synthesizer.getOrLoadModel(application, voice)

                return LANG_AVAILABLE
            }
        }

        return LANG_NOT_SUPPORTED
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.i(TAG, "onSynthesizeText: Hear2Read TTS Service")

        // TODO: implement parameters such as rate, amplitude from SyntesisRequest
        val text = request.charSequenceText.toString()
        val rate = request.speechRate
        val language = request.language
        val country = request.country
        val variant = request.variant

        val ret = onIsLanguageAvailable(language, country, variant)

        if (ret == LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }

        var foundVoice: Voice? = null

        for (voice in voices) {
            if (voice.iso3 == language) {
                foundVoice = voice
                break
            }
        }

        if (foundVoice == null) {
            return
        }

        Log.d(TAG, "Synthesis Voice found ${foundVoice.iso3}")

        // TODO: fix sample rate
        callback.start(foundVoice.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        if (text.isBlank() || text.isEmpty()) {
            callback.done()
            return
        }

        val ttsCallback: (ShortArray) -> Int = fun(shortArray: ShortArray): Int {
            // convert FloatArray to ByteArray
            val samples = shortArrayToByteArray(shortArray)
            val maxBufferSize: Int = callback.maxBufferSize
            var offset = 0
            while (offset < samples.size) {
                val bytesToWrite = maxBufferSize.coerceAtMost(samples.size - offset)
                callback.audioAvailable(samples, offset, bytesToWrite)
                offset += bytesToWrite
            }

            // 1 means to continue
            // 0 means to stop
            return 1
        }

        runBlocking {
            Synthesizer.speak(text, foundVoice, 50, 100, application, ttsCallback)?.join()
        }

        callback.done()
    }

    override fun onDestroy() {
        // TODO: provide way to close model in Synthesizer
        Log.i(TAG, "onDestroy: Hear2Read TTS Service")
        super.onDestroy()
    }

     override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        Log.i(TAG, "onIsLangAvailable: Hear2Read TTS Service: $lang, $country, $variant")

        // TODO: provide a mapping and check from it
        for (voice in voices) {
            if (voice.iso3 == lang && voice.status.value == DownloadStatus.DOWNLOADED) {
                Log.i(TAG, "Known language $lang returned.")
                return LANG_AVAILABLE
            }
        }

        return LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        // TODO: remove hardcoded hi
        Log.i(TAG, "onGetLanguage")
        return arrayOf("hin", "", "")
    }

    override fun onStop() {
        Log.i(TAG, "onStop: Hear2Read TTS Service")
//        Synthesizer.termeSpeak()
        // TODO: implement close model feature in Synthesizer
    }

    // TODO: temporary implementation avoid so many conversions
    private fun shortArrayToByteArray(audio: ShortArray): ByteArray {
        // byteArray is actually a ShortArray
        val byteArray = ByteArray(audio.size * 2)
        for (i in audio.indices) {
            val sample = audio[i].toInt()
            byteArray[2 * i] = sample.toByte()
            byteArray[2 * i + 1] = (sample shr 8).toByte()
        }
        return byteArray
    }
}