package org.hear2read.h2rng

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class GetSampleText : Activity() {
    private val LOG_TAG = "H2RNG_" + GetSampleText::class.simpleName

    private fun getSampleTextData(lang: String) : String {
        val iso3lang = Locale(lang).isO3Language
        val resourceName = "${iso3lang}_sample"
        Log.d(LOG_TAG, "Resource name: $resourceName")
        val resId = this.resources.getIdentifier(resourceName, "string", this.packageName)

        val sampleText = if (resId != 0) this.getString(resId) else "Some random sample text."
        Log.d(LOG_TAG, sampleText)
        return sampleText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(LOG_TAG, "onCreate")
        super.onCreate(savedInstanceState)
        var result = TextToSpeech.LANG_AVAILABLE

        val request = intent
        val lang = request.getStringExtra("language")

        Log.d(LOG_TAG, "Language: $lang")
//        val country = request.getStringExtra("country")
//        val variant = request.getStringExtra("variant")

        val text: String = getSampleTextData(lang ?: "")
        if (text.isEmpty()) {
            result = TextToSpeech.LANG_NOT_SUPPORTED
        }

        val intent = Intent().apply {
            if (result == TextToSpeech.LANG_AVAILABLE) {
                putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, text)
            } else {
                putExtra("sampleText", text)
            }
        }

        setResult(result, intent)
        finish()
    }
}