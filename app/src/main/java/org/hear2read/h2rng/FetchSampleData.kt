/*
 * This file contains logic for fetching sample text for each language.
 * GetSampleText is the activity that fetches the sample text from 'res/values/strings.xml' file.
 * This XML file contains sample text for Indic languages under the <string/> tag with attribute "name" in the format "<iso3_language_code>_sample".
 *
 * Functions:
 * getSampleTextData(lang: String) : String
 *  This is the main helper function that fetches the sample text
 *
 * onCreate(savedInstanceState: Bundle?)
 *  This is the function called when the activity is created. It internally calls getSampleTextData() to fetch the sample text.
 */

package org.hear2read.h2rng

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class GetSampleText : Activity() {
    private val TAG = "FetchSampleText"

    private fun getSampleTextData(lang: String) : String {
        val iso3lang = Locale(lang).isO3Language
        val resourceName = "${iso3lang}_sample"
        Log.d(TAG, "Resource name: $resourceName")
        val resId = this.resources.getIdentifier(resourceName, "string", this.packageName)

        val sampleText = if (resId != 0) this.getString(resId) else "Some random sample text."
        Log.d(TAG, sampleText)
        return sampleText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var result = TextToSpeech.LANG_AVAILABLE

        val request = intent
        val lang = request.getStringExtra("language")

        Log.d(TAG, "Language: $lang")
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