/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package com.example.ttsdemo

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech.LANG_AVAILABLE
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.speech.tts.TextToSpeechService
import android.util.Log
import kotlinx.coroutines.runBlocking

class TtsService : TextToSpeechService() {
    private val TAG = "TtsService"
//    private val _timesource = TimeSource.Monotonic

    override fun onCreate() {
        Log.i(TAG, "onCreate: Hear2Read TTS Service")
        super.onCreate()

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
            if (voice.iso3 != lang) {
                continue
            }

            Log.i(TAG, "Known lang $lang called inside onLoadLanguage")

            // TODO: remove hardcoded hi
            Synthesizer.getOrLoadModel(application, voice)
            return LANG_AVAILABLE
        }

        return LANG_NOT_SUPPORTED
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.i(TAG, "onSynthesizeText: Hear2Read TTS Service")

        // TODO: implement parameters such as rate, amplitude from SyntesisRequest
        val text = request.charSequenceText.toString()
        val language = request.language
        val country = request.country
        val variant = request.variant

        val ret = onIsLanguageAvailable(language, country, variant)
        if (ret == LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }

        // TODO: fix sample rate
        callback.start(22050, AudioFormat.ENCODING_PCM_16BIT, 1)

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
            Synthesizer.speak(text, voices[0], 50, 100, application, ttsCallback)?.join()
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
            if (voice.iso3 == lang) {
                Log.i(TAG, "Known language $lang returned.")
                return LANG_AVAILABLE
            }
        }

        return LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        // TODO: remove hardcoded hi
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