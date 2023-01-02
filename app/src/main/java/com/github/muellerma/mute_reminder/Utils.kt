package com.github.muellerma.mute_reminder

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
val PendingIntent_Immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    PendingIntent.FLAG_IMMUTABLE
} else {
    0
}

val PendingIntent_Mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    PendingIntent.FLAG_MUTABLE
} else {
    0
}