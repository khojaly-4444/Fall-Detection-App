package com.falldetect.falldetection.repositories

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.falldetect.falldetection.models.FallEvent
import java.text.SimpleDateFormat
import java.util.*

class ArduinoManager(private val context: Context) {

    companion object {
        private val FALL_SERVICE_UUID = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")
        private val FALL_CHARACTERISTIC_UUID = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214")

        var onFallDetected: ((FallEvent) -> Unit)? = null
    }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val scanResults = mutableListOf<ScanResult>()
    private var isScanning = false

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!scanResults.contains(result)) {
                scanResults.add(result)
                val name = device.name ?: "Unknown"
                Log.d("ArduinoManager", "Found device: $name, Address: ${device.address}")
                if (name.equals("Seeed_BLE", true)) {
                    Log.d("ArduinoManager", "✅ Found Seeed_BLE! Connecting...")
                    stopScanning()
                    connectToDevice(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ArduinoManager", "Scan failed with error code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(targetDeviceName: String? = "Seeed_BLE") {
        if (!hasPermissions()) {
            Log.e("ArduinoManager", "Missing permissions")
            return
        }
        if (isScanning) return
        isScanning = true
        scanResults.clear()
        bleScanner?.startScan(scanCallback)
        Log.d("ArduinoManager", "Started BLE scanning...")
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        try {
            bleScanner?.stopScan(scanCallback)
            Log.d("ArduinoManager", "Stopped BLE scanning.")
        } catch (e: Exception) {
            Log.e("ArduinoManager", "Stop scan error: ${e.message}")
        }
        isScanning = false
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("ArduinoManager", "Connected to ${device.name}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e("ArduinoManager", "Disconnected from ${device.name}")
                    gatt.close()
                    startScanning()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val service = gatt.getService(FALL_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(FALL_CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(it)
                    }
                    Log.d("ArduinoManager", "Subscribed to fall notifications!")
                } else {
                    Log.e("ArduinoManager", "Fall service or characteristic not found")
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (characteristic.uuid == FALL_CHARACTERISTIC_UUID) {
                    val message = characteristic.value.toString(Charsets.UTF_8)
                    Log.d("ArduinoManager", "📩 Received BLE message: $message")

                    val parts = message.removePrefix("FALL:").split(":")
                    if (parts.size == 2) {
                        val fallType = parts[0].trim().replaceFirstChar { it.uppercaseChar() } + " Fall"
                        val impact = parts[1].trim()

                        val now = Date()
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)

                        val fallEvent = FallEvent(
                            fallType = fallType,
                            date = date,
                            time = time,
                            impactSeverity = impact
                        )

                        // 👉 Send to the ViewModel or UI
                        onFallDetected?.invoke(fallEvent)
                    }
                }
            }
        })
    }
}
