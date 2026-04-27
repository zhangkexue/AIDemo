package com.zkx.aidemo.entertainment.music

import android.bluetooth.BluetoothDevice
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BtConnectionState { IDLE, CONNECTING, CONNECTED, FAILED, PAIR_FAILED, A2DP_FAILED }
enum class BtConnectionMode { SOURCE, SINK }

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val showPermissionDialog: Boolean = false,
    val btDevices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectedDevice: BluetoothDevice? = null,
    val btConnectionState: BtConnectionState = BtConnectionState.IDLE,
    val btConnectionMode: BtConnectionMode = BtConnectionMode.SOURCE,
    val btDeviceType: BtDeviceType = BtDeviceType.OTHER,
    val showBtDisabledDialog: Boolean = false,
    val showSinkUnsupportedDialog: Boolean = false,
    // "我的手机" 跟踪：从已配对设备中自动识别手机类型设备
    val myPhoneDevice: BluetoothDevice? = null,
    val myPhoneFound: Boolean = false,
    val myPhoneBonded: Boolean = false
)

private const val PROGRESS_INTERVAL_MS = 500L
private const val PAIR_POLL_TIMEOUT = 30
private const val PAIR_POLL_INTERVAL_MS = 500L
private const val A2DP_CONNECT_DELAY_MS = 1500L

class MusicPlayerViewModel(application: android.app.Application) : AndroidViewModel(application) {

    private val musicRepo = MusicRepository(application)
    val btRepo = BluetoothRepository(application)

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    // region 音乐

