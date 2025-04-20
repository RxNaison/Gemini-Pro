package com.rx.geminipro.utils.network

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.rx.geminipro.R // Assuming you have a default icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlobDownloaderInterface(private val context: Context) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    @JavascriptInterface
    fun processBlobData(base64Data: String?, mimeType: String?, filenameHint: String?) {
        if (base64Data == null || mimeType == null) {
            Log.e("BlobDownloader", "Received null data or mimeType")
            showToast("Download failed: Invalid data received")
            return
        }

        ioScope.launch {
            try {
                Log.d("BlobDownloader", "Received blob data. MimeType: $mimeType, FilenameHint: $filenameHint")
                // Remove the "data:mime/type;base64," prefix if present
                val pureBase64 = base64Data.substringAfter("base64,")
                val fileData = Base64.decode(pureBase64, Base64.DEFAULT)

                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
                val defaultName = "download_${System.currentTimeMillis()}"
                val finalFilename = when {
                    filenameHint != null && filenameHint.contains('.') -> filenameHint // Use hint if it seems complete
                    filenameHint != null -> "$filenameHint.$extension" // Add extension to hint
                    else -> "$defaultName.$extension" // Generate default name
                }

                // Use MediaStore for Android Q+ ideally, but Downloads directory for simplicity here
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, finalFilename)

                Log.d("BlobDownloader", "Saving to: ${file.absolutePath}")

                FileOutputStream(file).use { os ->
                    os.write(fileData)
                    os.flush()
                }

                Log.d("BlobDownloader", "File saved successfully.")
                showDownloadCompleteNotification(file, mimeType)
                showToast("Download complete: $finalFilename")

            } catch (e: Exception) {
                Log.e("BlobDownloader", "Error saving blob file", e)
                showToast("Download failed: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showDownloadCompleteNotification(file: File, mimeType: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "blob_downloads"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val fileUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Make sure this matches AndroidManifest provider authority
                file
            )
        } catch (e: IllegalArgumentException) {
            Log.e("BlobDownloader", "FileProvider error: ${e.message}")
            showToast("Error creating file URI for notification")
            return // Cant proceed without a valid URI
        }


        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check if there's an activity that can handle this intent
        if (intent.resolveActivity(context.packageManager) == null) {
            Log.w("BlobDownloader", "No activity found to handle VIEW intent for type $mimeType")
            // Fallback: Maybe just open Download Manager or show a generic notification?
            // For now, just log it. The notification won't open anything specific.
            showToast("No app found to open ${file.name}")
            // You might want to create a generic intent here or just skip the contentIntent
        }


        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .setContentTitle("Download complete")
            .setContentText(file.name)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Use a unique ID for each notification based on file path or timestamp
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }


    companion object {
        // Generates the JavaScript code to fetch the blob, convert to base64, and call back
        fun getBase64StringFromBlobUrl(blobUrl: String, mimeType: String?, filenameHint: String?): String {
            // Escape potential problematic characters in parameters for JS string literals
            val safeMimeType = mimeType?.replace("'", "\\'") ?: ""
            val safeFilenameHint = filenameHint?.replace("'", "\\'") ?: ""
            val safeBlobUrl = blobUrl.replace("'", "\\'") // Basic escaping

            // Note: The JS interface name is 'AndroidBlobHandler' here
            return """
            javascript:(function() {
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '$safeBlobUrl', true);
                xhr.responseType = 'blob';
                xhr.onload = function(e) {
                    if (this.status == 200) {
                        var blobData = this.response;
                        var reader = new FileReader();
                        reader.readAsDataURL(blobData);
                        reader.onloadend = function() {
                            var base64data = reader.result;
                            // Call Android interface method
                            if (window.AndroidBlobHandler && typeof window.AndroidBlobHandler.processBlobData === 'function') {
                                window.AndroidBlobHandler.processBlobData(base64data, '$safeMimeType', '$safeFilenameHint');
                            } else {
                                console.error('AndroidBlobHandler interface not found or processBlobData method is missing.');
                            }
                        }
                        reader.onerror = function(error) {
                             console.error('FileReader error:', error);
                             // Optionally notify Android about the error
                        };
                    } else {
                         console.error('XHR failed with status:', this.status);
                         // Optionally notify Android about the error
                    }
                };
                 xhr.onerror = function(error) {
                    console.error('XHR network error:', error);
                     // Optionally notify Android about the error
                };
                xhr.send();
            })();
            """.trimIndent()
        }
    }
}