package com.example.quicksnapdiary.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.quicksnapdiary.util.DateUtil
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class ClipInput(
    val file: File,
    val timestamp: Long,
    val note: String,
    val videoRotation: Int = 0
)

@UnstableApi
class VlogComposer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeExport: ActiveExport? = null

    private class ActiveExport(
        val transformer: Transformer,
        val finish: (Boolean, String?) -> Unit,
        var progressJob: Job? = null
    )

    fun cancel() {
        val export = activeExport ?: return
        export.finish(false, "Vlog 生成已取消")
        export.transformer.cancel()
    }

    fun compose(
        clips: List<ClipInput>,
        output: File,
        onProgress: (Float) -> Unit,
        onDone: (Boolean, String?) -> Unit
    ) {
        cancel()
        if (clips.isEmpty()) {
            onDone(false, "没有可合成的视频片段")
            return
        }
        output.parentFile?.mkdirs()

        val mediaItems = clips.map { clip ->
            val mediaItem = MediaItem.fromUri(clip.file.toURI().toString())

            val overlaySettings = OverlaySettings.Builder()
                .setBackgroundFrameAnchor(0f, 0f)
                .setScale(0.55f, 0.55f)
                .setRotationDegrees(clip.videoRotation.toFloat())
                .build()

            val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(
                renderOverlayBitmap(DateUtil.formatTimeHHmm(clip.timestamp), clip.note),
                overlaySettings
            )

            val overlayEffect = OverlayEffect(ImmutableList.of(bitmapOverlay))
            val effects = Effects(
                emptyList(),
                listOf<Effect>(overlayEffect)
            )

            EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()
        }

        val sequence = EditedMediaItemSequence(mediaItems)
        val composition = Composition.Builder(sequence).build()
        var completed = false
        lateinit var export: ActiveExport
        val finish: (Boolean, String?) -> Unit = { success, message ->
            if (!completed) {
                completed = true
                export.progressJob?.cancel()
                if (activeExport === export) activeExport = null
                onDone(success, message)
            }
        }

        val currentTransformer = Transformer.Builder(appContext)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (!completed) {
                        onProgress(1f)
                        finish(true, output.absolutePath)
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    finish(false, exportException.message ?: "视频合成失败")
                }
            })
            .build()

        export = ActiveExport(currentTransformer, finish)
        activeExport = export
        try {
            currentTransformer.start(composition, output.absolutePath)
        } catch (exception: Exception) {
            finish(false, exception.message ?: "无法开始视频合成")
            return
        }
        export.progressJob = scope.launch {
            val progressHolder = ProgressHolder()
            while (!completed) {
                if (currentTransformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(progressHolder.progress / 100f)
                }
                delay(250)
            }
        }
    }

    private fun createOverlay(timestamp: Long, note: String): BitmapOverlay {
        val bitmap = renderOverlayBitmap(
            DateUtil.formatTimeHHmm(timestamp),
            note
        )
        val overlaySettings = OverlaySettings.Builder()
            .setBackgroundFrameAnchor(0f, 0f)
            .setScale(0.55f, 0.55f)
            .build()
        return BitmapOverlay.createStaticBitmapOverlay(bitmap, overlaySettings)
    }

    private fun renderOverlayBitmap(time: String, note: String): Bitmap {
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 96f
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(12f, 0f, 0f, Color.BLACK)
        }
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 56f
            color = Color.WHITE
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }

        val canvasWidth = 900
        val timeBounds = Rect()
        timePaint.getTextBounds(time, 0, time.length, timeBounds)
        val noteLines = if (note.isBlank()) emptyList() else wrapText(note, notePaint, canvasWidth.toFloat() - 40f)

        val lineHeight = notePaint.textSize * 1.3f
        val height = (timePaint.textSize + lineHeight * noteLines.size + 60).toInt()

        val bitmap = Bitmap.createBitmap(
            canvasWidth,
            height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        var y = timePaint.textSize + 20f
        canvas.drawText(time, canvasWidth / 2f, y, timePaint)
        y += lineHeight
        noteLines.forEach { line ->
            canvas.drawText(line, canvasWidth / 2f, y, notePaint)
            y += lineHeight
        }
        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            var line = StringBuilder()
            paragraph.split(' ').forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = StringBuilder(candidate)
                } else {
                    if (line.isNotEmpty()) result.add(line.toString())
                    line = StringBuilder(word)
                }
            }
            if (line.isNotEmpty()) result.add(line.toString())
        }
        return result
    }
}
