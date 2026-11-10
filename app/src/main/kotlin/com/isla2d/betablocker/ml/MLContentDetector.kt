package com.isla2d.betablocker.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("UNUSED_PARAMETER")
class MLContentDetector(context: Context) {

    private val faceDetector = FaceDetection.getClient()

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    companion object {
        const val FACE = "FACE"
        const val PERSON = "PERSON"
        const val HAND = "HAND"
        const val HEAD = "HEAD"
    }

    private suspend fun <T> awaitTask(task: Task<T>): T =
        suspendCoroutine { continuation ->
            task.addOnCompleteListener { completed ->
                when {
                    completed.isCanceled -> {
                        continuation.resumeWithException(
                            CancellationException("Detection cancelled")
                        )
                    }

                    completed.isSuccessful -> {
                        continuation.resume(completed.result)
                    }

                    else -> {
                        continuation.resumeWithException(
                            completed.exception
                                ?: IllegalStateException("Detection failed")
                        )
                    }
                }
            }
        }

    suspend fun detectFaces(bitmap: Bitmap): List<DetectedObject> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = awaitTask(faceDetector.process(image))

            faces.map { fa
