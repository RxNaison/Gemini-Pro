package com.rx.geminipro.utils.system

import android.util.Log
import com.rx.geminipro.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


data class UpdateInfo(
    val version: String,
    val changelog: String
)

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/RxNaison/Gemini-Pro/releases/latest"


    suspend fun checkForNewVersion(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val remoteTag = json.getString("tag_name")
                val body = json.optString("body", "No release notes provided.")

                val currentVersion = BuildConfig.VERSION_NAME
                val cleanRemote = remoteTag.replace("v", "")
                val cleanCurrent = currentVersion.replace("v", "")

                if (isNewer(cleanRemote, cleanCurrent)) {
                    Log.d("UpdateChecker", "Update found: $remoteTag (Current: $currentVersion)")
                    return@withContext UpdateInfo(remoteTag, body)
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Check failed", e)
        }
        return@withContext null
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun getReleaseUrl(): String {
        return "https://github.com/RxNaison/Gemini-Pro/releases/latest"
    }
}