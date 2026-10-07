package com.example.quicksnapdiary.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtil {
    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun fileName(): String = SimpleDateFormat(
        "'clip_'yyyyMMdd_HHmmss_SSS'.mp4'",
        Locale.US
    ).format(Date())

    fun vlogName(dateKey: String): String = "vlog_${dateKey.replace("-", "")}.mp4"

    fun formatTimeHHmm(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
