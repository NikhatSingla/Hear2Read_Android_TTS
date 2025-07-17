/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package com.example.ttsdemo

val langToFile = mapOf(
    Language.HI to "hi-v6-tdilv2mono-1665val-med.onnx",
    Language.PA to "pa-v4-tdil-1419-low.onnx",
    Language.TEMP to "temp.onnx"
)

val iso3ToLang = mapOf(
    "hin" to Language.HI,
    "pan" to Language.PA,
    "eng" to Language.HI
)


enum class VoiceCardStatus { DOWNLOAD, DOWNLOADING, DOWNLOADED, ERROR }
data class VoiceItemData(
    val id: String,
    val name: String,
    val quality: String? = null,
    var status: VoiceCardStatus,
    val fileSize: String? = null,
    val progress: Float? = null,
    val details: String = ""
)

val installedVoices = listOf(
    Language.HI,
    Language.PA
)

val updatableVoices = listOf(
    Language.HI,
)

val corruptedVoices = listOf(
    Language.PA
)

val downloadableVoices = listOf(
    Language.EN,
    Language.AS
)

enum class Language(val displayName: String) {
    HI("Hindi"),
    PA("Punjabi"),
    TEMP("Testing"),
    EN("English"),
    AS("Assamese")
}