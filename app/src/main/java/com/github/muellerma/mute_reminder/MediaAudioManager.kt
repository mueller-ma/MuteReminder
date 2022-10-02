package com.github.muellerma.mute_reminder

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService

class MediaAudioManager(context: Context) {
    private val audioManager: AudioManager = context.getSystemService()!!
    private val prefs = Prefs(context)

    fun shouldNotify(): Boolean {
        return (isRingToneMuted() || prefs.ignoreRingTone) && !isMediaMuted() && !usesRemoteOutput()
    }

    private fun isMediaMuted(): Boolean {
        val mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.d(TAG, "mediaVolume=$mediaVolume")
        return mediaVolume == 0
    }

    private fun isRingToneMuted(): Boolean {
        val ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        Log.d(TAG, "ringVolume=$ringVolume")
        return ringVolume == 0
    }

    private fun usesRemoteOutput(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val ignoreOutputs = listOf(
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET
            )

            val remoteOutputs = audioManager
                .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { device -> device.isSink }
                .filter { device -> device.type in ignoreOutputs }

            Log.d(TAG, "Connected to ${remoteOutputs.joinToString(", ") { it.productName }}")
            remoteOutputs.isNotEmpty()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
        }
    }

    companion object {
        private val TAG = MediaAudioManager::class.java.simpleName
    }
}