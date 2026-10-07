package com.example.quicksnapdiary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.example.quicksnapdiary.DiaryApp
import com.example.quicksnapdiary.data.Clip
import com.example.quicksnapdiary.data.Vlog
import com.example.quicksnapdiary.util.DateUtil
import com.example.quicksnapdiary.util.MediaStoreUtil
import com.example.quicksnapdiary.video.ClipInput
import com.example.quicksnapdiary.video.VlogComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun TimelineScreen(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApp
    val dateKey = remember { DateUtil.todayKey() }
    val clips by app.db.clipDao().observeByDate(dateKey).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val composer = remember(context) { VlogComposer(context) }
    DisposableEffect(composer) {
        onDispose { composer.cancel() }
    }
    var progress by remember { mutableStateOf<Float?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日时间线") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                progress?.let { value ->
                    LinearProgressIndicator(progress = value.coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
                    Text("生成中 ${(value * 100).toInt()}%", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                statusMessage?.let { Text(it, modifier = Modifier.align(Alignment.CenterHorizontally)) }
                Button(
                    onClick = {
                        val output = File(context.filesDir, DateUtil.vlogName(dateKey))
                        progress = 0f
                        statusMessage = null
                        composer.compose(
                            clips = clips.map {
                                ClipInput(
                                    file = File(it.filePath),
                                    timestamp = it.timestamp,
                                    note = it.note,
                                    videoRotation = it.videoRotation
                                )
                            },
                            output = output,
                            onProgress = { progress = it }
                        ) { success, result ->
                            if (success && result != null) {
                                coroutineScope.launch {
                                    val saved = withContext(Dispatchers.IO) {
                                        MediaStoreUtil.saveVideoToGallery(
                                            context,
                                            File(result),
                                            "快拍日记_${dateKey.replace("-", "")}.mp4"
                                        )
                                    }
                                    if (saved) {
                                        app.db.vlogDao().insert(
                                            Vlog(
                                                dateKey = dateKey,
                                                filePath = result,
                                                clipCount = clips.size,
                                                durationMs = clips.sumOf { it.durationMs },
                                                createdAt = System.currentTimeMillis()
                                            )
                                        )
                                        statusMessage = "已保存到相册"
                                    } else {
                                        statusMessage = "保存到相册失败"
                                    }
                                    progress = null
                                }
                            } else {
                                statusMessage = result ?: "Vlog 生成失败"
                                progress = null
                            }
                        }
                    },
                    enabled = clips.isNotEmpty() && progress == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (progress != null) "生成中 ${(progress!! * 100).toInt()}%" else "生成今日 Vlog")
                }
            }
        }
    ) { contentPadding ->
        if (clips.isEmpty()) {
            Text(
                "今天还没有片段",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .wrapContentSize(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clips, key = { it.id }) { clip ->
                    ClipCard(clip) {
                        coroutineScope.launch {
                            File(clip.filePath).delete()
                            app.db.clipDao().delete(clip)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipCard(clip: Clip, onDelete: () -> Unit) {
    val context = LocalContext.current
    val player = remember(clip.filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(File(clip.filePath).toURI().toString()))
            prepare()
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { rotationZ = clip.videoRotation.toFloat() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = DateUtil.formatTimeHHmm(clip.timestamp),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 10f))
                )
                if (clip.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = clip.note,
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 8f)),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color.White)
            }
        }
    }
}
