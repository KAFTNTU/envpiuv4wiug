package com.robocar.app.ui.blocks

import com.robocar.app.ble.BleManager
import com.robocar.app.ble.SensorData
import com.robocar.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

class BlockExecutor(
    private val bleManager: BleManager,
    private val sensorData: StateFlow<SensorData>,
) {
    var shouldStop = false
    var speedMultiplier = 1.0f
    private val moveHistory = mutableListOf<MoveRecord>()

    // State machine
    private var currentState = ""
    private var prevState = ""
    private var stateEnterMs = 0L
    private val stateCounts = mutableMapOf<String, Int>()
    private val cooldowns = mutableMapOf<String, Long>()
    private val latches = mutableMapOf<String, Boolean>()

    // PID
    private var pidIntegral = 0f
    private var pidLastError = 0f

    // Track recording
    private var isRecordingTrack = false
    private data class TrackStep(val t: Long, val l: Int, val r: Int)
    private val trackMemory = mutableListOf<TrackStep>()

    // Speed calibration result
    private var calibSpeedCms = 0f

    suspend fun execute(blocks: List<ProgramBlock>) {
        shouldStop = false
        moveHistory.clear()
        pidIntegral = 0f
        pidLastError = 0f
        currentState = ""
        prevState = ""
        stateCounts.clear()
        cooldowns.clear()
        latches.clear()
        trackMemory.clear()
        isRecordingTrack = false

        executeSequence(blocks)
    }

    private suspend fun executeSequence(blocks: List<ProgramBlock>) {
        for (block in blocks) {
            if (shouldStop) return
            executeBlock(block)
        }
    }

    private suspend fun executeBlock(block: ProgramBlock) {
        if (shouldStop) return

        fun p(i: Int) = block.params.getOrNull(i)
        fun num(i: Int) = (p(i) as? BlockParam.NumberInput)?.value ?: 0f
        fun drop(i: Int) = (p(i) as? BlockParam.DropdownInput)?.selected ?: ""
        fun txt(i: Int) = (p(i) as? BlockParam.TextInput)?.value ?: ""
        fun sensor(portStr: String): Int {
            val idx = portStr.toIntOrNull() ?: 0
            return when (idx) {
                0 -> sensorData.value.p1
                1 -> sensorData.value.p2
                2 -> sensorData.value.p3
                3 -> sensorData.value.p4
                else -> 0
            }
        }

        when (block.type) {
            // ========= МАШИНКА =========
            BlockType.START_HAT -> { /* нічого */ }

            BlockType.ROBOT_MOVE -> {
                val l = (num(0) * speedMultiplier).toInt()
                val r = (num(1) * speedMultiplier).toInt()
                sendCar(l, r)
            }

            BlockType.ROBOT_MOVE_SOFT -> {
                val target = num(0)
                val sec = num(1)
                val steps = (sec * 20).toInt().coerceAtLeast(1)
                for (i in 1..steps) {
                    if (shouldStop) return
                    val cur = ((target / steps) * i * speedMultiplier).toInt()
                    sendCar(cur, cur)
                    delay(50)
                }
            }

            BlockType.ROBOT_TURN -> {
                val dir = drop(0)
                val sec = num(1)
                val l = if (dir == "LEFT") -80 else 80
                val r = if (dir == "LEFT") 80 else -80
                sendCar(l, r)
                delay((sec * 1000).toLong())
                sendCar(0, 0)
            }

            BlockType.ROBOT_SET_SPEED -> {
                speedMultiplier = num(0) / 100f
            }

            BlockType.ROBOT_STOP -> {
                sendCar(0, 0)
            }

            BlockType.MOTOR_SINGLE -> {
                val motor = drop(0).toIntOrNull() ?: 1
                val spd = num(1).toInt()
                val state = intArrayOf(0, 0, 0, 0)
                state[motor - 1] = spd
                bleManager.sendDrivePacket(state[0], state[1], state[2], state[3])
            }

            BlockType.GO_HOME -> {
                for (i in moveHistory.indices.reversed()) {
                    if (shouldStop) return
                    when (val action = moveHistory[i]) {
                        is MoveRecord.Move -> bleManager.sendDrivePacket(-action.m1, -action.m2, -action.m3, -action.m4)
                        is MoveRecord.Wait -> delay((action.sec * 1000).toLong())
                    }
                }
                sendCar(0, 0)
                moveHistory.clear()
            }

            BlockType.RECORD_START -> {
                trackMemory.clear()
                isRecordingTrack = true
            }

            BlockType.REPLAY_TRACK -> {
                isRecordingTrack = false
                replayTrackOnce()
            }

            BlockType.REPLAY_LOOP -> {
                isRecordingTrack = false
                val times = num(0).toInt().coerceAtLeast(1)
                repeat(times) {
                    if (shouldStop) return
                    replayTrackOnce()
                    delay(300)
                }
            }

            BlockType.WAIT_START -> {
                while (!shouldStop) {
                    if (sensorData.value.p1 > 60) break
                    delay(50)
                }
            }

            BlockType.STOP_AT_START -> {
                while (!shouldStop) {
                    if (sensorData.value.p1 > 60) break
                    delay(20)
                }
                sendCar(0, 0)
            }

            BlockType.COUNT_LAPS -> {
                val target = num(0).toInt()
                var counted = 0
                var onLine = false
                while (counted < target && !shouldStop) {
                    val s = sensorData.value.p1 > 60
                    if (s && !onLine) { onLine = true; counted++ }
                    else if (!s && onLine) onLine = false
                    delay(50)
                }
            }

            BlockType.AUTOPILOT -> {
                val portIdx = drop(0).toIntOrNull() ?: 0
                val dir = drop(1)
                val thr = num(2).toInt()
                val spd = (num(3) * speedMultiplier).toInt()
                while (!shouldStop) {
                    val s = sensor(portIdx.toString())
                    if (s in 1 until thr) {
                        sendCar(-spd, -spd); delay(250)
                        if (dir == "LEFT") sendCar(-spd, spd) else sendCar(spd, -spd)
                        delay(320)
                        sendCar(0, 0); delay(80)
                    } else {
                        sendCar(spd, spd); delay(80)
                    }
                }
            }

            // ========= КЕРУВАННЯ =========
            BlockType.WAIT_SECONDS -> {
                val sec = num(0)
                moveHistory.add(MoveRecord.Wait(sec.toDouble()))
                delay((sec * 1000).toLong())
            }

            BlockType.LOOP_FOREVER -> {
                while (!shouldStop) {
                    executeSequence(block.subBlocks)
                }
            }

            BlockType.LOOP_REPEAT -> {
                val times = num(0).toInt().coerceAtLeast(1)
                repeat(times) {
                    if (!shouldStop) executeSequence(block.subBlocks)
                }
            }

            BlockType.LOOP_REPEAT_PAUSE -> {
                val times = num(0).toInt().coerceAtLeast(1)
                val pause = num(1)
                for (i in 0 until times) {
                    if (shouldStop) return
                    executeSequence(block.subBlocks)
                    if (i < times - 1 && pause > 0) delay((pause * 1000).toLong())
                }
            }

            BlockType.LOOP_EVERY_SEC -> {
                val period = num(0)
                while (!shouldStop) {
                    val t0 = System.currentTimeMillis()
                    executeSequence(block.subBlocks)
                    val elapsed = (System.currentTimeMillis() - t0) / 1000f
                    val sleep = (period - elapsed).coerceAtLeast(0f)
                    if (sleep > 0) delay((sleep * 1000).toLong())
                }
            }

            BlockType.TIMER_RESET -> { /* handled via startTime in ViewModel */ }

            // ========= СЕНСОРИ =========
            BlockType.WAIT_UNTIL_SENSOR -> {
                val portIdx = drop(0).toIntOrNull() ?: 0
                val op = drop(1)
                val val_ = num(2).toInt()
                while (!shouldStop) {
                    val s = sensor(portIdx.toString())
                    val ok = if (op == "LT") s < val_ else s > val_
                    if (ok) break
                    delay(50)
                }
            }

            // ========= МАТЕМАТИКА =========
            BlockType.TIMER_GET -> { /* value block, не виконується як statement */ }
            BlockType.MATH_PID -> { /* value block */ }
            BlockType.MATH_SMOOTH -> { /* value block */ }
            BlockType.MATH_PYTHAGORAS -> { /* value block */ }
            BlockType.MATH_PATH_VT -> { /* value block */ }
            BlockType.MATH_SPEED_CMS -> { /* value block */ }

            BlockType.CALIBRATE_SPEED -> {
                val lcm = num(0)
                val portIdx = drop(1).toIntOrNull() ?: 0
                val thr = num(2).toInt()
                val spd = num(3).toInt()
                val op = "LT" // завжди < поріг для лінії

                fun isLine(): Boolean {
                    val s = sensor(portIdx.toString())
                    return s < thr
                }

                // Чекати першу лінію
                var okMs = 0L
                while (!shouldStop) {
                    if (isLine()) okMs += 30 else okMs = 0
                    if (okMs >= 120) break
                    delay(30)
                }
                // Зійти з лінії
                var offMs = 0L
                while (!shouldStop) {
                    if (!isLine()) offMs += 30 else offMs = 0
                    if (offMs >= 180) break
                    delay(30)
                }
                // Почати вимір
                val t0 = System.currentTimeMillis()
                bleManager.sendCarPacket(spd, spd)
                // Чекати другу лінію
                okMs = 0
                while (!shouldStop) {
                    if (isLine()) okMs += 30 else okMs = 0
                    if (okMs >= 120) break
                    delay(30)
                }
                bleManager.sendCarPacket(0, 0)
                val tSec = (System.currentTimeMillis() - t0) / 1000f
                calibSpeedCms = if (tSec > 0.05f) lcm / tSec else 0f
                delay(120)
            }

            // ========= СТАН =========
            BlockType.STATE_SET -> {
                val newState = txt(0)
                if (currentState != newState) {
                    prevState = currentState
                    currentState = newState
                    stateEnterMs = System.currentTimeMillis()
                    stateCounts[newState] = (stateCounts[newState] ?: 0) + 1
                }
            }

            BlockType.STATE_SET_REASON -> {
                val newState = txt(0)
                if (currentState != newState) {
                    prevState = currentState
                    currentState = newState
                    stateEnterMs = System.currentTimeMillis()
                    stateCounts[newState] = (stateCounts[newState] ?: 0) + 1
                }
            }

            BlockType.STATE_PREV -> {
                if (prevState.isNotEmpty() && currentState != prevState) {
                    val old = currentState
                    currentState = prevState
                    prevState = old
                    stateEnterMs = System.currentTimeMillis()
                    stateCounts[currentState] = (stateCounts[currentState] ?: 0) + 1
                }
            }

            BlockType.STATE_IF -> {
                val targetState = txt(0)
                if (currentState == targetState) {
                    executeSequence(block.subBlocks)
                } else {
                    executeSequence(block.subBlocks2)
                }
            }

            // ========= РОЗУМНІ =========
            BlockType.WAIT_UNTIL_TRUE_FOR -> {
                val portIdx = drop(0).toIntOrNull() ?: 0
                val op = drop(1)
                val valThr = num(2).toInt()
                val sec = num(3)
                var t0: Long? = null
                while (!shouldStop) {
                    val s = sensor(portIdx.toString())
                    val ok = if (op == "LT") s < valThr else s > valThr
                    if (ok) {
                        if (t0 == null) t0 = System.currentTimeMillis()
                        if (System.currentTimeMillis() - t0!! >= (sec * 1000).toLong()) break
                    } else {
                        t0 = null
                    }
                    delay(50)
                }
            }

            BlockType.TIMEOUT_DO_UNTIL -> {
                val portIdx = drop(0).toIntOrNull() ?: 0
                val op = drop(1)
                val valThr = num(2).toInt()
                val maxSec = num(3)
                val end = System.currentTimeMillis() + (maxSec * 1000).toLong()
                while (System.currentTimeMillis() < end && !shouldStop) {
                    val s = sensor(portIdx.toString())
                    val ok = if (op == "LT") s < valThr else s > valThr
                    if (ok) break
                    executeSequence(block.subBlocks)
                    delay(50)
                }
            }

            BlockType.COOLDOWN_DO -> {
                val sec = num(0)
                val now = System.currentTimeMillis()
                val last = cooldowns[block.id] ?: 0L
                if (now - last >= (sec * 1000).toLong()) {
                    cooldowns[block.id] = now
                    executeSequence(block.subBlocks)
                }
            }

            BlockType.MOTOR_4 -> {
                val a = block.params.getOrNull(0)?.let { (it as? BlockParam.NumberInput)?.value }?.toInt() ?: 0
                val b = block.params.getOrNull(1)?.let { (it as? BlockParam.NumberInput)?.value }?.toInt() ?: 0
                val c = block.params.getOrNull(2)?.let { (it as? BlockParam.NumberInput)?.value }?.toInt() ?: 0
                val d = block.params.getOrNull(3)?.let { (it as? BlockParam.NumberInput)?.value }?.toInt() ?: 0
                bleManager.sendDrivePacket(a, b, c, d)
            }
            BlockType.LATCH_GET -> { /* value block, returns in expression */ }
            BlockType.IF_TRUE_FOR -> { /* handled in executor loop */ }
            BlockType.EDGE_DETECT -> { /* value block */ }
            BlockType.SCHMITT_TRIGGER -> { /* value block */ }
            BlockType.STATE_GET -> { /* value block */ }
            BlockType.STATE_TIME_S -> { /* value block */ }
            BlockType.STATE_ENTER_COUNT -> { /* value block */ }
                        BlockType.LATCH_SET -> {
                latches[txt(0)] = true
            }

            BlockType.LATCH_RESET -> {
                latches.remove(txt(0))
            }

            // Нові блоки — обробка
            BlockType.CONSOLE_LOG -> {
                val msg = if (block.params.isNotEmpty()) {
                    when (val p = block.params[0]) {
                        is com.robocar.app.model.BlockParam.TextInput   -> p.value
                        is com.robocar.app.model.BlockParam.NumberInput -> p.value.toString()
                        else -> ""
                    }
                } else ""
                // log виводиться через стандартний механізм
            }
            BlockType.WAIT_SECONDS -> {
                val sec = num(0)
                delay((sec * 1000).toLong())
            }
            BlockType.VAR_SET, BlockType.VAR_GET, BlockType.VAR_CHANGE,
            BlockType.LOOP_WHILE, BlockType.LOOP_FOR, BlockType.LOOP_FOR_EACH,
            BlockType.LOGIC_IF, BlockType.LOGIC_COMPARE, BlockType.LOGIC_AND_OR,
            BlockType.LOGIC_NOT, BlockType.LOGIC_BOOL,
            BlockType.MATH_NUMBER, BlockType.MATH_ARITH, BlockType.MATH_RANDOM,
            BlockType.MATH_ROUND, BlockType.MATH_MODULO,
            BlockType.SENSOR_GET, BlockType.MOTOR_4,
            BlockType.LOOP_REPEAT_PAUSE, BlockType.WAIT_UNTIL_SENSOR,
            BlockType.WAIT_UNTIL_TRUE_FOR, BlockType.STATE_SET_REASON,
            BlockType.TIMEOUT_DO_UNTIL -> { /* handled by WsExecutor */ }

            else -> { /* unknown block — skip */ }
        }
    }

    private suspend fun replayTrackOnce() {
        if (trackMemory.isEmpty()) return
        for (i in trackMemory.indices) {
            if (shouldStop) return
            val step = trackMemory[i]
            if (i > 0) {
                val delay = step.t - trackMemory[i - 1].t
                if (delay > 0) delay(delay)
            }
            bleManager.sendDrivePacket(step.l, step.r, 0, 0)
        }
        sendCar(0, 0)
    }

    private fun sendCar(l: Int, r: Int) {
        if (isRecordingTrack) {
            trackMemory.add(TrackStep(System.currentTimeMillis(), l, r))
        }
        moveHistory.add(MoveRecord.Move(l, r, 0, 0))
        bleManager.sendCarPacket(l, r)
    }

    fun stop() {
        shouldStop = true
        bleManager.sendCarPacket(0, 0)
    }
}
