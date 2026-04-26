package com.zkx.aidemo.entertainment.music

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zkx.aidemo.R

// UI 颜色常量
private const val DARK_BG_COLOR = 0xFF1A1A2E
private const val DARK_BG_COLOR2 = 0xFF16213E
private const val DISC_BG_COLOR = 0xFF0F3460
private const val ACCENT_COLOR = 0xFFE94560
private const val DISC_ROTATION_DURATION_MS = 8000

/** A2DP 连接状态变化广播 Action（隐藏 API，使用字符串常量） */
private const val ACTION_A2DP_CONNECTION_STATE_CHANGED =
    "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED"

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
    val btSheetState = rememberModalBottomSheetState()
    var showBtSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

    // 蓝牙广播：扫描 + 配对状态 + A2DP 连接状态
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
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                        device?.let { viewModel.onBondStateChanged(it, bondState) }
                    }
                    ACTION_A2DP_CONNECTION_STATE_CHANGED -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                        device?.let { viewModel.onA2dpStateChanged(it, state) }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(ACTION_A2DP_CONNECTION_STATE_CHANGED)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_music_player)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showBtSheet = true }) {
                        Icon(
                            if (uiState.btConnectionState == BtConnectionState.CONNECTED)
                                Icons.Default.BluetoothConnected
                            else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (uiState.btConnectionState == BtConnectionState.CONNECTED)
                                Color(ACCENT_COLOR) else Color.White
                        )
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

            // 蓝牙连接状态提示
            if (uiState.btConnectionState == BtConnectionState.CONNECTED && uiState.connectedDevice != null) {
                Spacer(Modifier.height(8.dp))
                val modeText = if (uiState.btConnectionMode == BtConnectionMode.SOURCE) {
                    stringResource(R.string.music_bt_connected_output)
                } else {
                    stringResource(R.string.music_bt_connected_input)
                }
                val deviceLabel = viewModel.deviceDisplayName(uiState.connectedDevice!!).ifEmpty {
                    stringResource(R.string.music_bt_unknown)
                }
                Text(
                    text = "$deviceLabel · $modeText",
                    color = Color(ACCENT_COLOR),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

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

    // 蓝牙设备底部弹窗
    if (showBtSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBtSheet = false },
            sheetState = btSheetState,
            containerColor = Color(DARK_BG_COLOR2)
        ) {
            BluetoothSheet(
                viewModel = viewModel,
                uiState = uiState,
                onScan = {
                    val hasPerms = btPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPerms) viewModel.startScan() else btPermLauncher.launch(btPermissions)
                },
                onDeviceClick = { viewModel.connectDevice(it) },
                onDisconnect = { viewModel.disconnectDevice() }
            )
        }
    }

    // 权限拒绝对话框
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

    // 蓝牙未开启对话框
    if (uiState.showBtDisabledDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBtDisabledDialog() },
            title = { Text(stringResource(R.string.music_bt_title)) },
            text = { Text(stringResource(R.string.music_bt_disabled)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissBtDisabledDialog()
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }) {
                    Text(stringResource(R.string.music_bt_go_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBtDisabledDialog() }) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }

    // A2DP Sink 不支持对话框
    if (uiState.showSinkUnsupportedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSinkUnsupportedDialog() },
            title = { Text(stringResource(R.string.music_bt_sink_unsupported_title)) },
            text = { Text(stringResource(R.string.music_bt_sink_unsupported_msg)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSinkUnsupportedDialog() }) {
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

/** 获取设备类型对应的图标 */
private fun deviceTypeIcon(deviceType: BtDeviceType): ImageVector = when (deviceType) {
    BtDeviceType.HEADSET -> Icons.Default.Headset
    BtDeviceType.PHONE -> Icons.Default.PhoneAndroid
    BtDeviceType.OTHER -> Icons.Default.Bluetooth
}

/** 获取设备类型显示名称 */
@Composable
private fun deviceTypeLabel(deviceType: BtDeviceType): String = when (deviceType) {
    BtDeviceType.HEADSET -> stringResource(R.string.music_bt_device_headset)
    BtDeviceType.PHONE -> stringResource(R.string.music_bt_device_phone)
    BtDeviceType.OTHER -> stringResource(R.string.music_bt_device_other)
}

/** 获取连接模式显示文本 */
@Composable
private fun connectionModeText(mode: BtConnectionMode): String = when (mode) {
    BtConnectionMode.SOURCE -> stringResource(R.string.music_bt_mode_output)
    BtConnectionMode.SINK -> stringResource(R.string.music_bt_mode_input)
}

@Composable
fun BluetoothSheet(
    viewModel: MusicPlayerViewModel,
    uiState: MusicUiState,
    onScan: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(DARK_BG_COLOR2))
            .padding(16.dp)
    ) {
        // 标题栏 + 扫描按钮
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
                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(ACCENT_COLOR),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onScan, enabled = !uiState.isScanning) {
                    Text(
                        stringResource(if (uiState.isScanning) R.string.music_bt_scanning else R.string.music_bt_scan),
                        color = Color(ACCENT_COLOR)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 我的手机（从已配对设备自动识别）
        val myPhone = uiState.myPhoneDevice
        if (myPhone != null) {
            val isMyPhoneConnected = uiState.connectedDevice == myPhone && uiState.btConnectionState == BtConnectionState.CONNECTED
            val myPhoneName = viewModel.deviceDisplayName(myPhone).ifEmpty { stringResource(R.string.music_bt_my_phone) }
            val statusText = buildString {
                append(myPhone.address)
                append(" · ")
                if (uiState.myPhoneFound) {
                    append(stringResource(R.string.music_bt_my_phone_found))
                } else {
                    append(stringResource(R.string.music_bt_my_phone_not_found))
                }
                append(" · ")
                if (uiState.myPhoneBonded) {
                    append(stringResource(R.string.music_bt_my_phone_bonded))
                } else {
                    append(stringResource(R.string.music_bt_my_phone_not_bonded))
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(enabled = !isMyPhoneConnected) { onDeviceClick(myPhone) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isMyPhoneConnected) Color(ACCENT_COLOR).copy(alpha = 0.2f) else Color(DISC_BG_COLOR)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            "${stringResource(R.string.music_bt_my_phone)} · $myPhoneName",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text(statusText, color = Color.White.copy(alpha = 0.6f))
                    },
                    leadingContent = {
                        Icon(
                            if (isMyPhoneConnected) Icons.Default.BluetoothConnected else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (isMyPhoneConnected) Color(ACCENT_COLOR) else Color(0xFF4FC3F7)
                        )
                    },
                    trailingContent = if (isMyPhoneConnected) {
                        {
                            TextButton(onClick = onDisconnect) {
                                Text(
                                    stringResource(R.string.music_bt_disconnect),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else null,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        // 已连接设备（非我的手机）
        if (uiState.connectedDevice != null && uiState.btConnectionState == BtConnectionState.CONNECTED
            && uiState.connectedDevice != myPhone
        ) {
            val device = uiState.connectedDevice!!
            val name = viewModel.deviceDisplayName(device).ifEmpty { stringResource(R.string.music_bt_unknown) }
            val modeText = connectionModeText(uiState.btConnectionMode)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(ACCENT_COLOR).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                ListItem(
                    headlineContent = {
                        Text(name, color = Color.White)
                    },
                    supportingContent = {
                        Text(
                            "${device.address} · $modeText",
                            color = Color(ACCENT_COLOR)
                        )
                    },
                    leadingContent = {
                        Icon(deviceTypeIcon(uiState.btDeviceType), contentDescription = null, tint = Color(ACCENT_COLOR))
                    },
                    trailingContent = {
                        TextButton(onClick = onDisconnect) {
                            Text(
                                stringResource(R.string.music_bt_disconnect),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onDisconnect() }
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // 连接中提示
        if (uiState.btConnectionState == BtConnectionState.CONNECTING && uiState.connectedDevice != null) {
            val device = uiState.connectedDevice!!
            val name = viewModel.deviceDisplayName(device).ifEmpty { stringResource(R.string.music_bt_unknown) }
            val modeText = if (uiState.btConnectionMode == BtConnectionMode.SOURCE) {
                stringResource(R.string.music_bt_connecting_output)
            } else {
                stringResource(R.string.music_bt_connecting_input)
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(DISC_BG_COLOR)),
                shape = RoundedCornerShape(8.dp)
            ) {
                ListItem(
                    headlineContent = { Text(name, color = Color.White) },
                    supportingContent = {
                        Text(
                            "${device.address} · $modeText",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    leadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(ACCENT_COLOR),
                            strokeWidth = 2.dp
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // 连接失败提示
        if (uiState.connectedDevice != null && (uiState.btConnectionState == BtConnectionState.FAILED
                    || uiState.btConnectionState == BtConnectionState.PAIR_FAILED
                    || uiState.btConnectionState == BtConnectionState.A2DP_FAILED)
        ) {
            val device = uiState.connectedDevice!!
            val name = viewModel.deviceDisplayName(device).ifEmpty { stringResource(R.string.music_bt_unknown) }
            val failMsg = when (uiState.btConnectionState) {
                BtConnectionState.PAIR_FAILED -> stringResource(R.string.music_bt_pair_fail)
                BtConnectionState.A2DP_FAILED -> stringResource(R.string.music_bt_connect_fail_a2dp)
                else -> stringResource(R.string.music_bt_connect_fail)
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(DISC_BG_COLOR)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ListItem(
                        headlineContent = { Text(name, color = Color.White) },
                        supportingContent = {
                            Text(
                                "${device.address} · $failMsg",
                                color = Color.Red.copy(alpha = 0.8f)
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (uiState.btConnectionState == BtConnectionState.PAIR_FAILED) {
                            TextButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            }) {
                                Text(stringResource(R.string.music_bt_go_bt_settings), color = Color(ACCENT_COLOR))
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        TextButton(onClick = { onDeviceClick(device) }) {
                            Text(stringResource(R.string.music_bt_retry), color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 无设备提示
        if (uiState.btDevices.isEmpty() && !uiState.isScanning) {
            Text(
                stringResource(R.string.music_bt_empty),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // 设备列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 264.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.btDevices.size) { index ->
                val device = uiState.btDevices[index]
                // 已在上面单独显示的设备跳过
                val isConnected = uiState.connectedDevice == device && uiState.btConnectionState == BtConnectionState.CONNECTED
                if (isConnected) return@items
                // 我的手机已单独显示，不在普通列表中重复
                if (device == uiState.myPhoneDevice) return@items

                val name = viewModel.deviceDisplayName(device).ifEmpty { stringResource(R.string.music_bt_unknown) }
                val deviceType = viewModel.getDeviceType(device)
                val typeLabel = deviceTypeLabel(deviceType)
                val icon = deviceTypeIcon(deviceType)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceClick(device) },
                    colors = CardDefaults.cardColors(containerColor = Color(DISC_BG_COLOR)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(name, color = Color.White) },
                        supportingContent = {
                            Text(
                                "$typeLabel · ${device.address}",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        leadingContent = {
                            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
