package com.rx.geminipro.utils.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.rx.geminipro.R
import java.io.File
import java.io.FileOutputStream

private const val TAG = "BlobDownloader"

class BlobDownloaderInterface(private val context: Context) {

    @JavascriptInterface
    fun processBlobData(base64Data: String?, mimeType: String?) {
        Log.d(TAG, "processBlobData received: mimeType=$mimeType, dataLength=${base64Data?.length ?: 0}")
        if (base64Data == null || mimeType == null) {
            Log.e(TAG, "processBlobData called with null data: base64Data=$base64Data, mimeType=$mimeType")
            return
        }

        try {
            val cleanBase64 = cleanBase64Payload(base64Data)

            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
            val filename = "geminipro_${System.currentTimeMillis()}.$extension"

            val targetUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Could not create MediaStore entry in Downloads")
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(decodedBytes)
                } ?: throw Exception("Could not open output stream for MediaStore URI")
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    val created = downloadsDir.mkdirs()
                    if (!created && !downloadsDir.exists()) {
                        throw Exception("Could not create Downloads directory")
                    }
                }
                val file = File(downloadsDir, filename)
                FileOutputStream(file).use { it.write(decodedBytes) }
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            }

            Log.d(TAG, "File saved with URI: $targetUri")

            showToast("Saved to Downloads: $filename")
            showDownloadCompleteNotification(filename, targetUri, mimeType)

        } catch (e: Exception) {
            Log.e(TAG, "Blob download failed", e)
            showToast("Download failed: ${e.message}")
        }
    }

    @JavascriptInterface
    fun jsLog(message: String) {
        Log.d("WebViewJS", "JS Log: $message")
    }

    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDownloadCompleteNotification(filename: String, fileUri: Uri, mimeType: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "blob_downloads"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.google_gemini_icon)
                .setContentTitle("Saved")
                .setContentText(filename)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification", e)
        }
    }

    companion object {
        internal fun cleanBase64Payload(data: String): String {
            return if (data.contains(",")) {
                data.substringAfter(",")
            } else {
                data
            }
        }
    }
}
