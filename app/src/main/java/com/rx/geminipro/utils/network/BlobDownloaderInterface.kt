package com.rx.geminipro.utils.network

import android.os.Environment
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "BlobDownloader"


class BlobDownloaderInterface() {
    @JavascriptInterface
    fun processBlobData(dataUrl: String, filename: String) : File? {
        try {
            val base64EncodedData = dataUrl.substringAfter("base64,")
            val data = Base64.decode(base64EncodedData, Base64.DEFAULT)

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