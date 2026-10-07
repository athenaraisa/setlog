package com.example.quicksnapdiary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.quicksnapdiary.DiaryApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as DiaryApp
    val settings by app.settings.flow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val duration = settings?.clipDurationSec ?: 3
    val keepAudio = settings?.keepAudio ?: true
    val resolution = settings?.resolutionP ?: 1080

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("默认拍摄时长")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 5, 10, 30).forEach { seconds ->
                    FilterChip(
                        selected = duration == seconds,
                        onClick = { coroutineScope.launch { app.settings.setDuration(seconds) } },
                        label = { Text("${seconds}s") }
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("保留原声", modifier = Modifier.weight(1f))
                Switch(
                    checked = keepAudio,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { app.settings.setAudio(enabled) }
                    }
                )
            }

            Text("分辨率")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(720, 1080).forEach { value ->
                    FilterChip(
                        selected = resolution == value,
                        onClick = { coroutineScope.launch { app.settings.setResolution(value) } },
                        label = { Text("${value}p") }
                    )
                }
            }
        }
    }
}
