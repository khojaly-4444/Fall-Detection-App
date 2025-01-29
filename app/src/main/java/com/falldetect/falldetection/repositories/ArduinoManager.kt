package com.falldetect.falldetection.repositories

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

class ArduinoManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val scanResults = mutableListOf<ScanResult>()
    private var isScanning = false

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }


    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (hasPermissions()) {
                val device = result.device
                if (!scanResults.contains(result)) {
                    scanResults.add(result)
                    Log.d(
                        "ArduinoManager",
                        "Found device: ${device.name}, Address: ${device.address}"
                    )

                    // Check if its the microcontroller device
                    if (device.name == "Seeed_BLE"){
                        Log.d("ArduinoManager", "Found the target device! Connecting...")
                        stopScanning()                                                          // Stop scanning once Arduino is found
                        connectToDevice(device)                                                  // Auto-connect to Arduino
                    }
                }
            } else {
                Log.e("ArduinoManager", "Missing Bluetooth permissions!")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            if (hasPermissions()) {
                for (result in results) {
                    if (!scanResults.contains(result)) {
                        scanResults.add(result)
                        Log.d(
                            "ArduinoManager",
                            "Batch device: ${result.device.name}, Address: ${result.device.address}"
                        )
                    }
                }
            } else {
                Log.e("ArduinoManager", "Missing Bluetooth permissions!")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("ArduinoManager", "Scan failed with error code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(targetDeviceName: String? = "Seeed_BLE") {                    // Changes once microcontroller arrives
        if (!hasPermissions()) {
            Log.e("ArduinoManager", "Cannot start scan: Missing permissions")
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
        if (!hasPermissions()) {
            Log.e("ArduinoManager", "Cannot stop scan: Missing permissions")
            return
        }

        if (!isScanning) return
        isScanning = false

        try {
            bleScanner?.stopScan(scanCallback)
            Log.d("ArduinoManager", "Stopped BLE scanning.")
        } catch (e: SecurityException) {
            Log.e("ArduinoManager", "Bluetooth stop scan failed: Missing permissions")
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        Log.d("ArduinoManager", "Attempting to connect to ${device.name} at ${device.address}")

        // Connect to the device using GATT (Generic Attribute Profile)
        val bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("ArduinoManager", "Connected to ${device.name}")
                    gatt.discoverServices() // Discover available services
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e("ArduinoManager", "Disconnected from ${device.name}")
                    gatt.close() // Close GATT connection
                    startScanning()  // Restart Scan if disconnected
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("ArduinoManager", "Services discovered on ${device.name}")
                    // Here you would read/write characteristics
                } else {
                    Log.e("ArduinoManager", "Failed to discover services")
                }
            }
        })
    }

}
