/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
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
    private val LOG_TAG = "H2RNG_" + TtsService::class.simpleName
//    private val _timesource = TimeSource.Monotonic

    override fun onCreate() {
        Log.i(LOG_TAG, "onCreate: Hear2Read TTS Service")
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
        Log.i(LOG_TAG, "onLoadCalled: $lang, $country, $variant")

        for (voice in voices) {
            val status by voice.status

            if (voice.iso3 == lang && status == DownloadStatus.DOWNLOADED) {
                Log.i(LOG_TAG, "Known lang $lang called inside onLoadLanguage")

                // TODO: remove hardcoded hi
                // Removing loading language here, we will only load when called from within
                // onSynthesizeText()
                //return Synthesizer.getOrLoadModel(application, voice)

                return LANG_AVAILABLE
            }
        }

        return LANG_NOT_SUPPORTED
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.i(LOG_TAG, "onSynthesizeText: Hear2Read TTS Service")

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

        Log.d(LOG_TAG, "Synthesis Voice found ${foundVoice.iso3}")

        var loadVoiceReturn: Int

        runBlocking {
            loadVoiceReturn = Synthesizer.getOrLoadModel(application, foundVoice)
        }

        if (loadVoiceReturn != LANG_AVAILABLE)
            return

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
        Log.i(LOG_TAG, "onDestroy: Hear2Read TTS Service")
        super.onDestroy()
    }

     override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
//        Log.i(LOG_TAG, "onIsLangAvailable: Hear2Read TTS Service: $lang, $country, $variant")

        // TODO: provide a mapping and check from it
        for (voice in voices) {
            if (voice.iso3 == lang) {
//                Log.i(LOG_TAG, "Known language $lang returned.")
                return LANG_AVAILABLE
            }
        }

        return LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        // TODO: remove hardcoded hi
        Log.i(LOG_TAG, "onGetLanguage")
        return arrayOf("hin", "", "")
    }

    override fun onStop() {
        Log.i(LOG_TAG, "onStop: Hear2Read TTS Service")
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