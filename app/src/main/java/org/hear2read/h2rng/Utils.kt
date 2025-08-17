/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*
* Description:
* Functions:
* fun copyFile(context: Context, filename: String)
*   Simply copies the file named 'filename' (parameter) from the assets to the internal files directory.
*
* fun copyAssets(context: Context, path: String)
*   This function recursively traverses the directory at 'path' (parameter) in the assets and copies all files to the internal files directory using copyFile.
*
* fun copyDataDir(context: Context, dataDir: String): String
*   Wrapper around copyAssets that also return the absolute path of the newly copied directory.
*
* fun getFileWithExtension(directory: String, extension: String) : File?
*   Returns the first file (File object) in the 'directory' (parameter) with the given 'extension' (parameter).
*/

package org.hear2read.h2rng

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun copyFile(context: Context, filename: String) {
    try {
        val istream = context.assets.open(filename)
        val newFilename = context.filesDir.absolutePath + "/" + filename

//        println("newFilename: $newFilename")

        val ostream = FileOutputStream(newFilename)
        // Log.i(TAG, "Copying $filename to $newFilename")
        val buffer = ByteArray(1024)
        var read = 0
        while (read != -1) {
            ostream.write(buffer, 0, read)
            read = istream.read(buffer)
        }
        istream.close()
        ostream.flush()
        ostream.close()
    } catch (ex: Exception) {
        println("Failed to copy $filename, $ex")
    }
}

fun copyAssets(context: Context, path: String) {
    val assets: Array<String>?
    try {
        assets = context.assets.list(path)
        if (assets!!.isEmpty()) {
            // If assets is empty, that means we have reached a file.
            copyFile(context, path)
        } else {
            val fullPath = "${context.filesDir.absolutePath}/$path"
            val dir = File(fullPath)
            dir.mkdirs()
            for (asset in assets.iterator()) {
                val p: String = if (path == "") "" else "$path/"
                copyAssets(context, p + asset)
            }
        }
    } catch (ex: IOException) {
        println("Failed to copy $path. $ex")
    }
}

fun copyDataDir(context: Context, dataDir: String): String {
    println("data dir is $dataDir")
    copyAssets(context, dataDir)

    val newDataDir = File(context.filesDir!!.absolutePath, dataDir)
    println("newDataDir: ${newDataDir.absolutePath}")
    return newDataDir.absolutePath
}

fun getFileWithExtension(directory: String, extension: String) : File? {
    val files = File(directory).listFiles()
    if (files != null) {
        for (file in files) {
            if (file.isFile && file.name.endsWith(extension)) {
                return file
            }
        }
    }

    return null
}
// Use the below implementation if model needs to be stored in external storage

//fun downloadFileWithDownloadManager(
//    context: Context,
//    url: String
//) {
//    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//    val fileUrl = url.toUri()
//
//    val modelFileName = fileUrl.path!!.substringAfterLast('/') // Extract filename from URL
//
//    val request = DownloadManager.Request(fileUrl)
//        .setTitle(modelFileName)
//        .setDescription("Downloading file $modelFileName")
//        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//        .setDestinationInExternalFilesDir(context, "H2R_Model_Files", modelFileName)
//        .setAllowedOverMetered(true)
//        .setAllowedOverRoaming(false)
//
//    val downloadId = downloadManager.enqueue(request)
//    println("Download enqueued with ID: $downloadId. File: $modelFileName")
//}