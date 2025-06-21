package com.example.ttsdemo

import ai.onnxruntime.*
import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.io.FileOutputStream
import java.lang.Float.max
import java.nio.FloatBuffer
import java.nio.LongBuffer

// TODO: frame skips observed on app startup. Optimise for multi-threading
val env = OrtEnvironment.getEnvironment()!!

const val MAX_WAV_VALUE: Short = Short.MAX_VALUE

fun speak(
    text: String,
    selectedLanguage: Language,
    rate: Int,
    amplitude: Int,
    context: Context
) {
    val modelFile = langToFile[selectedLanguage]!!.let { File(context.filesDir, it) }
    val session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())

    val phonemeIdsArr = longArrayOf(1, 0, 26, 0, 59, 0, 25, 0, 120, 0, 102, 0, 31, 0, 32, 0, 18, 0, 122, 0, 3, 0, 15, 0, 130, 0, 120, 0, 14, 0, 122, 0, 92, 0, 59, 0, 32, 0, 2)
    val phonemeIds = LongBuffer.wrap(phonemeIdsArr)
    val phonemeIdsShape = longArrayOf(1, phonemeIdsArr.size.toLong())

    val phonemeIdsLengthsArr = longArrayOf(phonemeIdsArr.size.toLong())
    val phonemeIdsLengths = LongBuffer.wrap(phonemeIdsLengthsArr)
    val phonemeIdLengthsShape = longArrayOf(phonemeIdsLengthsArr.size.toLong())

    // TODO: even verify if it is the correct way to control speed
    // TODO: safely handle phonemeLenScale values out of bounds
    var phonemeLenScale: Float = (1.0f / (((rate.toFloat() - 50.0f) / 25.0f) + 1.0f));
    if (rate < 50) {
        phonemeLenScale = (1.0f / (((rate.toFloat()) / 75.0f) + (1.0f / 3.0f)))
    }

    val scalesArr = floatArrayOf(0.677f, phonemeLenScale, 0.8f)
    val scales = FloatBuffer.wrap(scalesArr)
    val scalesShape = longArrayOf(scalesArr.size.toLong())

    val ortInputs = mutableMapOf(
        "input" to OnnxTensor.createTensor(env, phonemeIds, phonemeIdsShape),
        "input_lengths" to OnnxTensor.createTensor(env, phonemeIdsLengths, phonemeIdLengthsShape),
        "scales" to OnnxTensor.createTensor(env, scales, scalesShape)
    )

    try {
//        var audioTime = 1.0f
        session.run(ortInputs).use { results ->
            val outputTensor = results.get(0) as OnnxTensor // Since output map will have only one node which is called "output"
            val outputShape : LongArray = outputTensor.info.shape as LongArray
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
            val audioScale = MAX_WAV_VALUE * (amplitude.toFloat() / 100.0f) / max(0.01f, maxAudioValue) // 0.01f to avoid division by zero
            val shortArray = ShortArray(audioCount)
            for (i in 0 until audioCount) {
                val scaled = outputArr[i] * audioScale

                shortArray[i] = scaled.coerceIn(
                    Short.MIN_VALUE.toFloat(),
                    Short.MAX_VALUE.toFloat()
                ).toInt().toShort()
            }

            playAudio(shortArray, 22500)
        }
    } catch (e: Exception) {
        println("Exception ${e.message} has occurred.")
        e.printStackTrace()
    }
}