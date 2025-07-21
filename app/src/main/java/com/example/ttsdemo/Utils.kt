/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package com.example.ttsdemo

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

fun downloadFile(context: Context, modelUrl: String) {
    Thread {
        try {
            val url = URL(modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                println("Server returned HTTP ${connection.responseCode}")
                return@Thread
            }

            // Extract filename from URL
            val fileName = url.path.substringAfterLast('/')

            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)

            val inputStream = connection.inputStream
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()

            println("Downloaded file: $fileName")
        } catch (e: Exception) {
            println("Download has failed")
            e.printStackTrace()
        }
    }.start()
}

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