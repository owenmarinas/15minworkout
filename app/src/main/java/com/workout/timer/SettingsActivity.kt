package com.workout.timer

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    private lateinit var spinnerProfile: Spinner
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
    private var pauseSpinnerListener = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        spinnerProfile = findViewById(R.id.spinnerProfile)
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

        setupProfileSpinner()

        // Reps +/- buttons
        reps = prefs.reps
        tvRepsVal.text = "$reps"
        findViewById<Button>(R.id.btnRepsMinus).setOnClickListener {
            if (reps > 1) { reps--; tvRepsVal.text = "$reps"; updateTotal() }
        }
        findViewById<Button>(R.id.btnRepsPlus).setOnClickListener {
            if (reps < 30) { reps++; tvRepsVal.text = "$reps"; updateTotal() }
        }

        sbActive.max = 170
        sbActive.progress = prefs.activeSeconds - 10
        tvActiveVal.text = formatSec(prefs.activeSeconds)
        sbActive.setOnSeekBarChangeListener(simpleSeekListener { p ->
            tvActiveVal.text = formatSec(p + 10)
            updateTotal()
        })

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

        swWarmup.isChecked = prefs.warmupEnabled
        sbWarmup.max = 49
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

        // Profile management buttons
        findViewById<Button>(R.id.btnProfileAdd).setOnClickListener { promptAddProfile() }
        findViewById<Button>(R.id.btnProfileRename).setOnClickListener { promptRenameProfile() }
        findViewById<Button>(R.id.btnProfileDelete).setOnClickListener { confirmDeleteProfile() }
    }

    // ── Profile spinner ──────────────────────────────────────────────────────

    private fun setupProfileSpinner() {
        val names = prefs.profileNames
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pauseSpinnerListener = true
        spinnerProfile.adapter = adapter
        val idx = names.indexOf(prefs.activeProfile).coerceAtLeast(0)
        spinnerProfile.setSelection(idx)
        pauseSpinnerListener = false
        spinnerProfile.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (pauseSpinnerListener) return
                val selected = names[pos]
                if (selected != prefs.activeProfile) {
                    prefs.activeProfile = selected
                    loadSettingsFromPrefs()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun refreshProfileSpinner() {
        val names = prefs.profileNames
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pauseSpinnerListener = true
        spinnerProfile.adapter = adapter
        val idx = names.indexOf(prefs.activeProfile).coerceAtLeast(0)
        spinnerProfile.setSelection(idx)
        pauseSpinnerListener = false
    }

    private fun loadSettingsFromPrefs() {
        reps = prefs.reps
        tvRepsVal.text = "$reps"
        sbActive.progress = prefs.activeSeconds - 10
        tvActiveVal.text = formatSec(prefs.activeSeconds)
        sbRest.progress = prefs.restSeconds - 5
        tvRestVal.text = formatSec(prefs.restSeconds)
        swStart.isChecked = prefs.announceStart
        swRest.isChecked = prefs.announceRest
        swMilestones.isChecked = prefs.announceMilestones
        swWarmup.isChecked = prefs.warmupEnabled
        sbWarmup.progress = prefs.warmupSeconds - 10
        sbWarmup.isEnabled = prefs.warmupEnabled
        tvWarmupVal.text = if (prefs.warmupEnabled) "${prefs.warmupSeconds}s" else "off"
        updateTotal()
    }

    // ── Profile CRUD dialogs ─────────────────────────────────────────────────

    private fun promptAddProfile() {
        val input = EditText(this).apply { hint = "Profile name" }
        AlertDialog.Builder(this)
            .setTitle("Add Profile")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (prefs.profileNames.contains(name)) {
                    Toast.makeText(this, "Profile already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefs.createProfile(name)
                prefs.activeProfile = name
                refreshProfileSpinner()
                loadSettingsFromPrefs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptRenameProfile() {
        val current = prefs.activeProfile
        val input = EditText(this).apply { setText(current) }
        AlertDialog.Builder(this)
            .setTitle("Rename Profile")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (name != current && prefs.profileNames.contains(name)) {
                    Toast.makeText(this, "Profile already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefs.renameProfile(current, name)
                refreshProfileSpinner()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteProfile() {
        val current = prefs.activeProfile
        if (prefs.profileNames.size <= 1) {
            Toast.makeText(this, "Cannot delete the last profile", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Profile")
            .setMessage("Delete \"$current\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                prefs.deleteProfile(current)
                refreshProfileSpinner()
                loadSettingsFromPrefs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Workout settings helpers ─────────────────────────────────────────────

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
        tvRepsVal.text    = "12"
        sbActive.progress = 20
        sbRest.progress   = 5
        tvActiveVal.text  = formatSec(30)
        tvRestVal.text    = formatSec(10)
        swStart.isChecked      = true
        swRest.isChecked       = true
        swMilestones.isChecked = false
        swWarmup.isChecked     = true
        sbWarmup.progress      = 0
        sbWarmup.isEnabled     = true
        tvWarmupVal.text       = formatSec(10)
        updateTotal()
    }

    private fun simpleSeekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) = onChange(p)
        override fun onStartTrackingTouch(sb: SeekBar) {}
        override fun onStopTrackingTouch(sb: SeekBar) {}
    }

    private fun formatSec(s: Int): String {
        if (s < 60) return "${s}s"
        val m = s / 60
        val r = s % 60
        return if (r == 0) "${m}m" else "${m}m ${r}s"
    }
}
