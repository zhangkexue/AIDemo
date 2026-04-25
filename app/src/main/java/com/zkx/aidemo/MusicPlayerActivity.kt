package com.zkx.aidemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

// UI 颜色常量
private const val DARK_BG_COLOR = 0xFF1A1A2E
private const val DARK_BG_COLOR2 = 0xFF16213E
private const val DISC_BG_COLOR = 0xFF0F3460
private const val ACCENT_COLOR = 0xFFE94560
private const val DISC_ROTATION_DURATION_MS = 8000

class MusicPlayerActivity : ComponentActivity() {

    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicPlayerScreen(
                viewModel = viewModel,
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(viewModel: MusicPlayerViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 音频权限
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadSongs() else viewModel.onPermissionDenied()
    }

    // 蓝牙权限
    val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val btPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms -> if (perms.values.all { it }) viewModel.startScan() }

    // 初始加载本地音乐
    DisposableEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadSongs()
        } else {
            audioPermLauncher.launch(audioPermission)
        }
        onDispose { }
    }

    // 蓝牙广播
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            @Suppress("DEPRECATION")
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        device?.let { viewModel.onBluetoothDeviceFound(it) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> viewModel.onScanFinished()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // 唱片旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "disc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(DISC_ROTATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            BluetoothSheet(
                devices = uiState.btDevices,
                isScanning = uiState.isScanning,
                deviceName = { viewModel.deviceDisplayName(it) },
                onScan = {
                    val hasPerms = btPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPerms) viewModel.startScan() else btPermLauncher.launch(btPermissions)
                }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_music_player)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            if (scaffoldState.bottomSheetState.isVisible) {
                                scaffoldState.bottomSheetState.hide()
                            } else {
                                scaffoldState.bottomSheetState.expand()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(DARK_BG_COLOR))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(DARK_BG_COLOR), Color(DARK_BG_COLOR2))))
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            DiscCover(isPlaying = uiState.isPlaying, rotation = rotation)

            Spacer(Modifier.height(32.dp))

            val currentSong = uiState.songs.getOrNull(uiState.currentIndex)
            Text(
                text = currentSong?.title ?: stringResource(R.string.music_no_song),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = currentSong?.artist.orEmpty(),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            Slider(
                value = uiState.progress,
                onValueChange = { viewModel.seekTo(it) },
                modifier = Modifier.padding(horizontal = 32.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(ACCENT_COLOR)
                )
            )

            Spacer(Modifier.height(16.dp))

            PlayerControls(
                isPlaying = uiState.isPlaying,
                onPrevious = { viewModel.skipPrevious() },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.skipNext() }
            )

            Spacer(Modifier.height(24.dp))

            SongList(
                songs = uiState.songs,
                currentIndex = uiState.currentIndex,
                onSongClick = { viewModel.playSongAt(it) }
            )
        }
    }

    if (uiState.showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionDialog() },
            title = { Text(stringResource(R.string.music_permission_title)) },
            text = { Text(stringResource(R.string.music_permission_msg)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPermissionDialog() }) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }
}

@Composable
private fun DiscCover(isPlaying: Boolean, rotation: Float) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(Color(DISC_BG_COLOR))
            .rotate(if (isPlaying) rotation else 0f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(ACCENT_COLOR))
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun SongList(songs: List<Song>, currentIndex: Int, onSongClick: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.music_empty), color = Color.White.copy(alpha = 0.5f))
                }
            }
        } else {
            itemsIndexed(songs) { idx, song ->
                ListItem(
                    headlineContent = {
                        Text(
                            song.title,
                            color = if (idx == currentIndex) Color(ACCENT_COLOR) else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = { Text(song.artist, color = Color.White.copy(alpha = 0.5f)) },
                    leadingContent = {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSongClick(idx) }
                )
            }
        }
    }
}

@Composable
fun BluetoothSheet(
    devices: List<BluetoothDevice>,
    isScanning: Boolean,
    deviceName: (BluetoothDevice) -> String,
    onScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(DARK_BG_COLOR2))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.music_bt_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(ACCENT_COLOR),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onScan, enabled = !isScanning) {
                    Text(
                        stringResource(if (isScanning) R.string.music_bt_scanning else R.string.music_bt_scan),
                        color = Color(ACCENT_COLOR)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (devices.isEmpty() && !isScanning) {
            Text(
                stringResource(R.string.music_bt_empty),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        devices.forEach { device ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(DISC_BG_COLOR)),
                shape = RoundedCornerShape(8.dp)
            ) {
                ListItem(
                    headlineContent = { Text(deviceName(device), color = Color.White) },
                    supportingContent = { Text(device.address, color = Color.White.copy(alpha = 0.5f)) },
                    leadingContent = {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(ACCENT_COLOR))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
