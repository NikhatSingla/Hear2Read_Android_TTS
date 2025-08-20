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
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import androidx.compose.runtime.getValue
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import kotlinx.coroutines.runBlocking
import java.util.Locale

class TtsService : TextToSpeechService() {
    private val LOG_TAG = "H2RNG_" + TtsService::class.simpleName
//    private val _timesource = TimeSource.Monotonic

    private var _curLang: Locale? = null

    // Needs to be updated on new language beyond list
    // TODO: implement reverse for consistent behaviour with apps setting language with ISO2 code
    private val iso3ToIso2Map = mapOf(
        "hin" to "hi", // Hindi
        "kan" to "kn", // Kannada
        "tel" to "te", // Telugu
        "tam" to "ta", // Tamil
        "mal" to "ml", // Malayalam
        "guj" to "gu", // Gujarati
        "pan" to "pa", // Punjabi (Gurmukhi)
        "ori" to "or", // Odia
        "ben" to "bn", // Bengali
        "mar" to "mr", // Marathi
        "asm" to "as", // Assamese
        "sin" to "si", // Sinhala
        "nep" to "ne", // Nepali
        "eng" to "en", // English
    )

    private val iso2ToIso3Map = mapOf(
        "hi" to "hin", // Hindi
        "kn" to "kan", // Kannada
        "te" to "tel", // Telugu
        "ta" to "tam", // Tamil
        "ml" to "mal", // Malayalam
        "gu" to "guj", // Gujarati
        "pa" to "pan", // Punjabi (Gurmukhi)
        "or" to "ori", // Odia
        "bn" to "ben", // Bengali
        "mr" to "mar", // Marathi
        "as" to "asm", // Assamese
        "si" to "sin", // Sinhala
        "ne" to "nep", // Nepali
        "en" to "eng"  // English
    )

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
        Log.d(LOG_TAG, "onLoadLanguage: $lang, $country, $variant")

//        val langISO2 = if (lang?.length==3) iso3ToIso2Map[lang] else lang

        for (voice in h2RVoices) {
            val status by voice.status

            if (voice.locale.isO3Language == lang && status == DownloadStatus.DOWNLOADED) {
                Log.d(LOG_TAG, "Known lang $lang called inside onLoadLanguage")

                // TODO: remove hardcoded hi
                // Removing loading language here, we will only load when called from within
                // onSynthesizeText()
                //return Synthesizer.getOrLoadModel(application, voice)

                return TextToSpeech.LANG_AVAILABLE
            }
        }

        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.d(LOG_TAG, "onSynthesizeText: Hear2Read TTS Service")

        // TODO: implement parameters such as rate, amplitude from SyntesisRequest
        val text = request.charSequenceText.toString()
        val rate = request.speechRate
        val language = request.language
        val langISO2 = if (language.length==3) iso3ToIso2Map[language] else language
        val country = request.country
        val variant = request.variant

        val ret = onIsLanguageAvailable(language, country, variant)

        if (ret == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }

        var foundH2RVoice: H2RVoice? = null

        for (voice in h2RVoices) {
            if (voice.locale.language == langISO2) {
                foundH2RVoice = voice
                break
            }
        }

        if (foundH2RVoice == null) {
            Log.w(LOG_TAG, "No appropriate voice found for synthesis language: $langISO2")
            return
        }

        Log.d(LOG_TAG, "Synthesis Voice found ${foundH2RVoice.locale}")

        var loadVoiceReturn: Int

        runBlocking {
            loadVoiceReturn = Synthesizer.getOrLoadModel(application, foundH2RVoice)
        }

        if (loadVoiceReturn != TextToSpeech.LANG_AVAILABLE)
            return

        if (_curLang?.language != foundH2RVoice.locale.language) {
            Log.d(LOG_TAG, "Updating _curLang: $_curLang to ${foundH2RVoice.locale}")
            _curLang = Locale.Builder().setLanguage(foundH2RVoice.locale.language).build()
        }

        // TODO: fix sample rate
        callback.start(foundH2RVoice.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

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
            Synthesizer.speak(text, foundH2RVoice, 50, 100, application, ttsCallback)?.join()
        }

        callback.done()
    }

    override fun onDestroy() {
        // TODO: provide way to close model and espeak in Synthesizer
        Log.d(LOG_TAG, "onDestroy: Hear2Read TTS Service")
        super.onDestroy()
    }

    override fun onGetVoices(): MutableList<Voice> {
//        for (voice in voices) {
//            if (voice.iso3 == lang) {
//                Log.i(LOG_TAG, "Known language $lang returned: $country, $variant")
//                return LANG_AVAILABLE
//            }
//        }
        val returnVoices = h2RVoices.mapNotNull { h2rVoice ->
             if (h2rVoice.status.value == DownloadStatus.DOWNLOADED) {
              Voice(
                /* name = */ h2rVoice.id,
                /* locale = */ Locale.Builder()
                    .setLanguage(h2rVoice.locale.isO3Language)   //ISO-3 language code
                    //.setRegion("IN")         // ISO country code (optional)
                    .build(),
                /* quality = */ Voice.QUALITY_HIGH,
                /* latency = */ Voice.LATENCY_NORMAL,
                /* requiresNetworkConnection = */ false,
                /* features = */ setOf()
              )
             }
            else null
        }
        Log.d(LOG_TAG, "onGetVoices returning: $returnVoices")
        return returnVoices.toMutableList()
    }

     override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        Log.d(LOG_TAG, "onIsLangAvailable: Hear2Read TTS Service: $lang, $country, $variant")

        // TODO: provide a mapping and check from it

         for (voice in h2RVoices) {
             if (voice.locale.isO3Language == lang){ // onIsLanguageAvailable is guaranteed to have ISO3
                 Log.i(LOG_TAG, "Known language $lang returned: $country, $variant")
                 return TextToSpeech.LANG_AVAILABLE
             }
         }

         return TextToSpeech.LANG_NOT_SUPPORTED
         return if (h2RVoices.any { h2RVoice ->
             h2RVoice.locale.isO3Language == lang
         }) TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        // TODO: remove hardcoded hi
        Log.d(LOG_TAG, "onGetLanguage")
        return arrayOf(_curLang?.isO3Language ?: "", "", "")
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        Log.d(LOG_TAG, "onIsValidVoiceName: $voiceName")
        return if (h2RVoices.any { h2RVoice: H2RVoice -> h2RVoice.id == voiceName }) {
            Log.d(LOG_TAG, "onIsValidVoiceName: $voiceName success")
            TextToSpeech.SUCCESS
        } else {
            TextToSpeech.ERROR
        }
    }

    override fun onGetDefaultVoiceNameFor(
        lang: String?,
        country: String?,
        variant: String?
    ): String {
        val iso3Map = h2RVoices.associateBy { h2RVoice ->
            h2RVoice.locale.isO3Language
        }
        Log.d(LOG_TAG, "onGetDefaultVoiceNameFor $lang returning : " +
                (iso3Map[lang.toString()]?.id ?: "")
        )
        return iso3Map[lang.toString()]?.id ?: ""
    }

    override fun onStop() {
        Log.d(LOG_TAG, "onStop: Hear2Read TTS Service")
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