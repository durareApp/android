package com.subhajitrajak.durare.exercise

interface RepCounterListener {
    fun onCountChanged(count: Int)
    fun onStatusChanged(statusRes: Int)
    fun onRepCompleted()
    fun onFaceSizeChanged(percentage: Int) {}
}