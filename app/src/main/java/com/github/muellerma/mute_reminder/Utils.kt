package com.github.muellerma.mute_reminder

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

private const val TAG = "Utils"

fun String.openInBrowser(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(this))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.d(TAG, "Unable to open url in browser: $intent")
        Toast
            .makeText(context, R.string.error_no_browser_found, Toast.LENGTH_SHORT)
            .show()
    }
}