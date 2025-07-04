/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package com.example.ttsdemo

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

fun playAudio(shortArray: ShortArray, sampleRate: Int = 16000) {
    val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    // TODO: AudioTrack is deprecated.
    val audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC,
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize,
        AudioTrack.MODE_STREAM  // Or MODE_STATIC for one-shot buffers
    )

    audioTrack.play()
    audioTrack.write(shortArray, 0, shortArray.size)
    audioTrack.stop()
    audioTrack.release()
}
