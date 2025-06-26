package com.example.ttsdemo

import android.content.Context
import java.io.File
import java.io.FileOutputStream
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
            e.printStackTrace()
        }
    }.start()
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