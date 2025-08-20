/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
* Description:
*
* Variables:
* manager: AssetPackManager?
*   Global variable to store the AssetPackManager instance.
*
* Functions:
* @Composable
* GlobalTopBar(text: String)
*   A universal top bar with Hear2Read logo and custom text for the application.
*   This function is used in multiple Kotlin files in this package, for each screen.
*
* @Composable
* LanguageDropdown(...)
*   Composable function to display a dropdown menu for selecting a language.
*   Displays all the languages that are properly downloaded in the "voices" list.
*
* @Composable
* SettingSlider(...)
*   Composable function to display slider (used for speed and volume).
*
* @Composable
* InputScreen()
*   Main composable that renders the whole home (TTS) screen.
*
* MainActivity.onCreate()
*   Main function that runs on application launch.
*   Does the following in the listed order:
*       - Assigns an instance of AssetPackManager to the global variable "manager"
*       - Runs populateVoices() (see InstallVoicesLogic.kt)
*       - Copies the espeak-ng data files using AssetManger into a folder in the files directory
*       - Defines the navigation routes
*       - Renders the screen using InputScreen()
*
* Classes:
* sealed class Screen:
*   Used for navigation. Defines a screen name for each route.
*   If a new screen needs to be added, define its route here.
*   It is not mandatory to have this class, but it is a good practice since it avoids hardcoded routes.
*/

package org.hear2read.h2rng

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import java.util.Locale

@Composable
fun LanguageDropdown(
    availableLangs: List<Locale>,
    isInitialized: Boolean,
    selectedVoice: Locale?,
    onVoiceSelected: (Locale) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedVoice?.displayName ?: "Select Language",
            onValueChange = { /* Read Only */ },
            label = { Text("Language") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = true },
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    "Select Language",
                    Modifier.clickable { isExpanded = true }
                )
            }
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (isInitialized)
                availableLangs.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice.displayName) },
                        onClick = {
                            onVoiceSelected(voice)
                            isExpanded = false
                        }
                    )
                }

        }

    }

}

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label: ${value.toInt()}", style = MaterialTheme.typography.bodyMedium)

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("input")
    data object Settings : Screen("settings")
    data object VoiceManager : Screen("voiceManager")
}

class MainActivity : ComponentActivity() {

