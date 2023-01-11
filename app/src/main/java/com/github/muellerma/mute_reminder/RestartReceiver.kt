package com.github.muellerma.mute_reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restart MuteReminder after boot or app update.
 */
class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in validActions) {
            return
        }

        ForegroundService.changeState(context, true)
    }

    companion object {
        private val validActions = listOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_BOOT_COMPLETED
        )
    }
}