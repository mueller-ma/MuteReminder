package com.github.muellerma.mute_reminder

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

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

fun Context.hasPermission(string: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        string
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return hasPermission(android.Manifest.permission.POST_NOTIFICATIONS)
}