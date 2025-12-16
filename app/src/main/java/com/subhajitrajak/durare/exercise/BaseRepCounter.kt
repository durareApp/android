package com.subhajitrajak.durare.exercise

import com.subhajitrajak.durare.R

abstract class BaseRepCounter(
    protected val listener: RepCounterListener
) {
    var count = 0
        protected set

    var cumulativeDurationMs = 0L
        protected set
    private var lastRepTimeMs = 0L

    open fun reset() {
        count = 0
        cumulativeDurationMs = 0L
        lastRepTimeMs = 0L
        listener.onCountChanged(0)
        listener.onStatusChanged(R.string.status_ready)
        listener.onFaceSizeChanged(0)
    }

    // Call this from subclasses when a rep is finished
    protected fun incrementRep() {
        count++

        val now = System.currentTimeMillis()
        if (lastRepTimeMs != 0L) {
            cumulativeDurationMs += (now - lastRepTimeMs)
        }
        lastRepTimeMs = now

        listener.onCountChanged(count)
        listener.onRepCompleted()
    }

    // Start the timer when the user gets into "Ready" position
    protected fun startDurationTracking() {
        if (lastRepTimeMs == 0L) {
            lastRepTimeMs = System.currentTimeMillis()
        }
    }
}