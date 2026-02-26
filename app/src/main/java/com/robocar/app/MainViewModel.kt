package com.robocar.app

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robocar.app.ble.BleManager
import com.robocar.app.ble.BleState
import com.robocar.app.ble.SensorData
import com.robocar.app.model.MoveRecord
import com.robocar.app.model.TuningSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

val Context.dataStore by preferencesDataStore("settings")

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val bleManager = BleManager(app)
    val bleState: StateFlow<BleState> = bleManager.state
    val sensorData: StateFlow<SensorData> = bleManager.sensorData
    val scanResults: StateFlow<List<BluetoothDevice>> = bleManager.scanResults
    val logMessages = bleManager.logMessages

    // --- Tabs ---
    private val _currentTab = MutableStateFlow(0) // 0=Joystick, 1=Blockly
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()
    fun setTab(tab: Int) { _currentTab.value = tab }

    // --- Log ---
    private val _logs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val logs: StateFlow<List<Pair<String, String>>> = _logs.asStateFlow()

    // --- Joystick state ---
    private val _motorL = MutableStateFlow(0)
    private val _motorR = MutableStateFlow(0)
    val motorL: StateFlow<Int> = _motorL.asStateFlow()
    val motorR: StateFlow<Int> = _motorR.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1.0f)
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    // --- Gyro ---
    private val _gyroEnabled = MutableStateFlow(false)
    val gyroEnabled: StateFlow<Boolean> = _gyroEnabled.asStateFlow()

    private val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroCalibrated = false
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f

    // --- Tuning ---
    private val _tuning = MutableStateFlow(TuningSettings())
    val tuning: StateFlow<TuningSettings> = _tuning.asStateFlow()

    // --- Track record/replay ---
    private val moveHistory = mutableListOf<MoveRecord>()
    private var isRecordingTrack = false

    // --- Joystick continuous send ---
    private var joyJob: Job? = null
    private var lastJoyX = 0
    private var lastJoyY = 0

    // --- Log modal ---
    private val _showLog = MutableStateFlow(false)
    val showLog: StateFlow<Boolean> = _showLog.asStateFlow()
    fun toggleLog() { _showLog.value = !_showLog.value }

    // --- Scan modal ---
    private val _showScanDialog = MutableStateFlow(false)
    val showScanDialog: StateFlow<Boolean> = _showScanDialog.asStateFlow()

    // --- Settings modal ---
    private val _showTuning = MutableStateFlow(false)
    val showTuning: StateFlow<Boolean> = _showTuning.asStateFlow()
    fun toggleTuning() { _showTuning.value = !_showTuning.value }

    // --- Password modal ---
    private val _showPassword = MutableStateFlow(false)
    val showPassword: StateFlow<Boolean> = _showPassword.asStateFlow()
    fun togglePassword() { _showPassword.value = !_showPassword.value }

    init {
        // Collect logs
        viewModelScope.launch {
            bleManager.logMessages.collect { msg ->
                _logs.update { list -> (list + msg).takeLast(200) }
            }
        }

        // Auto-send joystick every 50ms
        joyJob = viewModelScope.launch {
            while (true) {
                delay(50)
                if (bleManager.isConnected) {
                    val t = _tuning.value
                    val (l, r) = tankDrive(lastJoyX, lastJoyY, t)
                    _motorL.value = l
                    _motorR.value = r
                    val m34 = if (t.use4Motors) l to r else 0 to 0
                    bleManager.sendDrivePacket(l, r, m34.first, m34.second)
                }
            }
        }

        loadTuning()
    }

    // ===================== BLUETOOTH =====================

    fun onConnectClicked() {
        if (bleManager.isConnected) {
            bleManager.disconnect()
        } else {
            _showScanDialog.value = true
            bleManager.startScan()
        }
    }

    fun onDeviceSelected(device: BluetoothDevice) {
        _showScanDialog.value = false
        bleManager.connect(device)
    }

    fun dismissScan() {
        _showScanDialog.value = false
        bleManager.stopScan()
    }

    // ===================== JOYSTICK =====================

    fun updateJoystick(vx: Int, vy: Int) {
        lastJoyX = vx
        lastJoyY = vy
        val t = _tuning.value
        val (l, r) = tankDrive(vx, vy, t)
        _motorL.value = l
        _motorR.value = r
        if (bleManager.isConnected) {
            val m34 = if (_tuning.value.use4Motors) l to r else 0 to 0
            bleManager.sendDrivePacket(l, r, m34.first, m34.second)
        }
    }

    fun resetJoystick() {
        lastJoyX = 0
        lastJoyY = 0
        _motorL.value = 0
        _motorR.value = 0
        if (bleManager.isConnected) bleManager.sendDrivePacket(0, 0, 0, 0)
    }

    fun setSpeed(percent: Int) {
        _speedMultiplier.value = percent / 100f
    }

    private fun tankDrive(vx: Int, vy: Int, t: TuningSettings): Pair<Int, Int> {
        var adjX = (vx * (t.turnSens / 100.0)).toInt()
        val speed = _speedMultiplier.value

        var vL = (vy + adjX) * speed
        var vR = (vy - adjX) * speed

        if (t.trim > 0) vR = vR * (1f - t.trim / 100f)
        else if (t.trim < 0) vL = vL * (1f - (-t.trim) / 100f)

        if (t.invertL) vL = -vL
        if (t.invertR) vR = -vR

        return Pair(vL.toInt().coerceIn(-100, 100), vR.toInt().coerceIn(-100, 100))
    }

    // ===================== GYROSCOPE =====================

    fun toggleGyro() {
        _gyroEnabled.value = !_gyroEnabled.value
        if (_gyroEnabled.value) {
            gyroCalibrated = false
            gyroOffsetX = 0f
            gyroOffsetY = 0f
            sensorManager.registerListener(
                gyroListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME
            )
        } else {
            sensorManager.unregisterListener(gyroListener)
            resetJoystick()
        }
    }

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ORIENTATION) return
            val gamma = event.values[2] // tilt left/right
            val beta = event.values[1]  // tilt front/back

            if (!gyroCalibrated) {
                gyroOffsetX = gamma
                gyroOffsetY = beta
                gyroCalibrated = true
                return
            }

            var x = gamma - gyroOffsetX
            var y = beta - gyroOffsetY
            val maxTilt = 30f

            x = x.coerceIn(-maxTilt, maxTilt)
            y = y.coerceIn(-maxTilt, maxTilt)

            var valX = ((x / maxTilt) * 100).toInt()
            var valY = ((y / maxTilt) * 100).toInt()

            if (Math.abs(valX) < 10) valX = 0
            if (Math.abs(valY) < 10) valY = 0
            valY = -valY

            updateJoystick(valX, valY)
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    // ===================== TRACK RECORD/REPLAY =====================

    fun recordMove(m1: Int, m2: Int, m3: Int, m4: Int) {
        if (isRecordingTrack) {
            moveHistory.add(MoveRecord.Move(m1, m2, m3, m4))
        }
    }

    fun recordWait(sec: Double) {
        if (isRecordingTrack) {
            moveHistory.add(MoveRecord.Wait(sec))
        }
    }

    fun startRecording() {
        moveHistory.clear()
        isRecordingTrack = true
    }

    fun stopRecording() {
        isRecordingTrack = false
    }

    fun replayTrack(times: Int = 1, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repeat(times) {
                for (action in moveHistory) {
                    when (action) {
                        is MoveRecord.Move -> bleManager.sendDrivePacket(action.m1, action.m2, action.m3, action.m4)
                        is MoveRecord.Wait -> delay((action.sec * 1000).toLong())
                    }
                }
            }
            bleManager.sendDrivePacket(0, 0, 0, 0)
            onComplete()
        }
    }

    fun goHome() {
        viewModelScope.launch {
            for (i in moveHistory.indices.reversed()) {
                val action = moveHistory[i]
                when (action) {
                    is MoveRecord.Move -> bleManager.sendDrivePacket(-action.m1, -action.m2, -action.m3, -action.m4)
                    is MoveRecord.Wait -> delay((action.sec * 1000).toLong())
                }
            }
            bleManager.sendDrivePacket(0, 0, 0, 0)
            moveHistory.clear()
        }
    }

    // ===================== TUNING =====================

    fun updateTuning(settings: TuningSettings) {
        _tuning.value = settings
        saveTuning(settings)
    }

    private fun loadTuning() {
        viewModelScope.launch {
            getApplication<Application>().dataStore.data.first().let { prefs ->
                _tuning.value = TuningSettings(
                    invertL   = prefs[booleanPreferencesKey("invertL")] ?: false,
                    invertR   = prefs[booleanPreferencesKey("invertR")] ?: false,
                    trim      = prefs[intPreferencesKey("trim")] ?: 0,
                    turnSens  = prefs[intPreferencesKey("turnSens")] ?: 100,
                    use4Motors = prefs[booleanPreferencesKey("use4Motors")] ?: false,
                    useSlip   = prefs[booleanPreferencesKey("useSlip")] ?: true
                )
            }
        }
    }

    private fun saveTuning(t: TuningSettings) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[booleanPreferencesKey("invertL")] = t.invertL
                prefs[booleanPreferencesKey("invertR")] = t.invertR
                prefs[intPreferencesKey("trim")]         = t.trim
                prefs[intPreferencesKey("turnSens")]     = t.turnSens
                prefs[booleanPreferencesKey("use4Motors")] = t.use4Motors
                prefs[booleanPreferencesKey("useSlip")]    = t.useSlip
            }
        }
    }

    // ===================== COMMANDS =====================

    fun sendPassword(pass: String) = bleManager.sendText("PASS:$pass")

    fun clearLog() { _logs.value = emptyList() }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(gyroListener)
        bleManager.destroy()
    }
}
