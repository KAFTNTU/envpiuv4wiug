package com.robocar.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

// BLE UUIDs — підтримка HM-10/HC-08 (0xFFE0) і NUS Nordic (6e400001)
private val UUID_HM10_SERVICE  = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val UUID_HM10_CHAR     = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
private val UUID_NUS_SERVICE   = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
private val UUID_NUS_TX        = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // write
private val UUID_NUS_RX        = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e") // notify
private val UUID_CCCD          = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// SLIP constants
private const val SLIP_END     = 0xC0.toByte()
private const val SLIP_ESC     = 0xDB.toByte()
private const val SLIP_ESC_END = 0xDC.toByte()
private const val SLIP_ESC_ESC = 0xDD.toByte()

sealed class BleState {
    object Disconnected : BleState()
    object Scanning : BleState()
    data class Connecting(val deviceName: String) : BleState()
    data class Connected(val deviceName: String) : BleState()
    data class Error(val message: String) : BleState()
}

data class SensorData(
    val p1: Int = 0,
    val p2: Int = 0,
    val p3: Int = 0,
    val p4: Int = 0
)

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter get() = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _state = MutableStateFlow<BleState>(BleState.Disconnected)
    val state: StateFlow<BleState> = _state.asStateFlow()

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _logMessages = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 50)
    val logMessages: SharedFlow<Pair<String, String>> = _logMessages.asSharedFlow()

    val isConnected: Boolean get() = _state.value is BleState.Connected

    // SLIP RX buffer
    private val rxBuffer = mutableListOf<Byte>()
    private var slipEscaping = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Scan
    fun startScan() {
        if (_state.value is BleState.Scanning) return
        _state.value = BleState.Scanning
        log("Scanning for BLE devices...", "info")
        bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        if (_state.value is BleState.Scanning) _state.value = BleState.Disconnected
    }

    private val foundDevices = mutableListOf<BluetoothDevice>()
    private val _scanResults = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scanResults: StateFlow<List<BluetoothDevice>> = _scanResults.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!foundDevices.any { it.address == device.address }) {
                foundDevices.add(device)
                _scanResults.value = foundDevices.toList()
                val name = device.name ?: "Unknown (${device.address})"
                log("Found: $name", "info")
            }
        }
        override fun onScanFailed(errorCode: Int) {
            _state.value = BleState.Error("Scan failed: $errorCode")
        }
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        val name = device.name ?: device.address
        _state.value = BleState.Connecting(name)
        log("Connecting to $name...", "info")
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeCharacteristic = null
        _state.value = BleState.Disconnected
        log("Disconnected", "warn")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("GATT Connected, discovering services...", "info")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    writeCharacteristic = null
                    _state.value = BleState.Disconnected
                    log("Disconnected from GATT", "warn")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = BleState.Error("Service discovery failed")
                return
            }

            // Try HM-10 first, then NUS
            var char: BluetoothGattCharacteristic? = null
            var notifyChar: BluetoothGattCharacteristic? = null

            val hm10 = gatt.getService(UUID_HM10_SERVICE)
            if (hm10 != null) {
                char = hm10.getCharacteristic(UUID_HM10_CHAR)
                notifyChar = char
                log("HM-10 mode (0xFFE0)", "info")
            } else {
                val nus = gatt.getService(UUID_NUS_SERVICE)
                if (nus != null) {
                    char = nus.getCharacteristic(UUID_NUS_TX)
                    notifyChar = nus.getCharacteristic(UUID_NUS_RX)
                    log("NUS Nordic mode", "info")
                }
            }

            if (char == null) {
                _state.value = BleState.Error("No compatible service found")
                log("No compatible BLE service!", "err")
                return
            }

            writeCharacteristic = char

            // Enable notifications
            if (notifyChar != null) {
                gatt.setCharacteristicNotification(notifyChar, true)
                val descriptor = notifyChar.getDescriptor(UUID_CCCD)
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            }

            val deviceName = gatt.device.name ?: gatt.device.address
            _state.value = BleState.Connected(deviceName)
            log("Connected! Ready to send.", "info")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            for (byte in data) processSlipByte(byte)
        }

        // API 33+
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            for (byte in value) processSlipByte(byte)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("TX Error: $status", "err")
            }
        }
    }

    // SLIP decode
    private fun processSlipByte(byte: Byte) {
        when {
            byte == SLIP_END -> {
                if (rxBuffer.isNotEmpty()) {
                    handlePacket(rxBuffer.toByteArray())
                    rxBuffer.clear()
                }
                slipEscaping = false
            }
            slipEscaping -> {
                slipEscaping = false
                when (byte) {
                    SLIP_ESC_END -> rxBuffer.add(SLIP_END)
                    SLIP_ESC_ESC -> rxBuffer.add(SLIP_ESC)
                    else -> rxBuffer.add(byte)
                }
            }
            byte == SLIP_ESC -> slipEscaping = true
            else -> rxBuffer.add(byte)
        }
    }

    private fun handlePacket(data: ByteArray) {
        if (data.size >= 4) {
            _sensorData.value = SensorData(
                p1 = data[0].toInt() and 0xFF,
                p2 = data[1].toInt() and 0xFF,
                p3 = data[2].toInt() and 0xFF,
                p4 = data[3].toInt() and 0xFF
            )
        } else {
            val hex = data.joinToString(" ") { "%02X".format(it) }
            val str = try { String(data).trim() } catch (e: Exception) { "" }
            log("RX RAW: $hex / \"$str\"", "rx")
        }
    }

    // SLIP encode
    private fun slipEncode(data: ByteArray): ByteArray {
        val out = mutableListOf<Byte>()
        out.add(SLIP_END)
        for (byte in data) {
            when (byte) {
                SLIP_END -> { out.add(SLIP_ESC); out.add(SLIP_ESC_END) }
                SLIP_ESC -> { out.add(SLIP_ESC); out.add(SLIP_ESC_ESC) }
                else -> out.add(byte)
            }
        }
        out.add(SLIP_END)
        return out.toByteArray()
    }

    // Send car packet (2 bytes: L, R)
    fun sendCarPacket(l: Int, r: Int) {
        val cL = l.coerceIn(-100, 100)
        val cR = r.coerceIn(-100, 100)
        val raw = byteArrayOf(cL.toByte(), cR.toByte())
        writeBytes(slipEncode(raw))
        log("TX Car: L=$cL R=$cR", "tx")
    }

    // Send drive packet (4 bytes: M1..M4)
    fun sendDrivePacket(m1: Int, m2: Int, m3: Int, m4: Int) {
        val raw = byteArrayOf(
            m1.coerceIn(-100, 100).toByte(),
            m2.coerceIn(-100, 100).toByte(),
            m3.coerceIn(-100, 100).toByte(),
            m4.coerceIn(-100, 100).toByte()
        )
        writeBytes(slipEncode(raw))
    }

    // Send text command
    fun sendText(text: String) {
        val raw = (text + "\n").toByteArray()
        writeBytes(slipEncode(raw))
        log("CMD: $text", "tx")
    }

    @SuppressLint("MissingPermission")
    private fun writeBytes(data: ByteArray) {
        val char = writeCharacteristic ?: return
        val g = gatt ?: return
        if (!isConnected) return

        scope.launch {
            char.value = data
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            g.writeCharacteristic(char)
        }
    }

    private fun log(msg: String, type: String = "info") {
        scope.launch { _logMessages.emit(Pair(msg, type)) }
    }

    fun clearLogs() {}

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
