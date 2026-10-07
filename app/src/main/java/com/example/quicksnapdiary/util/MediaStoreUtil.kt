package com.example.quicksnapdiary.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object MediaStoreUtil {
    fun saveVideoToGallery(context: Context, file: File, displayName: String): Boolean {
        val resolver = context.contentResolver
        var uri: Uri? = null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/快拍日记"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "快拍日记"
                    )
                    if (!directory.exists() && !directory.mkdirs()) return false
                    put(MediaStore.Video.Media.DATA, File(directory, displayName).absolutePath)
                }

                uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, this)
                val destination = uri ?: return false
                resolver.openOutputStream(destination)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                } ?: run {
                    resolver.delete(destination, null, null)
                    return false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val completedValues = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    resolver.update(destination, completedValues, null, null)
                }
            }
            true
        } catch (_: Exception) {
            uri?.let { resolver.delete(it, null, null) }
            false
        }
    }
}
