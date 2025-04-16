package com.rx.geminipro.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun getFilePickerLauncher(
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>
): ActivityResultLauncher<Intent> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallbackState.value
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val clipData = data.clipData
                if (clipData != null) {
                    val uris = ArrayList<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                    callback?.onReceiveValue(uris.toTypedArray())
                } else {
                    val uri = data.data
                    if (uri != null) {
                        callback?.onReceiveValue(arrayOf(uri))
                    } else {
                        callback?.onReceiveValue(null)
                    }
                }
            } else {
                callback?.onReceiveValue(null)
            }
        } else {
            callback?.onReceiveValue(null)
        }
        filePathCallbackState.value = null
    }
}