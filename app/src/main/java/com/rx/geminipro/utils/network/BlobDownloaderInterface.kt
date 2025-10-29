package com.rx.geminipro.utils.network

import android.os.Environment
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder

private const val TAG = "BlobDownloader"


class BlobDownloaderInterface() {
    @JavascriptInterface
    fun processBlobData(dataUrl: String, filename: String) : File? {
        try {
            val isBase64 = dataUrl.contains(";base64")
            val data = if(isBase64){
                val base64EncodedData = dataUrl.substringAfter("base64,")
                Base64.decode(base64EncodedData, Base64.DEFAULT)
            } else {
                val utf8EncodedData = dataUrl.substringAfter("utf-8,")
                val decodedText = URLDecoder.decode(utf8EncodedData, "UTF-8")
                decodedText.toByteArray(Charsets.UTF_8)
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, filename)

            FileOutputStream(file).use { fos ->
                fos.write(data)
            }
            Log.d(TAG, "File saved successfully to ${file.absolutePath}")

            return file
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save blob data to file.", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid Base64 string from data URL.", e)
        }
        return null
    }
}