package com.example.quicksnapdiary

import android.app.Application
import com.example.quicksnapdiary.data.AppDatabase
import com.example.quicksnapdiary.data.SettingsStore

class DiaryApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
}
