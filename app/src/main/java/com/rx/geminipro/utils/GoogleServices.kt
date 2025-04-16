package com.rx.geminipro.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

class GoogleServices {
    fun openGoogleDocs(context: Context) {
        val googleDocsUrl = "https://docs.google.com/document/create"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleDocsUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openGoogleDrive(context: Context) {
        val googleDocsUrl = "https://drive.google.com/drive/my-drive"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleDocsUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}