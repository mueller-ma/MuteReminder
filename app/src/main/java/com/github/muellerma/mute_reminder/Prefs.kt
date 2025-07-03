package com.github.muellerma.mute_reminder

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Prefs(context: Context) {
    var sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        private set

    var notifyOnlyWhenMuted: Boolean
        get() = sharedPrefs.getBoolean(NOTIFY_ONLY_WHEN_MUTED, true)
        set(value) = sharedPrefs.edit { putBoolean(NOTIFY_ONLY_WHEN_MUTED, value) }

    var autoMute: Boolean
        get() = sharedPrefs.getBoolean(AUTO_MUTE, false)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_MUTE, value) }

    companion object {
        const val NOTIFY_ONLY_WHEN_MUTED = "notify_only_when_muted"
        const val AUTO_MUTE = "auto_mute"
    }
}
