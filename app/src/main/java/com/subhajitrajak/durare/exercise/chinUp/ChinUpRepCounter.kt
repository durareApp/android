package com.subhajitrajak.durare.exercise.chinUp

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.subhajitrajak.durare.R

class ChinUpRepCounter(
    private val listener: Listener
) {

    interface Listener {
        fun onCountChanged(count: Int)
        fun onStatusChanged(statusRes: Int)
    }

    private var count = 0
    private var state = State.DOWN

    private var startTimeMs: Long? = null
    private var countingStarted = false

    private val waitTimeMs = 5_000L
    private var barConfirmed = false
    private val offsetPx = 16          // chin over bar margin
    private val wristMarginPx = 12     // both wrists above nose
    private val smoothingAlpha = 0.25f // EMA smoothing
    private var smoothedBarY: Float? = null

    private enum class State { UP, DOWN }

    fun reset() {
        count = 0
        state = State.DOWN
        startTimeMs = null
        countingStarted = false
        barConfirmed = false
        smoothedBarY = null
        listener.onCountChanged(count)
        listener.onStatusChanged(R.string.status_hold_position)
    }

    fun process(pose: Pose) {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE) ?: return
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST) ?: return

        val noseY = nose.position.y

        val leftWristY = leftWrist.position.y
        val rightWristY = rightWrist.position.y

        // Both wrists above nose (bar validation)
        val bothWristsAboveNose =
            leftWristY < noseY - wristMarginPx &&
                    rightWristY < noseY - wristMarginPx

        // Raw bar Y (average wrists)
        val rawBarY = (leftWristY + rightWristY) / 2f

        // EMA smoothing
        smoothedBarY = smoothedBarY?.let {
            smoothingAlpha * rawBarY + (1f - smoothingAlpha) * it
        } ?: rawBarY

        val barY = smoothedBarY!!

        // WAIT PHASE
        if (!countingStarted) {
            val now = System.currentTimeMillis()
            if (startTimeMs == null) startTimeMs = now

            if (bothWristsAboveNose) {
                barConfirmed = true
            }

            val elapsed = now - startTimeMs!!
            if (elapsed >= waitTimeMs && barConfirmed) {
                countingStarted = true
                state = State.DOWN
                listener.onStatusChanged(R.string.status_ready1)
            } else {
                listener.onStatusChanged(R.string.status_hold_position)
            }
            return
        }

        // STATE MACHINE
        // UP: chin clearly above bar
        if (barConfirmed && noseY < barY - offsetPx && state == State.DOWN) {
            state = State.UP
            listener.onStatusChanged(R.string.status_go_up)
        }

        // DOWN: chin clearly below bar AFTER being UP
        else if (barConfirmed && noseY > barY + offsetPx && state == State.UP) {
            count++
            state = State.DOWN
            listener.onCountChanged(count)
            listener.onStatusChanged(R.string.status_great1)
        }
    }
}