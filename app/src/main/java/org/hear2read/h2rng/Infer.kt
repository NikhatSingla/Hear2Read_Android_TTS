/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package org.hear2read.h2rng

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.speech.tts.TextToSpeech.LANG_AVAILABLE
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Float.max
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

const val MAX_WAV_VALUE: Short = Short.MAX_VALUE

object Synthesizer {
    init {
        System.loadLibrary("phonemizer")
    }

    private val LOG_TAG = "H2RNG_" + Synthesizer::class.simpleName
    private var _loadJob : Job? = null
    private var _session : OrtSession? = null
    private var _speakJob : Job? = null
    private var _curVoice : Voice? = null

    private val _timeSource = TimeSource.Monotonic

    // Marking a function as JvmStatic is really important, otherwise it crashes the app
    @JvmStatic
    external fun phonemizeWithSilence(text: String, jsonConfigPath: String) : String

    @JvmStatic
    external fun initeSpeak(espeakDataPath: String)

    @JvmStatic
    external fun termeSpeak()

    // TODO: handle concurrency
    @JvmStatic
    suspend fun getOrLoadModel(
        context: Context,
        selectedVoice: Voice,
        synchronous: Boolean = true
    ): Int {
        // Skip if language already loaded
        // TODO: this may cause a problem if there's a voice update with a different ID
        if ((_curVoice?.iso3 == selectedVoice.iso3) && (_session != null)) {
            return LANG_AVAILABLE
        }

        if (manager == null) {
            manager = AssetPackManagerFactory.getInstance(context)
        }

        val assetPackLocation = manager!!.getPackLocation(selectedVoice.iso3) ?:  run {
            Log.e(LOG_TAG,"getPackLocation returned null")
            return LANG_NOT_SUPPORTED
        }
        Log.d(LOG_TAG, "AssetPack: " + assetPackLocation.assetsPath().toString())
        val assetPackPath = assetPackLocation.assetsPath() ?: run {
            Log.e(LOG_TAG,"assetsPath returned null")
            return LANG_NOT_SUPPORTED
        }
        val modelFile = getFileWithExtension(assetPackPath, "onnx") ?: run {
            Log.e(LOG_TAG,"getFileWithExtension returned null")
            return LANG_NOT_SUPPORTED
        }

        Log.d(LOG_TAG, "AssetPack: " + modelFile.absolutePath)

        try {
            _loadJob?.cancel()
        } catch (e: Exception) {
            Log.w(LOG_TAG,"Error canceling prev job: ${e.message}")
            e.printStackTrace()
        }

        val loadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                try {
                    _session?.close()
                    _speakJob?.cancel()
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Error closing previous session or coroutine: ${e.message}")
                    e.printStackTrace()
                }

                Log.d(LOG_TAG, "Current thread is ${Thread.currentThread().name}")

                val startGetEnv = _timeSource.markNow()
                // TODO: check if duplicates are being created
                val env = OrtEnvironment.getEnvironment()!!
                val endGetEnv = _timeSource.markNow()
                Log.d(LOG_TAG, "GetEnv took time: ${(endGetEnv - startGetEnv).toDouble(DurationUnit.MILLISECONDS)}")

                val startSession = _timeSource.markNow()
                _session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
                val endSession = _timeSource.markNow()
                Log.d(LOG_TAG, "Session took time: ${(endSession - startSession).toDouble(DurationUnit.MILLISECONDS)}")
                _curVoice = selectedVoice
            } catch (e: Exception) {
                // TODO better fallback
                Log.e(LOG_TAG, "Model loading failed:  ${e.message}")
                e.printStackTrace()
                _curVoice = null
            }
        }

        _loadJob = loadJob
        // We usually call this from onSynthesizeText(), which requires it to be blocking
        if (synchronous)
            _loadJob!!.join()
        return LANG_AVAILABLE
    }

    @JvmStatic
    fun speak(
        text: String,
        selectedVoice: Voice,
        rate: Int,
        amplitude: Int,
        context: Context,
        callback: ((shortArray : ShortArray) -> Int)?
    ) : Job? {
        if (_loadJob == null)
            return null

        Log.d(LOG_TAG, "Running synthesizer")

        try {
            _speakJob?.cancel()
            Log.d(LOG_TAG, "Previous speaking stopped.")
        } catch (e : Exception) {
            Log.w(LOG_TAG, "Error closing previous session or coroutine: ${e.message}")
            e.printStackTrace()
        }

        val speakJob = CoroutineScope(Dispatchers.Default).launch {


//            _speakJob = null;
//            _loadJob!!.join()
            while (! _loadJob!!.isCompleted) {
                // TODO: to track max time limit
            }

            // TODO: check if modelFile exists, else prompt for download
            val assetPackLocation = manager!!.getPackLocation(selectedVoice.iso3) ?: return@launch
            val assetPackPath = assetPackLocation.assetsPath() ?: return@launch
            val jsonConfigFile = getFileWithExtension(assetPackPath, "json") ?: return@launch
//            val jsonConfigFile = File(context.filesDir, langToFile[selectedLanguage]!! + ".json")

            //            Log.d(LOG_TAG, "Model file is ${modelFile.isFile()}")
            Log.d(LOG_TAG,"Config file is ${jsonConfigFile.isFile()}")
            //            Log.d(LOG_TAG, isDirectoryPathValid(context.filesDir.absolutePath))

            val startEspeak = _timeSource.markNow()
            val outputTextToPhonemes = phonemizeWithSilence(text, jsonConfigFile.absolutePath)
            val endEspeak = _timeSource.markNow()
            Log.d(LOG_TAG, "Espeak took time: ${(endEspeak - startEspeak).toDouble(DurationUnit.MILLISECONDS)}")

            // TODO: frame skips observed on app startup. Optimise for multi-threading
            val startGetEnv = _timeSource.markNow()
            val env = OrtEnvironment.getEnvironment()!!
            val endGetEnv = _timeSource.markNow()
            Log.d(LOG_TAG, "GetEnv took time: ${(endGetEnv - startGetEnv).toDouble(DurationUnit.MILLISECONDS)}")

            Log.d(LOG_TAG, "outputTextToPhonemes: $outputTextToPhonemes")

            val startRep = _timeSource.markNow()
            val phonemeIdsArr = outputTextToPhonemes
                //                .replace(";", ",")
                //                .replace(" ", ",")
                //                .replace("\t", ",")
                //                .replace("\n", ",") // normalise text first into a common delimiter
                .split(",")
                .filter { it.isNotBlank() } // filter out empty tokens
                .map { it.toLong() }
                .toLongArray()
            val endRep = _timeSource.markNow()
            Log.d(LOG_TAG, "Replacement took time: ${(endRep - startRep).toDouble(DurationUnit.MILLISECONDS)}")

            //            Log.d(LOG_TAG, phonemeIdsArr.toString())

            val startInputPrep = _timeSource.markNow()
            val phonemeIds = LongBuffer.wrap(phonemeIdsArr)
            val phonemeIdsShape = longArrayOf(1, phonemeIdsArr.size.toLong())

            val phonemeIdsLengthsArr = longArrayOf(phonemeIdsArr.size.toLong())
            val phonemeIdsLengths = LongBuffer.wrap(phonemeIdsLengthsArr)
            val phonemeIdLengthsShape = longArrayOf(phonemeIdsLengthsArr.size.toLong())

            // TODO: even verify if it is the correct way to control speed
            // TODO: safely handle phonemeLenScale values out of bounds
            var phonemeLenScale: Float = (1.0f / (((rate.toFloat() - 50.0f) / 25.0f) + 1.0f))
            if (rate < 50) {
                phonemeLenScale = (1.0f / (((rate.toFloat()) / 75.0f) + (1.0f / 3.0f)))
            }

            // TODO: load scalesArr values from JSON
            val scalesArr = floatArrayOf(0.667f, phonemeLenScale, 0.8f)
            val scales = FloatBuffer.wrap(scalesArr)
            val scalesShape = longArrayOf(scalesArr.size.toLong())
            val speakerID = LongBuffer.wrap(longArrayOf(0))
            val speakerIDShape = longArrayOf(1)

            val ortInputs = mutableMapOf(
                "input" to OnnxTensor.createTensor(env, phonemeIds, phonemeIdsShape),
                "input_lengths" to OnnxTensor.createTensor(
                    env,
                    phonemeIdsLengths,
                    phonemeIdLengthsShape
                ),
                "scales" to OnnxTensor.createTensor(env, scales, scalesShape),
            )
            if (selectedVoice.isMultiSpeaker) {
                ortInputs["sid"] = OnnxTensor.createTensor(env, speakerID, speakerIDShape)
            }
            val endInputPrep = _timeSource.markNow()
            Log.d(LOG_TAG, "Input prep took time: ${(endInputPrep - startInputPrep).toDouble(DurationUnit.MILLISECONDS)}")

            try {
                val start = _timeSource.markNow()
                _session?.run(ortInputs)?.use { results ->
                    val end = _timeSource.markNow()
                    val inferenceTimeMillis = (end - start).toDouble(DurationUnit.MILLISECONDS)
                    Log.d(LOG_TAG, "ORT took time: $inferenceTimeMillis ms")

                    val outputTensor =
                        results.get(0) as OnnxTensor // Since output map will have only one node which is called "output"
                    val outputShape: LongArray = outputTensor.info.shape as LongArray
                    val outputBuffer = outputTensor.floatBuffer

                    val audioCount = outputShape[3].toInt()
                    val outputArr = FloatArray(audioCount)
                    outputBuffer.get(outputArr)

                    // Find max value in outputArr
                    var maxAudioValue: Float = Float.NEGATIVE_INFINITY
                    for (i in 0 until audioCount) {
                        val value = outputArr[i]
                        if (value > maxAudioValue) {
                            maxAudioValue = value
                        }
                    }

                    // TODO: discuss if it is the correct and preferred way
                    // TODO: handle amplitude out of bounds
                    val audioScale = MAX_WAV_VALUE * (amplitude.toFloat() / 100.0f) / max(
                        0.01f,
                        maxAudioValue
                    ) // 0.01f to avoid division by zero
                    val shortArray = ShortArray(audioCount)
                    for (i in 0 until audioCount) {
                        val scaled = outputArr[i] * audioScale

                        shortArray[i] = scaled.coerceIn(
                            Short.MIN_VALUE.toFloat(),
                            Short.MAX_VALUE.toFloat()
                        ).toInt().toShort()
                    }

                    val realTimeMillis = audioCount.toDouble() * 1000.0 / 22500.0
                    Log.d(LOG_TAG,"Real time: $realTimeMillis ms")

                    Log.d(LOG_TAG,"Inference ratio: ${inferenceTimeMillis / realTimeMillis}")

                    // TODO: provide playAudio as callback too!
                    if (callback == null) {
                        playAudio(shortArray, selectedVoice.sampleRate)
                    } else {
                        callback(shortArray)
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG,"Exception ${e.message} has occurred.")
                e.printStackTrace()
            }
        }

        _speakJob = speakJob

        return _speakJob
    }
}