    fun loadSongs() {
        val songs = musicRepo.loadLocalSongs()
        _uiState.update { it.copy(songs = songs) }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(showPermissionDialog = true) }
    }

    fun dismissPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    fun playSongAt(index: Int) {
        val songs = _uiState.value.songs
        if (songs.isEmpty() || index !in songs.indices) return
        mediaPlayer?.release()
        mediaPlayer = musicRepo.createPlayer(songs[index])?.also { it.start() }
        _uiState.update { it.copy(currentIndex = index, isPlaying = mediaPlayer != null, progress = 0f) }
        startProgressTracking()
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.songs.isEmpty()) return
        val mp = mediaPlayer
        if (mp == null) {
            playSongAt(state.currentIndex)
            return
        }
        if (state.isPlaying) {
            mp.pause()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            mp.start()
            _uiState.update { it.copy(isPlaying = true) }
            startProgressTracking()
        }
    }

    fun skipNext() {
        val state = _uiState.value
        if (state.songs.isEmpty()) return
        playSongAt((state.currentIndex + 1) % state.songs.size)
    }

    fun skipPrevious() {
        val state = _uiState.value
        if (state.songs.isEmpty()) return
        playSongAt((state.currentIndex - 1 + state.songs.size) % state.songs.size)
    }

    fun seekTo(fraction: Float) {
        val mp = mediaPlayer ?: return
        mp.seekTo((fraction * mp.duration).toInt())
        _uiState.update { it.copy(progress = fraction) }
    }

    private fun startProgressTracking() {
        viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    _uiState.update { it.copy(progress = mp.currentPosition.toFloat() / mp.duration.toFloat()) }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    /** 重新创建 MediaPlayer 以拾取新的音频路由 */
    private fun restartPlayback() {
        val state = _uiState.value
        val songs = state.songs
        if (songs.isEmpty()) return
        val idx = state.currentIndex
        if (idx !in songs.indices) return
        mediaPlayer?.release()
        mediaPlayer = musicRepo.createPlayer(songs[idx])?.also { it.start() }
        _uiState.update { it.copy(isPlaying = mediaPlayer != null, progress = 0f) }
        startProgressTracking()
    }

    // endregion

    // region 蓝牙

    fun onBluetoothDeviceFound(device: BluetoothDevice) {
        val current = _uiState.value.btDevices
        if (!current.contains(device)) {
            _uiState.update { it.copy(btDevices = current + device) }
        }
        // 检查是否为手机类型，更新 "我的手机"
        updateMyPhone(device)
    }

    /** 从已配对设备和扫描设备中识别手机 */
    private fun updateMyPhone(device: BluetoothDevice) {
        if (btRepo.getDeviceType(device) != BtDeviceType.PHONE) return
        val current = _uiState.value.myPhoneDevice
        if (current != null && current != device) return // 已有我的手机
        val bonded = try { device.bondState == BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false }
        _uiState.update {
            it.copy(
                myPhoneDevice = device,
                myPhoneFound = true,
                myPhoneBonded = bonded
            )
        }
    }

    /** 从已配对设备中查找手机 */
    fun findMyPhoneFromBonded() {
        val phones = btRepo.bondedDevices()
            .filter { btRepo.getDeviceType(it) == BtDeviceType.PHONE }
        if (phones.isNotEmpty()) {
            val phone = phones.first()
            _uiState.update {
                it.copy(
                    myPhoneDevice = phone,
                    myPhoneFound = true,
                    myPhoneBonded = true
                )
            }
        }
    }

    /** 配对状态变化回调（由 BroadcastReceiver 触发） */
    fun onBondStateChanged(device: BluetoothDevice, bondState: Int) {
        val myPhone = _uiState.value.myPhoneDevice
        if (device == myPhone) {
            _uiState.update {
                it.copy(myPhoneBonded = bondState == BluetoothDevice.BOND_BONDED)
            }
        }
        // 如果正在等待配对完成，检查是否是目标设备
        val connected = _uiState.value.connectedDevice
        if (device == connected && bondState == BluetoothDevice.BOND_BONDED) {
            val mode = _uiState.value.btConnectionMode
            connectProfile(device, mode)
        }
    }

    fun onScanFinished() {
        _uiState.update { it.copy(isScanning = false) }
    }

    fun startScan() {
        if (!btRepo.isEnabled) {
            _uiState.update { it.copy(showBtDisabledDialog = true) }
            return
        }
        _uiState.update { it.copy(btDevices = emptyList(), isScanning = true) }
        val started = btRepo.startDiscovery()
        if (!started) _uiState.update { it.copy(isScanning = false) }
        // 同时从已配对设备中查找我的手机
        findMyPhoneFromBonded()
    }

    fun dismissBtDisabledDialog() {
        _uiState.update { it.copy(showBtDisabledDialog = false) }
    }

    fun dismissSinkUnsupportedDialog() {
        _uiState.update { it.copy(showSinkUnsupportedDialog = false) }
    }

    fun cancelScan() {
        btRepo.cancelDiscovery()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun deviceDisplayName(device: BluetoothDevice): String =
        btRepo.deviceName(device) ?: ""

    fun getDeviceType(device: BluetoothDevice): BtDeviceType =
        btRepo.getDeviceType(device)

    /**
     * 连接蓝牙设备，根据设备类型自动选择连接模式：
     * - 耳机/音箱 → A2DP Source（本机音乐输出到耳机）
     * - 手机 → A2DP Sink（从手机接收音乐在本机播放）
     * - 其他 → 默认 A2DP Source
     */
    fun connectDevice(device: BluetoothDevice) {
        val state = _uiState.value
        if (state.connectedDevice == device && state.btConnectionState == BtConnectionState.CONNECTED) {
            disconnectDevice()
            return
        }
        if (state.btConnectionState == BtConnectionState.CONNECTING) return

        val deviceType = btRepo.getDeviceType(device)
        val mode = if (deviceType == BtDeviceType.PHONE) BtConnectionMode.SINK else BtConnectionMode.SOURCE

        _uiState.update {
            it.copy(
                btConnectionState = BtConnectionState.CONNECTING,
                connectedDevice = device,
                btConnectionMode = mode,
                btDeviceType = deviceType
            )
        }

        try {
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                val started = btRepo.pairDevice(device)
                if (!started) {
                    _uiState.update { it.copy(btConnectionState = BtConnectionState.PAIR_FAILED) }
                    return
                }
                // 配对结果由 onBondStateChanged() 回调处理
                // 设置超时，防止回调未触发
                viewModelScope.launch {
                    repeat(PAIR_POLL_TIMEOUT) {
                        delay(PAIR_POLL_INTERVAL_MS)
                        if (_uiState.value.btConnectionState != BtConnectionState.CONNECTING) return@launch
                        if (device.bondState == BluetoothDevice.BOND_BONDED) {
                            connectProfile(device, mode)
                            return@launch
                        }
                    }
                    // 超时
                    if (_uiState.value.btConnectionState == BtConnectionState.CONNECTING) {
                        _uiState.update { it.copy(btConnectionState = BtConnectionState.PAIR_FAILED) }
                    }
                }
            } else {
                connectProfile(device, mode)
            }
        } catch (_: SecurityException) {
            _uiState.update { it.copy(btConnectionState = BtConnectionState.FAILED) }
        }
    }

    private fun connectProfile(device: BluetoothDevice, mode: BtConnectionMode) {
        if (mode == BtConnectionMode.SINK) {
            connectA2dpSink(device)
        } else {
            connectA2dpSource(device)
        }
    }

    /** A2DP Source 连接：本机音乐 → 蓝牙耳机/音箱 */
    private fun connectA2dpSource(device: BluetoothDevice) {
        btRepo.connectA2dp(device) { success ->
            if (success) {
                // A2DP 连接成功后延迟一下再重启播放，让音频路由生效
                viewModelScope.launch {
                    delay(A2DP_CONNECT_DELAY_MS)
                    _uiState.update { it.copy(btConnectionState = BtConnectionState.CONNECTED) }
                    restartPlayback()
                }
            } else {
                _uiState.update { it.copy(btConnectionState = BtConnectionState.A2DP_FAILED) }
            }
        }
    }

    /** A2DP Sink 连接：手机音乐 → 本机播放 */
    private fun connectA2dpSink(device: BluetoothDevice) {
        btRepo.connectA2dpSink(device) { success ->
            if (success) {
                _uiState.update { it.copy(btConnectionState = BtConnectionState.CONNECTED) }
            } else {
                // A2DP Sink 不受支持
                _uiState.update {
                    it.copy(
                        btConnectionState = BtConnectionState.A2DP_FAILED,
                        showSinkUnsupportedDialog = true
                    )
                }
            }
        }
    }

    /** A2DP 连接状态变化回调（由 BroadcastReceiver 触发） */
    fun onA2dpStateChanged(device: BluetoothDevice, state: Int) {
        val connected = _uiState.value.connectedDevice
        if (device != connected) return
        when (state) {
            android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                if (_uiState.value.btConnectionMode == BtConnectionMode.SOURCE) {
                    restartPlayback()
                }
                _uiState.update { it.copy(btConnectionState = BtConnectionState.CONNECTED) }
            }
            android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                _uiState.update {
                    it.copy(
                        connectedDevice = null,
                        btConnectionState = BtConnectionState.IDLE,
                        btConnectionMode = BtConnectionMode.SOURCE,
                        btDeviceType = BtDeviceType.OTHER
                    )
                }
            }
        }
    }

    fun disconnectDevice() {
        val device = _uiState.value.connectedDevice ?: return
        val mode = _uiState.value.btConnectionMode
        val disconnectFn = if (mode == BtConnectionMode.SINK) {
            { cb: (Boolean) -> Unit -> btRepo.disconnectA2dpSink(device, cb) }
        } else {
            { cb: (Boolean) -> Unit -> btRepo.disconnectA2dp(device, cb) }
        }
        disconnectFn { _ ->
            _uiState.update {
                it.copy(
                    connectedDevice = null,
                    btConnectionState = BtConnectionState.IDLE,
                    btConnectionMode = BtConnectionMode.SOURCE,
                    btDeviceType = BtDeviceType.OTHER
                )
            }
            if (mode == BtConnectionMode.SOURCE) {
                restartPlayback()
            }
        }
    }

    // endregion

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
        btRepo.cancelDiscovery()
    }
}
