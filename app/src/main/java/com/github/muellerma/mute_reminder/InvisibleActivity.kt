package com.github.muellerma.mute_reminder

import android.app.Activity
import android.os.Bundle
import android.util.Log

class InvisibleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == ACTION_MUTE) {
            Log.d(TAG, "Received mute action")
            MediaAudioManager(this).muteMedia()
        }

        finishAndRemoveTask()
    }

    companion object {
        private val TAG = InvisibleActivity::class.java.simpleName
        const val ACTION_MUTE = "mute"
    }
}