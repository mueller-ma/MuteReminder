package com.github.muellerma.mute_reminder

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Prefs(context: Context) {
    var sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        private set

    var ignoreRingTone: Boolean
        get() = sharedPrefs.getBoolean(IGNORE_RING_TONE, true)
        set(value) = sharedPrefs.edit { putBoolean(IGNORE_RING_TONE, value) }

    companion object {
        const val IGNORE_RING_TONE = "notify_only_when_muted"
    }
}