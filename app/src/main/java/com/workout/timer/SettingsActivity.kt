package com.workout.timer

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    private lateinit var tvRepsVal: TextView
    private lateinit var sbActive: SeekBar
    private lateinit var sbRest: SeekBar
    private lateinit var tvActiveVal: TextView
    private lateinit var tvRestVal: TextView
    private lateinit var tvTotal: TextView
    private lateinit var swStart: SwitchCompat
    private lateinit var swRest: SwitchCompat
    private lateinit var swMilestones: SwitchCompat
    private lateinit var swWarmup: SwitchCompat
    private lateinit var sbWarmup: SeekBar
    private lateinit var tvWarmupVal: TextView

    private var reps = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        tvRepsVal    = findViewById(R.id.tvRepsVal)
        sbActive     = findViewById(R.id.sbActive)
        sbRest       = findViewById(R.id.sbRest)
        tvActiveVal  = findViewById(R.id.tvActiveVal)
        tvRestVal    = findViewById(R.id.tvRestVal)
        tvTotal      = findViewById(R.id.tvTotal)
        swStart      = findViewById(R.id.swAnnounceStart)
        swRest       = findViewById(R.id.swAnnounceRest)
        swMilestones = findViewById(R.id.swMilestones)
        swWarmup     = findViewById(R.id.swWarmup)
        sbWarmup     = findViewById(R.id.sbWarmup)
        tvWarmupVal  = findViewById(R.id.tvWarmupVal)

        // Reps +/- buttons
        reps = prefs.reps
        tvRepsVal.text = "$reps"
        findViewById<Button>(R.id.btnRepsMinus).setOnClickListener {
            if (reps > 1) { reps--; tvRepsVal.text = "$reps"; updateTotal() }
        }
        findViewById<Button>(R.id.btnRepsPlus).setOnClickListener {
            if (reps < 30) { reps++; tvRepsVal.text = "$reps"; updateTotal() }
        }

        // Active time: 10–180s, seekbar offset by 10
        sbActive.max = 170
        sbActive.progress = prefs.activeSeconds - 10
        tvActiveVal.text = formatSec(prefs.activeSeconds)
        sbActive.setOnSeekBarChangeListener(simpleSeekListener { p ->
            tvActiveVal.text = formatSec(p + 10)
            updateTotal()
        })

        // Rest time: 5–180s, seekbar offset by 5
        sbRest.max = 175
        sbRest.progress = prefs.restSeconds - 5
        tvRestVal.text = formatSec(prefs.restSeconds)
        sbRest.setOnSeekBarChangeListener(simpleSeekListener { p ->
            tvRestVal.text = formatSec(p + 5)
            updateTotal()
        })

        swStart.isChecked      = prefs.announceStart
        swRest.isChecked       = prefs.announceRest
        swMilestones.isChecked = prefs.announceMilestones

        // Warmup: 10–59s, offset by 10
        swWarmup.isChecked = prefs.warmupEnabled
        sbWarmup.max = 49  // 59 - 10
        sbWarmup.progress = prefs.warmupSeconds - 10
        tvWarmupVal.text = "${prefs.warmupSeconds}s"
        sbWarmup.isEnabled = prefs.warmupEnabled
        swWarmup.setOnCheckedChangeListener { _, checked ->
            sbWarmup.isEnabled = checked
            tvWarmupVal.text = if (checked) "${sbWarmup.progress + 10}s" else "off"
        }
        sbWarmup.setOnSeekBarChangeListener(simpleSeekListener { p ->
            tvWarmupVal.text = "${p + 10}s"
        })

        updateTotal()

        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }
        findViewById<Button>(R.id.btnReset).setOnClickListener { resetDefaults() }
    }

    private fun updateTotal() {
        val active = sbActive.progress + 10
        val rest   = sbRest.progress + 5
        val total  = (reps * active) + ((reps - 1) * rest)
        val min = total / 60
        val sec = total % 60
        tvTotal.text = if (sec == 0) "${min}m" else "${min}m ${sec}s"
    }

    private fun save() {
        prefs.reps               = reps
        prefs.activeSeconds      = sbActive.progress + 10
        prefs.restSeconds        = sbRest.progress + 5
        prefs.announceStart      = swStart.isChecked
        prefs.announceRest       = swRest.isChecked
        prefs.announceMilestones = swMilestones.isChecked
        prefs.warmupEnabled      = swWarmup.isChecked
        prefs.warmupSeconds      = sbWarmup.progress + 10
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resetDefaults() {
        reps = 12
        tvRepsVal.text   = "12"
        sbActive.progress = 20   // 30 - 10
        sbRest.progress   = 5    // 10 - 5
        tvActiveVal.text  = formatSec(30)
        tvRestVal.text    = formatSec(10)
        swStart.isChecked      = true
        swRest.isChecked       = true
        swMilestones.isChecked = false
        swWarmup.isChecked     = true
        sbWarmup.progress      = 0  // 10s
        sbWarmup.isEnabled     = true
        tvWarmupVal.text       = formatSec(10)
        updateTotal()
    }

    private fun simpleSeekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) = onChange(p)
        override fun onStartTrackingTouch(sb: SeekBar) {}
        override fun onStopTrackingTouch(sb: SeekBar) {}
    }

    /** Show seconds under 60 as "45s", at 60+ as "1m", "1m 30s", etc. */
    private fun formatSec(s: Int): String {
        if (s < 60) return "${s}s"
        val m = s / 60
        val r = s % 60
        return if (r == 0) "${m}m" else "${m}m ${r}s"
    }
}
