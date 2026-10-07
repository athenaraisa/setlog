package com.example.quicksnapdiary.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.quicksnapdiary.DiaryApp
import com.example.quicksnapdiary.camera.CameraController
import com.example.quicksnapdiary.data.Clip
import com.example.quicksnapdiary.util.DateUtil
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApp
    val lifecycleOwner = LocalLifecycleOwner.current
    val dateKey = remember { DateUtil.todayKey() }
    val settings by app.settings.flow.collectAsState(initial = null)
    val clips by app.db.clipDao().observeByDate(dateKey).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    var recording by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var noteDialogClipId by remember { mutableStateOf<Long?>(null) }
    var noteText by remember { mutableStateOf("") }
    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val cameraController = remember(context, lifecycleOwner) {
        CameraController(context, lifecycleOwner)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    LaunchedEffect(settings?.resolutionP) {
        cameraController.setResolution(settings?.resolutionP ?: 1080)
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermissions) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PreviewView(viewContext).also { view ->
                        previewView = view
                        cameraController.bind(view) {}
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("需要相机和麦克风权限才能拍摄", color = Color.White)
                androidx.compose.material3.Button(
                    onClick = {
                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                    }
                ) {
                    Text("授予权限")
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("快拍日记 · 今天 ${clips.size} 段") },
                    actions = {
                        IconButton(onClick = { navController.navigate("timeline") }) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = "今日时间线")
                        }
                        IconButton(onClick = { navController.navigate("history") }) {
                            Icon(Icons.Filled.History, contentDescription = "历史 Vlog")
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xAA10110F),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE610110F))
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { previewView?.let(cameraController::switchCamera) },
                        enabled = hasPermissions && !recording
                    ) {
                        Icon(
                            Icons.Filled.FlipCameraAndroid,
                            contentDescription = "切换摄像头",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            if (recording) {
                                cameraController.stopNow()
                            } else {
                                val durationSec = settings?.clipDurationSec ?: 3
                                val keepAudio = settings?.keepAudio ?: true
                                val outputDirectory = File(context.filesDir, "clips").apply {
                                    mkdirs()
                                }
                                val outputFile = File(outputDirectory, DateUtil.fileName())
                                val timestamp = System.currentTimeMillis()
                                recording = true
                                cameraController.startRecording(
                                    outputFile,
                                    durationSec * 1000L,
                                    keepAudio
                                ) { success, rotation ->
                                    recording = false
                                    if (success) {
                                        coroutineScope.launch {
                                            val orderIndex = (app.db.clipDao().maxOrder(dateKey) ?: -1) + 1
                                            val id = app.db.clipDao().insert(
                                                Clip(
                                                    dateKey = dateKey,
                                                    timestamp = timestamp,
                                                    filePath = outputFile.absolutePath,
                                                    durationMs = durationSec * 1000L,
                                                    hasAudio = keepAudio,
                                                    orderIndex = orderIndex,
                                                    videoRotation = rotation
                                                )
                                            )
                                            noteText = ""
                                            noteDialogClipId = id
                                        }
                                    } else {
                                        outputFile.delete()
                                    }
                                }
                            }
                        },
                        enabled = hasPermissions,
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                color = if (recording) Color(0xFFFF4D4D) else Color.White,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = if (recording) "停止录制" else "开始录制",
                            tint = if (recording) Color.White else Color(0xFF151612),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(1.dp))
                if (recording) {
                    Text(
                        "正在拍摄",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        noteDialogClipId?.let { clipId ->
            AlertDialog(
                onDismissRequest = { noteDialogClipId = null },
                title = { Text("写点什么") },
                text = {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("记下这一刻…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        maxLines = 12
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            app.db.clipDao().updateNote(clipId, noteText)
                            noteDialogClipId = null
                        }
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { noteDialogClipId = null }) { Text("跳过") }
                }
            )
        }
    }
}
