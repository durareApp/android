package com.subhajitrajak.durare.exercise.chinUp

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetector
import com.subhajitrajak.durare.exercise.ExerciseFrameAnalyzer

class ChinUpPoseAnalyzer(
    private val poseDetector: PoseDetector,
    private val detector: ChinUpRepCounter
) : ExerciseFrameAnalyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close(); return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                detector.process(pose)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun reset() = detector.reset()

    override fun onDestroy() = poseDetector.close()
}