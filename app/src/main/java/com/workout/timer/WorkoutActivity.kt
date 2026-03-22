package com.workout.timer

import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class WorkoutActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var prefs: Prefs
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // UI
    private lateinit var tvPhase: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvRepProgress: TextView
    private lateinit var tvOverallPct: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutWorkout: View
    private lateinit var layoutPaused: View
    private lateinit var layoutComplete: View
    private lateinit var tvCompleteTime: TextView
    private lateinit var tvPausedPhase: TextView
    private lateinit var tvPausedTimeLeft: TextView
    private lateinit var tvPausedSet: TextView

    // State
    private var currentRep = 1
    private var totalReps = 12
    private var activeSeconds = 30
    private var restSeconds = 10
    private var isWorkPhase = true
    private var isWarmup = false
    private var isPaused = false
    private var timer: CountDownTimer? = null
    private var remainingMs = 0L
    private var totalWorkoutMs = 0L
    private var elapsedMs = 0L

    private val milestonesAnnounced = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = Prefs(this)
        tts = TextToSpeech(this, this)

        tvPhase       = findViewById(R.id.tvPhase)
        tvCountdown   = findViewById(R.id.tvCountdown)
        tvRepProgress = findViewById(R.id.tvRepProgress)
        tvOverallPct  = findViewById(R.id.tvOverallPct)
        progressBar   = findViewById(R.id.progressBar)
        layoutWorkout = findViewById(R.id.layoutWorkout)
        layoutPaused  = findViewById(R.id.layoutPaused)
        layoutComplete = findViewById(R.id.layoutComplete)
        tvCompleteTime = findViewById(R.id.tvCompleteTime)
        tvPausedPhase    = findViewById(R.id.tvPausedPhase)
        tvPausedTimeLeft = findViewById(R.id.tvPausedTimeLeft)
        tvPausedSet      = findViewById(R.id.tvPausedSet)

        totalReps      = prefs.reps
        activeSeconds  = prefs.activeSeconds
        restSeconds    = prefs.restSeconds
        totalWorkoutMs = prefs.totalSeconds() * 1000L

        // Tap anywhere on the workout screen to pause
        layoutWorkout.setOnClickListener { if (!isWarmup) pause() }
        // Tap anywhere on the paused overlay to resume
        layoutPaused.setOnClickListener { resume() }

        // Paused overlay buttons
        findViewById<Button>(R.id.btnContinue).setOnClickListener { resume() }
        findViewById<Button>(R.id.btnCancelWorkout).setOnClickListener { confirmCancel() }

        // Complete screen buttons
        findViewById<Button>(R.id.btnDoAgain).setOnClickListener { restartWorkout() }
        findViewById<Button>(R.id.btnDone).setOnClickListener { finish() }

        if (prefs.warmupEnabled) startWarmup() else startPhase()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startWarmup() {
        isWarmup = true
        remainingMs = prefs.warmupSeconds * 1000L
        tvPhase.text = "GET READY"
        tvPhase.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        layoutWorkout.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark))
        tvRepProgress.text = "Workout starts soon"
        speak("Get ready!")
        runTimer(remainingMs, onDone = {
            isWarmup = false
            startPhase()
        })
    }

    private fun startPhase() {
        remainingMs = (if (isWorkPhase) activeSeconds else restSeconds) * 1000L
        updatePhaseUI()
        if (isWorkPhase && prefs.announceStart) speak("Go!")
        if (!isWorkPhase && prefs.announceRest) speak("Rest")
        runTimer(remainingMs)
    }
    private fun runTimer(durationMs: Long, onDone: (() -> Unit)? = null) {
        timer?.cancel()
        timer = object : CountDownTimer(durationMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                tvCountdown.text = formatTime(millisUntilFinished)
                if (!isWarmup) {
                    val phaseDuration = (if (isWorkPhase) activeSeconds else restSeconds) * 1000L
                    val currentElapsed = elapsedMs + (phaseDuration - millisUntilFinished)
                    val pct = ((currentElapsed.toFloat() / totalWorkoutMs) * 100).toInt().coerceIn(0, 100)
                    tvOverallPct.text = "$pct%"
                    progressBar.progress = pct
                    checkMilestones(pct)
                }
            }

            override fun onFinish() {
                if (onDone != null) {
                    onDone()
                } else {
                    elapsedMs += (if (isWorkPhase) activeSeconds else restSeconds) * 1000L
                    advancePhase()
                }
            }
        }.start()
    }

    private fun advancePhase() {
        if (isWorkPhase) {
            if (currentRep >= totalReps) { showComplete(); return }
            isWorkPhase = false
        } else {
            currentRep++
            isWorkPhase = true
        }
        startPhase()
    }

    private fun pause() {
        if (isPaused) return
        isPaused = true
        timer?.cancel()
        // Populate pause stats
        val phaseLabel = if (isWorkPhase) "Work phase" else "Rest phase"
        tvPausedPhase.text = phaseLabel
        tvPausedTimeLeft.text = formatTime(remainingMs) + " remaining"
        tvPausedSet.text = "Set $currentRep of $totalReps"
        layoutPaused.visibility = View.VISIBLE
    }

    private fun resume() {
        if (!isPaused) return
        isPaused = false
        layoutPaused.visibility = View.GONE
        runTimer(remainingMs)
    }

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Workout?")
            .setMessage("Are you sure you want to stop and exit?")
            .setPositiveButton("Yes, Cancel") { _, _ -> finish() }
            .setNegativeButton("Keep Going") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun restartWorkout() {
        elapsedMs = 0L
        milestonesAnnounced.clear()
        currentRep = 1
        isWorkPhase = true
        isWarmup = false
        isPaused = false
        layoutComplete.visibility = View.GONE
        layoutPaused.visibility = View.GONE
        layoutWorkout.visibility = View.VISIBLE
        if (prefs.warmupEnabled) startWarmup() else startPhase()
    }

    private fun updatePhaseUI() {
        if (isWorkPhase) {
            tvPhase.text = "WORK"
            tvPhase.setTextColor(ContextCompat.getColor(this, R.color.work_color))
            layoutWorkout.setBackgroundColor(ContextCompat.getColor(this, R.color.work_bg))
        } else {
            tvPhase.text = "REST"
            tvPhase.setTextColor(ContextCompat.getColor(this, R.color.rest_color))
            layoutWorkout.setBackgroundColor(ContextCompat.getColor(this, R.color.rest_bg))
        }
        tvRepProgress.text = "Set $currentRep / $totalReps"
    }

    private fun checkMilestones(pct: Int) {
        if (!prefs.announceMilestones) return
        listOf(50 to "Halfway done!", 75 to "75 percent complete!", 95 to "Almost there, keep going!")
            .forEach { (threshold, phrase) ->
                if (pct >= threshold && milestonesAnnounced.add(threshold)) speak(phrase)
            }
    }

    private fun showComplete() {
        timer?.cancel()
        speak("Workout complete! Great job!")
        layoutWorkout.visibility = View.GONE
        layoutComplete.visibility = View.VISIBLE
        val s = prefs.totalSeconds()
        tvCompleteTime.text = if (s % 60 == 0) "Total time: ${s / 60}m" else "Total time: ${s / 60}m ${s % 60}s"
    }

    private fun formatTime(ms: Long): String {
        val s = (ms / 1000).toInt()
        return if (s >= 60) "%d:%02d".format(s / 60, s % 60) else "$s"
    }

    // D-pad center → pause/resume
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                when {
                    layoutPaused.visibility == View.VISIBLE -> resume()
                    layoutWorkout.visibility == View.VISIBLE -> pause()
                }
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (layoutPaused.visibility == View.VISIBLE) {
                    confirmCancel()
                } else if (layoutWorkout.visibility == View.VISIBLE) {
                    pause()
                } else {
                    finish()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
