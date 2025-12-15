package com.subhajitrajak.durare.exercise

import androidx.camera.core.ImageProxy

interface ExerciseFrameAnalyzer {
    fun reset()
    fun analyze(imageProxy: ImageProxy)
    fun onDestroy()
}