    val LOG_TAG = "H2RNG_" + MainActivity::class.simpleName
    private var tts: TextToSpeech? = null
    private val ttsName = "org.hear2read.h2rng"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = AssetPackManagerFactory.getInstance(this)
        populateSizes() // TODO: check if there's a better place to put this

//        Synthesizer.initeSpeak(copyDataDir(this, "espeak-ng-data"))

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    InputScreen(this@MainActivity, navController)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController)
                }
                composable(Screen.VoiceManager.route) {
                    InstallVoicesScreen()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }

    @Composable
    fun InputScreen(context: Context, navController: NavController) {
        var inputText by remember { mutableStateOf("") }
        var isInitialized by remember { mutableStateOf(false) }
        var isSpeaking by remember { mutableStateOf(false) }
        var selectedH2RVoice: H2RVoice? by remember { mutableStateOf(null) }
        var selectedLang by remember { mutableStateOf<Locale?>(null) }
        var availableLangs by remember { mutableStateOf(listOf<Locale>()) }
        var selectedSpeed by remember { mutableFloatStateOf(1f) }
        var selectedVolume by remember { mutableFloatStateOf(50f) }

//    if (selectedVoice?.status?.value == DownloadStatus.NOT_DOWNLOADED
//        || selectedVoice?.status?.value == DownloadStatus.CORRUPTED
//    ) {
//        selectedVoice = null
//    }
//
//    var isInitialized by remember { mutableStateOf(false) }
//    var textToSpeech = TextToSpeech(
//        /* context = */ context,
//        /* listener = */ {
//            // TODO: warn user
//            isInitialized = it == TextToSpeech.SUCCESS
//        },
//        /* engine = */ "org.hear2read.h2rng"
//    )

        LaunchedEffect(Unit) {
            tts = TextToSpeech(context, { status ->
                if (status == TextToSpeech.SUCCESS) {
                    availableLangs = tts?.availableLanguages?.toList() ?: emptyList()
                    Log.d(LOG_TAG, "TTS onInit onDone, langs available: $availableLangs")
                    isInitialized = true
                }
            }, ttsName)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(LOG_TAG, "UtteranceProgressListener onStart: $utteranceId")
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(LOG_TAG, "UtteranceProgressListener onDone: $utteranceId")
                    isSpeaking = false
                }

                override fun onError(utteranceId: String?) {
                    Log.w(LOG_TAG, "UtteranceProgressListener onError: $utteranceId")
                    isSpeaking = false
                }
            })
        }

        Scaffold(
            topBar = {
                GlobalTopBar("Text to Speech")
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = @Composable { Text("Enter text to speak") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Language dropdown
                LanguageDropdown(
                    availableLangs = availableLangs,
                    isInitialized = isInitialized,
                    selectedVoice = selectedLang,
                    onVoiceSelected = { locale ->
                        selectedLang = locale
                        tts?.language = locale
                        // Send empty string to load language
                        tts?.speak(
                            "",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "LANG_LOAD"
                        )
                        val resourceName = "${selectedLang!!.isO3Language}_sample"
                        Log.d(LOG_TAG, "Resource name: $resourceName")
                        val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
                        val sampleText = if (resId != 0) context.getString(resId) else "Some random sample text."
                        inputText = sampleText
                    }
                )
//                var expanded by remember { mutableStateOf(false) }
//                OutlinedButton(
//                    onClick = { expanded = true },
//                    enabled = isInitialized
//                ) {
//                    Text(selectedLang?.displayName ?: "Select Language")
//                }
//                DropdownMenu(
//                    expanded = expanded,
//                    onDismissRequest = { expanded = false }
//                ) {
//                    availableLangs.forEach { locale ->
//                        DropdownMenuItem(
//                            text = { Text(locale.displayName) },
//                            onClick = {
//                                selectedLang = locale
//                                expanded = false
//                                tts?.language = locale
//                                // Send empty string to load language
//                                tts?.speak(
//                                    "",
//                                    TextToSpeech.QUEUE_FLUSH,
//                                    null,
//                                    "LANG_LOAD"
//                                )
//                            }
//                        )
//                    }
//                }

                SettingSlider(
                    label = "Speed",
                    value = selectedSpeed,
                    onValueChange = { selectedSpeed = it },
                    valueRange = 0.5f..2f,
//                    valueRange = 1f..100f,
                    steps = 98
                )

                SettingSlider(
                    label = "Volume",
                    value = selectedVolume,
                    onValueChange = { selectedVolume = it },
                    valueRange = 1f..100f,
                    steps = 98
                )

//            OutlinedTextField(
//                value = selectedSpeakerId,
//                onValueChange = { selectedSpeakerId = it },
//                label = { Text("Speaker ID (optional)") },
//                modifier = Modifier.fillMaxWidth(),
//                singleLine = true
//            )

                Button(
                    onClick = {
//                        if (text.isNotBlank() && selectedVoice != null) {
//                            CoroutineScope(Dispatchers.Default).launch {
//                                Synthesizer.speak(
//                                    text,
//                                    selectedVoice!!,
//                                    selectedSpeed.toInt(),
//                                    selectedVolume.toInt(),
//                                    //                            selectedSpeakerId
//                                    context,
//                                    null
//                                )
//                            }
//                        }
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            // Converting 0-100 to 0.5x to 2x rate
                            // var speechRate = (1.5f/100f)*selectedSpeed + 0.5f
                            tts?.setSpeechRate(selectedSpeed)
                            val params = Bundle().apply {
                                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (selectedVolume/100f))
                            }
                            tts?.speak(
                                text,
                                TextToSpeech.QUEUE_FLUSH,
                                params,
                                "UTTERANCE_ID"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inputText.isNotBlank() && isInitialized && selectedLang != null && !isSpeaking
                ) {
                    Text("Speak")
                }

                Button(
                    onClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Settings")
                }

//            Button(
//                onClick = {
//                    downloadFile(context, "https://hear2read.org/Hear2Read/voices-piper/${langToFile[selectedLanguage]}")
//                    downloadFile(context, "https://hear2read.org/Hear2Read/voices-piper/${langToFile[selectedLanguage]}.json")
//                },
//                modifier = Modifier.fillMaxWidth(),
//            ) {
//                Text("Download ${selectedLanguage.displayName} Model (Temporary)")
//            }

//            Button(
//                onClick = {
//                    val zipUrl = "https://nvda-addons.org/files/get.php?file=hear2read-beta"
//                    ZipDownloader.downloadAndUnzip(context, zipUrl, "hear2read_extracted")
//                },
//                modifier = Modifier.fillMaxWidth(),
//            ) {
//                Text("Download and unzip eSpeak files (Temporary)")
//            }
//
//            Button(
//                onClick = {
//                    val espeakDataPath = context.filesDir.absolutePath + "/hear2read_extracted/res/espeak-ng-data"
//                    println("espeakDataPath: $espeakDataPath")
//                    Synthesizer.initeSpeak(espeakDataPath)
//                },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Initialize eSpeak")
//            }
            }
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTopBar(text: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Hear2Read Logo",
                    modifier = Modifier
                        .padding(end = 8.dp)
                )
                Text(text)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}