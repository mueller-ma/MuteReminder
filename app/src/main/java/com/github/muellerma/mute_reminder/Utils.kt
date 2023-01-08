package com.github.muellerma.mute_reminder

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

private const val TAG = "Utils"

fun String.openInBrowser(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(this))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.d(TAG, "Unable to open url in browser: $intent")
        context.showToast(R.string.error_no_browser_found)
    }
}

fun Context.hasPermission(string: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        string
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.showToast(@StringRes msg: Int) {
    Toast
        .makeText(this, msg, Toast.LENGTH_SHORT)
        .show()
}
