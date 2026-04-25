package com.zkx.aidemo.entertainment.music

import android.app.Application
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

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val showPermissionDialog: Boolean = false,
    val btDevices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false
)

private const val PROGRESS_INTERVAL_MS = 500L

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

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

    // endregion

    // region 蓝牙

    fun onBluetoothDeviceFound(device: BluetoothDevice) {
        val current = _uiState.value.btDevices
        if (!current.contains(device)) {
            _uiState.update { it.copy(btDevices = current + device) }
        }
    }

    fun onScanFinished() {
        _uiState.update { it.copy(isScanning = false) }
    }

    fun startScan() {
        _uiState.update { it.copy(btDevices = emptyList(), isScanning = true) }
        val started = btRepo.startDiscovery()
        if (!started) _uiState.update { it.copy(isScanning = false) }
    }

    fun cancelScan() {
        btRepo.cancelDiscovery()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun deviceDisplayName(device: BluetoothDevice): String =
        btRepo.deviceName(device) ?: device.address

    // endregion

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
        btRepo.cancelDiscovery()
    }
}
