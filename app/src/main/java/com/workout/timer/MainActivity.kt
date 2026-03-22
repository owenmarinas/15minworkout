package com.workout.timer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        showHome()
    }

    private fun showHome() {
        setContentView(R.layout.activity_main)
        updateTotalTime()

        findViewById<Button>(R.id.btnStartWorkout).setOnClickListener {
            startActivity(Intent(this, WorkoutActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh total time display after returning from settings
        if (findViewById<View>(R.id.tvTotalTime) != null) {
            updateTotalTime()
        }
    }

    private fun updateTotalTime() {
        val total = prefs.totalSeconds()
        val min = total / 60
        val sec = total % 60
        val label = if (sec == 0) "${min}m" else "${min}m ${sec}s"
        findViewById<TextView>(R.id.tvTotalTime).text = "15-Min Workout  •  $label total"
    }
}
