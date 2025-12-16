package com.subhajitrajak.durare.exercise.pushUp

import com.subhajitrajak.durare.R
import com.subhajitrajak.durare.exercise.BaseRepCounter
import com.subhajitrajak.durare.exercise.RepCounterListener
import com.subhajitrajak.durare.utils.Preferences
import kotlin.math.max
import kotlin.math.min

class PushUpRepCounter(
    listener: RepCounterListener,
    preferences: Preferences
) : BaseRepCounter(listener) {

    private var isInDownPosition = false
    private var smoothedFacePercentage = 0f

    private var consecutiveDownFrames = 0
    private var consecutiveUpFrames = 0
    private var consecutiveNoFaceFrames = 0

    private val downThreshold: Float = preferences.getDownThreshold()
    private val upThreshold: Float = preferences.getUpThreshold()

    companion object {
        private const val FRAME_THRESHOLD = 3
        private const val LOST_FACE_GRACE_FRAMES = 5
        private const val SMOOTHING_ALPHA = 0.25f
    }

    override fun reset() {
        super.reset()
        isInDownPosition = false
        smoothedFacePercentage = 0f
        consecutiveDownFrames = 0
        consecutiveUpFrames = 0
        consecutiveNoFaceFrames = 0
    }

    fun process(faceArea: Int, imageArea: Int) {
        consecutiveNoFaceFrames = 0

        // Start tracking time as soon as a face is detected
        startDurationTracking()

        val rawPercentage = (faceArea.toFloat() / imageArea * 100f)
        smoothedFacePercentage =
            if (smoothedFacePercentage == 0f) rawPercentage
            else SMOOTHING_ALPHA * rawPercentage + (1 - SMOOTHING_ALPHA) * smoothedFacePercentage

        val clampedDisplay = min(100, max(0, smoothedFacePercentage.toInt()))
        listener.onFaceSizeChanged(clampedDisplay)

        when {
            smoothedFacePercentage > downThreshold -> {
                consecutiveDownFrames++
                consecutiveUpFrames = 0
                if (consecutiveDownFrames >= FRAME_THRESHOLD && !isInDownPosition) {
                    isInDownPosition = true
                    listener.onStatusChanged(R.string.status_go_up)
                }
            }
            smoothedFacePercentage > upThreshold -> {
                consecutiveUpFrames++
                consecutiveDownFrames = 0
                if (consecutiveUpFrames >= FRAME_THRESHOLD && isInDownPosition) {
                    isInDownPosition = false
                    listener.onStatusChanged(R.string.status_great)
                    incrementRep() // Uses Base Class logic
                } else if (consecutiveUpFrames >= FRAME_THRESHOLD) {
                    listener.onStatusChanged(R.string.status_ready_pushup)
                }
            }
            else -> {
                consecutiveDownFrames = 0
                consecutiveUpFrames = 0
                listener.onStatusChanged(R.string.status_move_closer)
            }
        }
    }

    fun onNoFace() {
        consecutiveNoFaceFrames++
        if (consecutiveNoFaceFrames >= LOST_FACE_GRACE_FRAMES) {
            listener.onStatusChanged(R.string.status_no_face)
            consecutiveDownFrames = 0
            consecutiveUpFrames = 0
        } else {
            listener.onStatusChanged(R.string.status_no_face)
        }
        listener.onFaceSizeChanged(0)
    }
}