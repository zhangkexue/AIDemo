package com.zkx.aidemo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

data class BtDevice(val name: String, val address: String)

class BluetoothRepository(private val context: Context) {

    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    val isEnabled: Boolean get() = adapter?.isEnabled == true

    /** 开始扫描，返回 true 表示成功启动 */
    fun startDiscovery(): Boolean {
        val a = adapter ?: return false
        if (!a.isEnabled) return false
        return try {
            if (a.isDiscovering) a.cancelDiscovery()
            a.startDiscovery()
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancelDiscovery() {
        try {
            adapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            // 权限不足时忽略
        }
    }

    fun deviceName(device: BluetoothDevice): String? = try {
        device.name
    } catch (e: SecurityException) {
        null
    }
}
