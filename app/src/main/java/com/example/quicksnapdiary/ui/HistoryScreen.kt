package com.example.quicksnapdiary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.quicksnapdiary.DiaryApp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as DiaryApp
    val vlogs by app.db.vlogDao().observeAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史 Vlog") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vlogs, key = { it.id }) { vlog ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(vlog.dateKey) },
                        supportingContent = {
                            Text("${vlog.clipCount} 段 · ${vlog.durationMs / 1000} 秒")
                        },
                        trailingContent = { Text(File(vlog.filePath).name) }
                    )
                }
            }
        }
    }
}
