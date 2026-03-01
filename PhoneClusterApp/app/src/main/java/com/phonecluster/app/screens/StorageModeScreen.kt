package com.phonecluster.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phonecluster.app.storage.PreferencesManager
import com.phonecluster.app.utils.heartbeat.HeartbeatManager
import com.phonecluster.app.utils.LocalChunkItem
import com.phonecluster.app.utils.LocalChunkStore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageModeScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val deviceId = PreferencesManager.getDeviceId(context) ?: -1

    // Local chunks state
    var localChunks by remember { mutableStateOf<List<LocalChunkItem>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshList() {
        localChunks = LocalChunkStore.listChunks(context)
    }

    LaunchedEffect(Unit) {
        if (deviceId != -1) {
            HeartbeatManager.start(
                serverBaseUrl = "http://10.0.2.2:8000",
                deviceId = deviceId
            )
        }
        // initial load
        refreshList()
    }

    // OPTIONAL: auto-refresh every 2 seconds so new chunks appear automatically
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            refreshList()
        }
    }

    DisposableEffect(Unit) {
        onDispose { HeartbeatManager.stop() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Mode") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {

                // Device ID badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Device ID: $deviceId",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stored chunks (${localChunks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Button(
                        onClick = {
                            isRefreshing = true
                            refreshList()
                            isRefreshing = false
                        }
                    ) {
                        Text(if (isRefreshing) "Refreshing..." else "Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (localChunks.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Waiting for chunks...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Folder: ${LocalChunkStore.chunksDir(context).absolutePath}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(localChunks) { item ->
                            ChunkFileCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChunkFileCard(item: LocalChunkItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Size: ${LocalChunkStore.formatBytes(item.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Modified: ${LocalChunkStore.formatTime(item.lastModified)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}