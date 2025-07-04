/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package com.example.ttsdemo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ttsdemo.ui.theme.TTSNGDemoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val languages = Language.entries

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLanguage.displayName,
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
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onLanguageSelected(language)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(context: Context) {
    var text by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(Language.TEMP) }
    var selectedSpeed by remember { mutableFloatStateOf(50f) }
    var selectedVolume by remember { mutableFloatStateOf(50f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TTS Configuration") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
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
                value = text,
                onValueChange = { text = it },
                label = @Composable { Text("Enter text to speak") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            LanguageDropdown(selectedLanguage) { newLanguage ->
                selectedLanguage = newLanguage
                CoroutineScope(Dispatchers.IO).launch {
                    Synthesizer.getOrLoadModel(context, selectedLanguage)
                }
            }

            SettingSlider(
                label = "Speed",
                value = selectedSpeed,
                onValueChange = { selectedSpeed = it },
                valueRange = 1f..100f,
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
                    if (text.isNotBlank()) {
                        CoroutineScope(Dispatchers.Default).launch {
                            Synthesizer.speak(
                                text,
                                selectedLanguage,
                                selectedSpeed.toInt(),
                                selectedVolume.toInt(),
                                //                            selectedSpeakerId
                                context,
                                null
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank() // Enable button only if text is entered
            ) {
                Text("Speak")
            }

            Button(
                onClick = {
                    downloadFile(context, "https://hear2read.org/Hear2Read/voices-piper/${langToFile[selectedLanguage]}")
                    downloadFile(context, "https://hear2read.org/Hear2Read/voices-piper/${langToFile[selectedLanguage]}.json")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Download ${selectedLanguage.displayName} Model (Temporary)")
            }

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        copyDataDir(this, "espeak-ng-data")
        Synthesizer.initeSpeak(copyDataDir(this, "espeak-ng-data"))

        enableEdgeToEdge()
        setContent {
            InputScreen(this)
//            TTSNGDemoTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TTSNGDemoTheme {
        Greeting("Android")
    }
}