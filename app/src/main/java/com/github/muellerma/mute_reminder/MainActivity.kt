package com.github.muellerma.mute_reminder

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.muellerma.mute_reminder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.settings.setOnClickListener {
            Intent(this, PreferenceActivity::class.java).apply {
                startActivity(this)
            }
        }

        ForegroundService.changeState(this, true)
    }
}