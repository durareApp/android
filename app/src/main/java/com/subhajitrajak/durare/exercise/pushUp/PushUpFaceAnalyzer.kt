package com.subhajitrajak.durare.exercise.pushUp

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.subhajitrajak.durare.exercise.ExerciseFrameAnalyzer

class PushUpFaceAnalyzer(
    private val faceDetector: FaceDetector,
    private val detector: PushUpRepCounter
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

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val faceArea =
                        face.boundingBox.width() * face.boundingBox.height()
                    val imageArea =
                        imageProxy.width * imageProxy.height
                    detector.process(faceArea, imageArea)
                } else {
                    detector.onNoFace()
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun reset() = detector.reset()

    override fun onDestroy() = faceDetector.close()
}