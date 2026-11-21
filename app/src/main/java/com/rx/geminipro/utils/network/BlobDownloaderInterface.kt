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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.rx.geminipro.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
                saveToMediaStore(filename, mimeType, decodedBytes)
            } else {
                saveToLegacyDownloads(filename, decodedBytes)
            }

            Log.d(TAG, "File saved with URI: $targetUri")

            showToast("Saved to Downloads: $filename")
            showDownloadCompleteNotification(filename, targetUri, mimeType)

        } catch (e: Exception) {
            Log.e(TAG, "Blob download failed", e)
            showToast("Download failed: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(filename: String, mimeType: String, bytes: ByteArray): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            // Keep the entry hidden from other apps until the write has finished.
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Could not create MediaStore entry in Downloads")

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
            } ?: throw IOException("Could not open output stream for MediaStore URI")

            val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, published, null, null)
        } catch (e: Exception) {
            // Don't leave an empty or half-written file behind in Downloads.
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    private fun saveToLegacyDownloads(filename: String, bytes: ByteArray): Uri {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            val created = downloadsDir.mkdirs()
            if (!created && !downloadsDir.exists()) {
                throw IOException("Could not create Downloads directory")
            }
        }

        val file = File(downloadsDir, filename)
        try {
            FileOutputStream(file).use { it.write(bytes) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
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
