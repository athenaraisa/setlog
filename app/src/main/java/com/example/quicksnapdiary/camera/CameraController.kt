package com.example.quicksnapdiary.camera

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var stopRunnable: Runnable? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var resolutionP = 1080
    private var previewView: PreviewView? = null
    private var readyCallback: (() -> Unit)? = null

    private fun displayRotationToDegrees(rotation: Int): Int = when (rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }

    private fun currentVideoRotationDegrees(): Int =
        previewView?.display?.rotation?.let(::displayRotationToDegrees) ?: 0

    fun setResolution(resolutionP: Int) {
        if (this.resolutionP == resolutionP) return
        this.resolutionP = resolutionP
        if (cameraProvider != null) bindUseCases()
    }

    fun bind(previewView: PreviewView, onReady: () -> Unit) {
        this.previewView = previewView
        readyCallback = onReady
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            bindUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera(previewView: PreviewView) {
        this.previewView = previewView
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindUseCases()
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        file: File,
        durationMs: Long,
        withAudio: Boolean,
        onFinish: (Boolean, Int) -> Unit
    ) {
        val capture = videoCapture
        if (capture == null || recording != null) {
            onFinish(false, 0)
            return
        }

        val rotation = currentVideoRotationDegrees()

        val outputOptions = FileOutputOptions.Builder(file).build()
        val pendingRecording = capture.output.prepareRecording(context, outputOptions)
        val configuredRecording = if (withAudio) pendingRecording.withAudioEnabled() else pendingRecording
        recording = configuredRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                cancelStopTask()
                recording = null
                onFinish(!event.hasError(), rotation)
            }
        }

        val timeoutTask = Runnable { recording?.stop() }
        stopRunnable = timeoutTask
        mainHandler.postDelayed(timeoutTask, durationMs.coerceAtLeast(1L))
    }

    fun stopNow() {
        cancelStopTask()
        recording?.stop()
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        val view = previewView ?: return
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview = Preview.Builder()
            .setTargetRotation(view.display?.rotation ?: Surface.ROTATION_0)
            .build()
            .also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    if (resolutionP >= 1080) {
                        listOf(Quality.FHD, Quality.HD, Quality.SD)
                    } else {
                        listOf(Quality.HD, Quality.SD)
                    },
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .build()
        val capture = VideoCapture.withOutput(recorder)
        capture.targetRotation = view.display?.rotation ?: Surface.ROTATION_0

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            videoCapture = capture
            readyCallback?.invoke()
        } catch (_: Exception) {
            videoCapture = null
        }
    }

    private fun cancelStopTask() {
        stopRunnable?.let(mainHandler::removeCallbacks)
        stopRunnable = null
    }
}
