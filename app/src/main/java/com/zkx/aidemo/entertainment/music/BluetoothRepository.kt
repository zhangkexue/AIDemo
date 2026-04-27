package com.zkx.aidemo.entertainment.music

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager

enum class BtDeviceType { HEADSET, PHONE, OTHER }

class BluetoothRepository(private val context: Context) {

    private val adapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val isEnabled: Boolean
        get() = try { adapter?.isEnabled == true } catch (_: SecurityException) { false }

    // region 设备分类

    // BluetoothClass.Major 常量在 API 35 中被移除，使用直接数值
    private companion object {
        const val BT_MAJOR_AUDIO_VIDEO = 1024
        const val BT_MAJOR_PHONE = 512
        const val PROFILE_A2DP_SINK = 11
    }

    /** 根据 BluetoothClass 判断设备类型 */
    fun getDeviceType(device: BluetoothDevice): BtDeviceType {
        val btClass = try { device.bluetoothClass } catch (_: SecurityException) { null }
        if (btClass == null) return BtDeviceType.OTHER
        return when (btClass.majorDeviceClass) {
            BT_MAJOR_AUDIO_VIDEO -> BtDeviceType.HEADSET
            BT_MAJOR_PHONE -> BtDeviceType.PHONE
            else -> {
                // 部分设备主类不准确，再用子类判断
                val dc = btClass.deviceClass
                when {
                    dc == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> BtDeviceType.HEADSET
                    dc == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> BtDeviceType.HEADSET
                    dc == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> BtDeviceType.HEADSET
                    dc == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> BtDeviceType.HEADSET
                    dc == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> BtDeviceType.HEADSET
                    dc == BluetoothClass.Device.PHONE_SMART -> BtDeviceType.PHONE
                    dc == BluetoothClass.Device.PHONE_CELLULAR -> BtDeviceType.PHONE
                    else -> BtDeviceType.OTHER
                }
            }
        }
    }

    // endregion

    // region 扫描与配对

    fun bondedDevices(): Set<BluetoothDevice> = try {
        adapter?.bondedDevices ?: emptySet()
    } catch (_: SecurityException) { emptySet() }

    fun startDiscovery(): Boolean {
        val a = adapter ?: return false
        if (!a.isEnabled) return false
        return try {
            if (a.isDiscovering) a.cancelDiscovery()
            a.startDiscovery()
        } catch (_: SecurityException) { false }
    }

    fun cancelDiscovery() {
        try { adapter?.cancelDiscovery() } catch (_: SecurityException) { }
    }

    fun deviceName(device: BluetoothDevice): String? = try {
        device.name
    } catch (_: SecurityException) { null }

    fun pairDevice(device: BluetoothDevice): Boolean = try {
        device.createBond()
    } catch (_: SecurityException) { false }

    // endregion

    // region A2DP Source（本机 → 耳机/音箱）

    /**
     * 连接 A2DP Source 模式，将本机音频输出到蓝牙耳机/音箱。
     * 连接成功后，Android 音频框架会自动将 STREAM_MUSIC 路由到 A2DP 设备，
     * 不需要手动调用 SCO 或其他音频路由方法。
     * 调用方应在连接成功后重新创建 MediaPlayer 以拾取新路由。
     */
    fun connectA2dp(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
        val a = adapter ?: run { onResult(false); return }
        try {
            a.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val result = try {
                        val method = BluetoothA2dp::class.java.getMethod("connect", BluetoothDevice::class.java)
                        method.invoke(proxy, device) as? Boolean ?: false
                    } catch (_: Exception) { false }
                    try { a.closeProfileProxy(BluetoothProfile.A2DP, proxy) } catch (_: Exception) { }
                    onResult(result)
                }
                override fun onServiceDisconnected(profile: Int) { onResult(false) }
            }, BluetoothProfile.A2DP)
        } catch (_: SecurityException) { onResult(false) }
    }

    fun disconnectA2dp(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
        val a = adapter ?: run { onResult(false); return }
        try {
            a.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val method = BluetoothA2dp::class.java.getMethod("disconnect", BluetoothDevice::class.java)
                        method.invoke(proxy, device)
                    } catch (_: Exception) { }
                    try { a.closeProfileProxy(BluetoothProfile.A2DP, proxy) } catch (_: Exception) { }
                    onResult(true)
                }
                override fun onServiceDisconnected(profile: Int) { onResult(false) }
            }, BluetoothProfile.A2DP)
        } catch (_: SecurityException) { onResult(false) }
    }

    // endregion

    // region A2DP Sink（手机 → 本机）

    /**
     * 尝试以 A2DP Sink 模式连接手机，接收手机的音频在本机播放。
     * 大多数消费级手机不支持此功能，仅 Android TV / 车机等设备可能支持。
     */
    fun connectA2dpSink(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
        val a = adapter ?: run { onResult(false); return }
        try {
            a.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val result = try {
                        val method = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        method.invoke(proxy, device) as? Boolean ?: false
                    } catch (_: Exception) { false }
                    try { a.closeProfileProxy(PROFILE_A2DP_SINK, proxy) } catch (_: Exception) { }
                    onResult(result)
                }
                override fun onServiceDisconnected(profile: Int) { onResult(false) }
            }, PROFILE_A2DP_SINK)
        } catch (_: Exception) { onResult(false) }
    }

    fun disconnectA2dpSink(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
        val a = adapter ?: run { onResult(false); return }
        try {
            a.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val method = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                        method.invoke(proxy, device)
                    } catch (_: Exception) { }
                    try { a.closeProfileProxy(PROFILE_A2DP_SINK, proxy) } catch (_: Exception) { }
                    onResult(true)
                }
                override fun onServiceDisconnected(profile: Int) { onResult(false) }
            }, PROFILE_A2DP_SINK)
        } catch (_: Exception) { onResult(false) }
    }

    // endregion
}
