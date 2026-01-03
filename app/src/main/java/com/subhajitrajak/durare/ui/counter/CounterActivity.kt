package com.subhajitrajak.durare.ui.counter

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.subhajitrajak.durare.R
import com.subhajitrajak.durare.exercise.chinUp.ChinUpPoseAnalyzer
import com.subhajitrajak.durare.exercise.ExerciseFrameAnalyzer
import com.subhajitrajak.durare.exercise.ExerciseType
import com.subhajitrajak.durare.exercise.pushUp.PushUpFaceAnalyzer
import com.subhajitrajak.durare.data.models.DailyPushStats
import com.subhajitrajak.durare.data.models.DailyWorkoutStats
import com.subhajitrajak.durare.data.repositories.StatsRepository
import com.subhajitrajak.durare.databinding.ActivityCounterBinding
import com.subhajitrajak.durare.exercise.BaseRepCounter
import com.subhajitrajak.durare.exercise.RepCounterListener
import com.subhajitrajak.durare.ui.shareStats.ShareStatsActivity
import com.subhajitrajak.durare.exercise.chinUp.ChinUpRepCounter
import com.subhajitrajak.durare.utils.Constants.DATE_FORMAT
import com.subhajitrajak.durare.utils.Preferences
import com.subhajitrajak.durare.exercise.pushUp.PushUpRepCounter
import com.subhajitrajak.durare.utils.log
import com.subhajitrajak.durare.utils.reminderUtils.RestAlarmReceiver
import com.subhajitrajak.durare.utils.showToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class CounterActivity : AppCompatActivity(), RepCounterListener {

    private val binding: ActivityCounterBinding by lazy {
        ActivityCounterBinding.inflate(layoutInflater)
    }
    
    private val prefs: Preferences by lazy { 
        Preferences.getInstance(this) 
    }

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var exerciseFrameAnalyzer: ExerciseFrameAnalyzer
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private var showCameraCardSwitch: Boolean = false
    private var isSoundFeedbackEnabled: Boolean = false
    private var isVibrationFeedbackEnabled: Boolean = false
    private lateinit var exerciseType: ExerciseType
    private var repCounter: BaseRepCounter? = null

    // Session/rep state
    private var totalReps: Int = 3
    private var currentRep: Int = 1
    private var currentRepCount: Int = 0
    private var completedRepsPushUpsSum: Int = 0
    private val totalPushUps: Int
        get() = completedRepsPushUpsSum + currentRepCount

    // Timer state
    private var isPaused: Boolean = false
    private var isResting: Boolean = false
    private val uiHandler: Handler = Handler(Looper.getMainLooper())
    private var activeAccumulatedMs: Long = 0L
    private var restAccumulatedMs: Long = 0L
    private var lastTickStartMs: Long = 0L
    private var restRemainingMs: Long = 0L
    private var customRestMs: Long = 5000L
    private val tickRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (!isPaused) {
                val delta = now - lastTickStartMs
                if (isResting) {
                    restAccumulatedMs += delta
                    val endTs = Preferences.getInstance(this@CounterActivity).getRestEndTimestamp()
                    if (isResting && endTs > 0) {
                        restRemainingMs = max(0, endTs - System.currentTimeMillis())
                        updateCountdownNotification(restRemainingMs)
                    }
                    if (restRemainingMs <= 0) {
                        exitRestAndStartNextRep()
                    }
                } else {
                    activeAccumulatedMs += delta
                }
                updateTimerText()
            }
            lastTickStartMs = now
            uiHandler.postDelayed(this, 1000L)
        }
    }

    // Repo
    private val statsRepository: StatsRepository by lazy { StatsRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get dynamic values from intent extras
        totalReps = intent.getIntExtra(Preferences.KEY_TOTAL_REPS, 3)
        customRestMs = intent.getLongExtra(Preferences.KEY_REST_TIME, 5000L)

        // keeps screen on during push-ups
        window.decorView.keepScreenOn = true

        binding.backButton.setOnClickListener {
            finish()
        }

        exerciseType = ExerciseType.valueOf(
            intent.getStringExtra("exercise_type") ?: ExerciseType.PUSH_UP.name
        )

        exerciseFrameAnalyzer = when (exerciseType) {

            ExerciseType.PUSH_UP -> {
                val pushUpCounter = PushUpRepCounter(this, prefs)
                repCounter = pushUpCounter

                val faceOptions = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setMinFaceSize(0.1f)
                    .build()

                PushUpFaceAnalyzer(
                    FaceDetection.getClient(faceOptions),
                    pushUpCounter
                )
            }

            ExerciseType.CHIN_UP -> {
                val chinUpCounter  = ChinUpRepCounter(this)
                repCounter = chinUpCounter

                ChinUpPoseAnalyzer(
                    PoseDetection.getClient(
                        PoseDetectorOptions.Builder()
                            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                            .build()
                    ),
                    chinUpCounter
                )
            }
        }

        // Init helpers
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // initializing preferences
        showCameraCardSwitch = prefs.isCameraCardEnabled()
        isSoundFeedbackEnabled = prefs.isSoundFeedbackEnabled()
        isVibrationFeedbackEnabled = prefs.isVibrationFeedbackEnabled()

        // Camera card
        binding.cameraCard.visibility = if (showCameraCardSwitch) View.VISIBLE else View.INVISIBLE

        binding.resetButton.setOnClickListener { exerciseFrameAnalyzer.reset() }
        binding.playPause.setOnClickListener { togglePause() }
        binding.done.setOnClickListener { onDoneClicked() }
        binding.exitRest.setOnClickListener { onExitRestClick() }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Initialize UI
        updateRepTitle()
        updateTotalCount()
        updateTimerText()
        startUiTicker()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build().also { it.surfaceProvider = binding.viewFinder.surfaceProvider }

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ExerciseAnalyzer()) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                log("Camera start failed - ${exc.message}")
                showToast(this, "Camera failed to start")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startUiTicker() {
        lastTickStartMs = System.currentTimeMillis()
        uiHandler.removeCallbacks(tickRunnable)
        uiHandler.post(tickRunnable)
    }

    private fun togglePause() {
        isPaused = !isPaused
        val iconRes = if (isPaused) R.drawable.play else R.drawable.pause
        binding.playPauseIcon.setImageResource(iconRes)
    }

    private fun updateRepTitle() {
        binding.repCount.text = getString(R.string.rep_1).replace("1", "$currentRep/$totalReps")
    }

    private fun updateTotalCount() {
        binding.totalCountText.text = totalPushUps.toString()
    }

    private fun updateTimerText() {
        val displayMs = if (isResting) restRemainingMs else activeAccumulatedMs
        binding.timerText.text = formatDuration(displayMs)
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun onDoneClicked() {
        if (isResting) return
        binding.done.isEnabled = false
        // Finish current rep: move currentRepCount into completed sum, then reset per-rep counter
        completedRepsPushUpsSum += currentRepCount
        currentRepCount = 0
        binding.pushUpCount.text = currentRepCount.toString()
        updateTotalCount()

        if (currentRep >= totalReps) {
            completeSessionAndExit()
        } else {
            enterRest()
        }
    }

    private fun enterRest() {
        binding.done.isEnabled = true
        isResting = true
        restRemainingMs = customRestMs
        // Prepare for next rep: freeze counting during rest
        binding.pushUpCount.visibility = View.GONE
        binding.statusText.visibility = View.GONE
        binding.cameraCard.visibility = View.GONE
        binding.repCount.text = getString(R.string.rest_time)
        binding.resetButton.visibility = View.GONE
        binding.done.visibility = View.GONE
        binding.exitRest.visibility = View.VISIBLE

        scheduleRestAlarm(restRemainingMs)
    }

    private fun scheduleRestAlarm(durationMs: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Do NOT crash; just fallback to normal alarm
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + durationMs,
                    PendingIntent.getBroadcast(
                        this,
                        RestAlarmReceiver.REST_ALARM_REQUEST_CODE,
                        Intent(this, RestAlarmReceiver::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                return
            }
        }

        val intent = Intent(this, RestAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            RestAlarmReceiver.REST_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + durationMs

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        prefs.setRestEndTimestamp(triggerAtMillis)
    }

    private fun updateCountdownNotification(ms: Long) {
        val hasPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true

        if (!hasPermission) return

        val text = formatDuration(ms)

        val notif = NotificationCompat.Builder(this, RestAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.timer)
            .setContentTitle("Resting...")
            .setContentText("Time remaining: $text")
            .setColor(getColor(R.color.primary))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(this).notify(RestAlarmReceiver.REST_COUNTDOWN_NOTIFICATION_ID, notif)
    }

    private fun clearCountdownNotification() {
        val nm = NotificationManagerCompat.from(this)
        nm.cancel(RestAlarmReceiver.REST_COUNTDOWN_NOTIFICATION_ID)
    }

    private fun onExitRestClick() {
        // End the rest early
        restRemainingMs = 0L

        // Transition into the next rep (or complete session if last rep)
        exitRestAndStartNextRep()

        // Update timer text so UI reflects immediate change
        updateTimerText()
    }

    private fun exitRestAndStartNextRep() {
        cancelRestAlarm()
        prefs.setRestEndTimestamp(0)
        clearCountdownNotification()

        if (isSoundFeedbackEnabled) {
            try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200) } catch (_: Exception) {}
        }

        if (isVibrationFeedbackEnabled) {
            try { vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}
        }

        binding.pushUpCount.visibility = View.VISIBLE
        binding.statusText.visibility = View.VISIBLE
        binding.cameraCard.visibility = View.VISIBLE
        binding.resetButton.visibility = View.VISIBLE
        binding.done.visibility = View.VISIBLE
        binding.exitRest.visibility = View.GONE

        isResting = false
        currentRep++
        if (currentRep > totalReps) {
            completeSessionAndExit()
            return
        }
        // Reset per-rep counter for UI but keep totalPushUps
        currentRepCount = 0
        exerciseFrameAnalyzer.reset()
        updateRepTitle()
        updateTotalCount()
    }

    private fun cancelRestAlarm() {
        val intent = Intent(this, RestAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            RestAlarmReceiver.REST_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun completeSessionAndExit() {
        // Persist stats
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val date = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
        val totalDurationMs  = repCounter?.cumulativeDurationMs ?: 0L
        val averageWorkoutDurationMs = if (totalPushUps > 0) totalDurationMs  / totalPushUps else 0L

        if (uid == null) {
            showToast(this, "Couldn't save stats, please login again.")
            finish()
            return
        }

        when (exerciseType) {
            ExerciseType.PUSH_UP -> {
                val stats = DailyPushStats(
                    date = date,
                    totalReps = totalReps,
                    totalPushups = totalPushUps,
                    totalActiveTimeMs = activeAccumulatedMs,
                    averagePushDurationMs = averageWorkoutDurationMs,
                    totalRestTimeMs = restAccumulatedMs
                )

                statsRepository.saveOrAccumulateDaily(uid, date, stats)
                    .addOnSuccessListener {
                        navigateToShareStats(stats)
                    }
                    .addOnFailureListener { err ->
                        log("Failed to save stats - ${err.message}")
                        showToast(this, "Failed to save stats")
                        finish()
                    }
            }
            ExerciseType.CHIN_UP -> {
                val stats = DailyWorkoutStats(
                    date = date,
                    sets = totalReps,
                    reps = totalPushUps,
                    activeTimeMs = activeAccumulatedMs,
                    averageRepDurationMs = averageWorkoutDurationMs,
                    restTimeMs = restAccumulatedMs
                )

                statsRepository.saveDailyChinUpStats(uid, date, stats)
                    .addOnSuccessListener {
                        finish()
                    }
                    .addOnFailureListener { err ->
                        log("Failed to save stats - ${err.message}")
                        showToast(this, "Failed to save stats")
                        finish()
                    }
            }
        }
    }

    private fun navigateToShareStats(stats: DailyPushStats) {
        val pushUps = stats.totalPushups.toString()
        val timeMinutes = stats.totalActiveTimeMs / 1000 / 60
        val timeSeconds = (stats.totalActiveTimeMs / 1000) % 60
        val time = "${timeMinutes}m ${timeSeconds}s"

        val restMinutes = stats.totalRestTimeMs / 1000 / 60
        val restSeconds = (stats.totalRestTimeMs / 1000) % 60
        val rest = "${restMinutes}m ${restSeconds}s"

        val intent = Intent(this, ShareStatsActivity::class.java).apply {
            putExtra(ShareStatsActivity.EXTRA_PUSH_UPS, pushUps)
            putExtra(ShareStatsActivity.EXTRA_TIME, time)
            putExtra(ShareStatsActivity.EXTRA_REST, rest)
        }
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        } else {
            binding.statusText.text = getString(R.string.status_camera_permission)
        }
    }

    private inner class ExerciseAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            if (isPaused || isResting) {
                imageProxy.close()
                return
            }
            exerciseFrameAnalyzer.analyze(imageProxy)
        }
    }

    override fun onCountChanged(count: Int) {
        if (isResting || isPaused) return
        // count is per detector session; map it to current rep count
        currentRepCount = count
        binding.pushUpCount.text = currentRepCount.toString()
        updateTotalCount()
    }

    override fun onStatusChanged(statusRes: Int) {
        binding.statusText.text = getString(statusRes)
    }

    override fun onRepCompleted() {
        if (isResting || isPaused) return

        if (isSoundFeedbackEnabled) {
            try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120) } catch (_: Exception) {}
        }

        if (isVibrationFeedbackEnabled) {
            try { vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}
        }
    }

    override fun onFaceSizeChanged(percentage: Int) {
        binding.faceSizeText.text = getString(R.string.face_size_format, percentage)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        exerciseFrameAnalyzer.onDestroy()
        toneGenerator?.release()
        window.decorView.keepScreenOn = false
        uiHandler.removeCallbacksAndMessages(null)
        cancelRestAlarm()
        clearCountdownNotification()
    }
}