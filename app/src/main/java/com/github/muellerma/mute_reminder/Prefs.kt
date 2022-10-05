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

    companion object {
        const val NOTIFY_ONLY_WHEN_MUTED = "notify_only_when_muted"
    }
}