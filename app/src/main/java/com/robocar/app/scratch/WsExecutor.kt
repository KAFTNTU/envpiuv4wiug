package com.robocar.app.scratch

import com.robocar.app.ble.BleManager
import com.robocar.app.model.BlockParam
import com.robocar.app.model.BlockType
import com.robocar.app.ble.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

// ─────────────────────────────────────────────────────────────
// WsExecutor — виконує ланцюг блоків workspace
// Аналог BlockExecutor але для WsBlock
// ─────────────────────────────────────────────────────────────
class WsExecutor(
    private val blocks: Map<String, WsBlock>,
    private val bleManager: BleManager,
    private val sensorData: StateFlow<SensorData>,
    private val onLog: (String) -> Unit,
    private val onHighlight: (String?) -> Unit,
    private val isRunning: () -> Boolean,
) {
    // ── Стан State Machine ─────────────────────────────────
    private var currentState: String  = "IDLE"
    private var previousState: String = "IDLE"
    private var stateEnteredAt: Long  = System.currentTimeMillis()
    private val stateEnterCounts = mutableMapOf<String, Int>()
    private val latches = mutableMapOf<String, Boolean>()

    // Зупинка програми
    private fun checkRunning() {
        if (!isRunning()) throw CancellationException("Зупинено")
    }

    // ─────────────────────────────────────────────────────
    // ГОЛОВНИЙ ВХІД — виконати ланцюг від startId
    // ─────────────────────────────────────────────────────
    suspend fun executeChain(startId: String) {
        var id: String? = startId
        while (id != null && isRunning()) {
            val block = blocks[id] ?: break
            checkRunning()
            onHighlight(block.id)
            try {
                executeBlock(block)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onLog("❌ Помилка у ${block.type.label}: ${e.message}")
            }
            id = block.nextId
        }
        onHighlight(null)
    }

    // ─────────────────────────────────────────────────────
    // ВИКОНАННЯ ОДНОГО БЛОКУ
    // ─────────────────────────────────────────────────────
    private suspend fun executeBlock(block: WsBlock) {
        when (block.type) {

            // ── СТАРТ — нічого не робить ─────────────────
            BlockType.START_HAT -> {
                onLog("▶ Програма запущена")
            }

            // ── РУХ: ЇХАТИ L/R ──────────────────────────
            BlockType.ROBOT_MOVE -> {
                val l = block.numParam(0)
                val r = block.numParam(1)
                bleManager.sendDrivePacket(l.toInt(), r.toInt(), 0, 0)
                onLog("🚗 Їхати L=$l R=$r")
            }

            // ── РУХ: ПЛАВНИЙ СТАРТ ───────────────────────
            BlockType.ROBOT_MOVE_SOFT -> {
                val target = block.numParam(0)
                val sec    = block.numParam(1)
                val steps  = (sec * 20).toInt().coerceAtLeast(1)
                val delay  = (sec * 1000 / steps).toLong()
                onLog("🚗 Плавний старт → $target за ${sec}с")
                for (i in 1..steps) {
                    checkRunning()
                    val speed = (target * i / steps).toInt()
                    bleManager.sendDrivePacket(speed, speed, 0, 0)
                    delay(delay)
                }
            }

            // ── РУХ: ПОВОРОТ ─────────────────────────────
            BlockType.ROBOT_TURN -> {
                val dir = block.dropParam(0)
                val sec = block.numParam(1)
                val isLeft = dir == "LEFT"
                val l = if (isLeft) -60 else 60
                val r = if (isLeft) 60 else -60
                bleManager.sendDrivePacket(l, r, 0, 0)
                onLog("↩ Поворот ${if (isLeft) "ліво" else "право"} ${sec}с")
                delay((sec * 1000).toLong())
                bleManager.sendDrivePacket(0, 0, 0, 0)
            }

            // ── РУХ: ШВИДКІСТЬ ───────────────────────────
            BlockType.ROBOT_SET_SPEED -> {
                val speed = block.numParam(0)
                onLog("⚡ Швидкість $speed%")
                // Відповідно до поточного руху — тут зберігаємо як модифікатор
                // (в реальному проєкті: зберегти в контексті і множити на усі команди)
            }

            // ── РУХ: СТОП ────────────────────────────────
            BlockType.ROBOT_STOP -> {
                bleManager.sendDrivePacket(0, 0, 0, 0)
                onLog("🛑 Стоп")
            }

            // ── РУХ: ОДИН МОТОР ──────────────────────────
            BlockType.MOTOR_SINGLE -> {
                val motor = block.dropParam(0).toIntOrNull() ?: 1
                val speed = block.numParam(1)
                val p = IntArray(4)
                p[motor - 1] = speed.toInt()
                bleManager.sendDrivePacket(p[0], p[1], p[2], p[3])
                onLog("⚙ Мотор $motor → $speed")
            }

            // ── РУХ: 4 МОТОРИ ────────────────────────────
            BlockType.MOTOR_4 -> {
                val a = block.numParam(0).toInt()
                val b = block.numParam(1).toInt()
                val c = block.numParam(2).toInt()
                val d = block.numParam(3).toInt()
                bleManager.sendDrivePacket(a, b, c, d)
                onLog("⚙ ABCD=$a,$b,$c,$d")
            }

            // ── ЧЕКАТИ (сек) ─────────────────────────────
            BlockType.WAIT_SECONDS -> {
                val sec = block.numParam(0)
                onLog("⏳ Чекати ${sec}с")
                delay((sec * 1000).toLong())
            }

            // ── ЦИКЛ НАЗАВЖДИ ────────────────────────────
            BlockType.LOOP_FOREVER -> {
                onLog("🔁 Цикл назавжди")
                while (isRunning()) {
                    checkRunning()
                    block.subChainId?.let { executeChain(it) }
                    // Невелика пауза щоб не заблокувати coroutine
                    delay(10)
                }
            }

            // ── ЦИКЛ N РАЗІВ ─────────────────────────────
            BlockType.LOOP_REPEAT -> {
                val n = block.numParam(0).toInt()
                onLog("🔁 Повторити $n разів")
                repeat(n) { i ->
                    checkRunning()
                    onLog("  ↳ Ітерація ${i + 1}/$n")
                    block.subChainId?.let { executeChain(it) }
                }
            }

            // ── ЦИКЛ N РАЗІВ З ПАУЗОЮ ────────────────────
            BlockType.LOOP_REPEAT_PAUSE -> {
                val n     = block.numParam(0).toInt()
                val pause = block.numParam(1)
                onLog("🔁 Повторити $n разів (пауза ${pause}с)")
                repeat(n) { i ->
                    checkRunning()
                    block.subChainId?.let { executeChain(it) }
                    if (i < n - 1) delay((pause * 1000).toLong())
                }
            }

            // ── ЦИКЛ КОЖНІ N СЕКУНД ──────────────────────
            BlockType.LOOP_EVERY_SEC -> {
                val interval = block.numParam(0)
                onLog("⏱ Кожні ${interval}с")
                while (isRunning()) {
                    checkRunning()
                    block.subChainId?.let { executeChain(it) }
                    delay((interval * 1000).toLong())
                }
            }

            // ── СКИНУТИ ТАЙМЕР ───────────────────────────
            BlockType.TIMER_RESET -> {
                stateEnteredAt = System.currentTimeMillis()
                onLog("⏱ Таймер скинуто")
            }

            // ── ЧЕКАТИ ПОКИ СЕНСОР ───────────────────────
            BlockType.WAIT_UNTIL_SENSOR -> {
                val port  = block.dropParam(0).toIntOrNull() ?: 0
                val cond  = block.dropParam(1)
                val value = block.numParam(2)
                onLog("📡 Чекати: сенсор $port $cond $value")
                while (isRunning()) {
                    checkRunning()
                    val sensor = getSensor(port)
                    val triggered = if (cond == "LT") sensor < value else sensor > value
                    if (triggered) break
                    delay(50)
                }
                onLog("  ✓ Умова виконана")
            }

            // ── ЧЕКАТИ ПОКИ УМОВА (з часом) ──────────────
            BlockType.WAIT_UNTIL_TRUE_FOR -> {
                val port     = block.dropParam(0).toIntOrNull() ?: 0
                val cond     = block.dropParam(1)
                val value    = block.numParam(2)
                val duration = block.numParam(3)
                onLog("📡 Чекати поки умова тримається ${duration}с")
                var trueSince = 0L
                while (isRunning()) {
                    checkRunning()
                    val sensor = getSensor(port)
                    val ok = if (cond == "LT") sensor < value else sensor > value
                    if (ok) {
                        if (trueSince == 0L) trueSince = System.currentTimeMillis()
                        val elapsed = (System.currentTimeMillis() - trueSince) / 1000f
                        if (elapsed >= duration) break
                    } else {
                        trueSince = 0L
                    }
                    delay(50)
                }
                onLog("  ✓ Умова тривала ${duration}с")
            }

            // ── РОБИТИ ДО УМОВИ (з таймаутом) ────────────
            BlockType.TIMEOUT_DO_UNTIL -> {
                val port    = block.dropParam(0).toIntOrNull() ?: 0
                val cond    = block.dropParam(1)
                val value   = block.numParam(2)
                val maxSec  = block.numParam(3)
                val started = System.currentTimeMillis()
                onLog("⏳ Робити до умови (макс ${maxSec}с)")
                while (isRunning()) {
                    checkRunning()
                    val elapsed = (System.currentTimeMillis() - started) / 1000f
                    if (elapsed >= maxSec) { onLog("  ⌛ Таймаут"); break }
                    val sensor = getSensor(port)
                    val done = if (cond == "LT") sensor < value else sensor > value
                    if (done) { onLog("  ✓ Умова виконана"); break }
                    block.subChainId?.let { executeChain(it) }
                    delay(10)
                }
            }

            // ── НЕ ЧАСТІШЕ НІЖ N СЕК ─────────────────────
            BlockType.COOLDOWN_DO -> {
                val cooldown = block.numParam(0)
                val elapsed  = (System.currentTimeMillis() - stateEnteredAt) / 1000f
                if (elapsed >= cooldown) {
                    onLog("🕒 Cooldown: виконую")
                    stateEnteredAt = System.currentTimeMillis()
                    block.subChainId?.let { executeChain(it) }
                } else {
                    onLog("🕒 Cooldown: пропускаю (${elapsed.format(1)}/${cooldown}с)")
                }
            }

            // ── СТАН = ───────────────────────────────────
            BlockType.STATE_SET -> {
                val newState = block.textParam(0)
                previousState = currentState
                currentState  = newState
                stateEnteredAt = System.currentTimeMillis()
                stateEnterCounts[newState] = (stateEnterCounts[newState] ?: 0) + 1
                onLog("🧠 Стан → $newState")
            }

            // ── СТАН = (з причиною) ───────────────────────
            BlockType.STATE_SET_REASON -> {
                val newState = block.textParam(0)
                val reason   = block.textParam(1)
                previousState = currentState
                currentState  = newState
                stateEnteredAt = System.currentTimeMillis()
                stateEnterCounts[newState] = (stateEnterCounts[newState] ?: 0) + 1
                onLog("🧠 Стан → $newState (причина: $reason)")
            }

            // ── ПОВЕРНУТИСЬ У ПОПЕРЕДНІЙ СТАН ────────────
            BlockType.STATE_PREV -> {
                val tmp = currentState
                currentState = previousState
                previousState = tmp
                stateEnteredAt = System.currentTimeMillis()
                onLog("🔙 Стан → $currentState (повернення)")
            }

            // ── ЯКЩО СТАН = ──────────────────────────────
            BlockType.STATE_IF -> {
                val targetState = block.textParam(0)
                if (currentState == targetState) {
                    onLog("🧠 Стан == $targetState → DO")
                    block.subChainId?.let { executeChain(it) }
                } else {
                    onLog("🧠 Стан != $targetState → ELSE")
                    block.sub2ChainId?.let { executeChain(it) }
                }
            }

            // ── ПРАПОР ВСТАНОВИТИ ─────────────────────────
            BlockType.LATCH_SET -> {
                val flag = block.textParam(0)
                latches[flag] = true
                onLog("🚩 Прапор $flag = true")
            }

            // ── ПРАПОР СКИНУТИ ────────────────────────────
            BlockType.LATCH_RESET -> {
                val flag = block.textParam(0)
                latches[flag] = false
                onLog("🚩 Прапор $flag = false")
            }

            // ── ЯКЩО УМОВА ТРИМАЄТЬСЯ ────────────────────
            BlockType.IF_TRUE_FOR -> {
                val duration = block.numParam(0)
                onLog("⏱ IF умова тримається ${duration}с")
                var trueSince = 0L
                var triggered = false
                while (isRunning()) {
                    checkRunning()
                    val now = System.currentTimeMillis()
                    if (trueSince == 0L) trueSince = now
                    val elapsed = (now - trueSince) / 1000f
                    if (elapsed >= duration) {
                        triggered = true
                        break
                    }
                    delay(50)
                }
                if (triggered) {
                    onLog("  → DO")
                    block.subChainId?.let { executeChain(it) }
                } else {
                    onLog("  → ELSE")
                    block.sub2ChainId?.let { executeChain(it) }
                }
            }

            // ── АВТОПІЛОТ ─────────────────────────────────
            BlockType.AUTOPILOT -> {
                val port      = block.dropParam(0).toIntOrNull() ?: 0
                val turnDir   = block.dropParam(1)
                val threshold = block.numParam(2)
                val speed     = block.numParam(3).toInt()
                onLog("🤖 Автопілот порт=$port поріг=$threshold швидкість=$speed")
                while (isRunning()) {
                    checkRunning()
                    val sensor = getSensor(port)
                    if (sensor < threshold) {
                        // Поворот
                        val l = if (turnDir == "RIGHT") speed else -speed / 2
                        val r = if (turnDir == "RIGHT") -speed / 2 else speed
                        bleManager.sendDrivePacket(l, r, 0, 0)
                    } else {
                        bleManager.sendDrivePacket(speed, speed, 0, 0)
                    }
                    delay(50)
                }
            }

            // ── МАТЕМАТИКА (значення) ─────────────────────
            BlockType.TIMER_GET -> {
                val elapsed = (System.currentTimeMillis() - stateEnteredAt) / 1000f
                onLog("⏱ Таймер = ${elapsed.format(2)}с")
            }

            BlockType.MATH_SMOOTH -> {
                onLog("〰 Згладити (пасивний)")
            }

            BlockType.MATH_PID -> {
                onLog("📐 PID (пасивний)")
            }

            // ── ЗАПИС ТРАСИ ───────────────────────────────
            BlockType.RECORD_START -> {
                onLog("⏺ Запис траси (заглушка)")
            }

            // ── ВІДТВОРЕННЯ ТРАСИ ─────────────────────────
            BlockType.REPLAY_TRACK -> {
                onLog("▶ Відтворити трасу (заглушка)")
            }

            BlockType.REPLAY_LOOP -> {
                val n = block.numParam(0).toInt()
                onLog("▶ Відтворити трасу $n разів (заглушка)")
            }

            // ── ВСЕ ІНШЕ ─────────────────────────────────
            else -> {
                onLog("⚙ ${block.type.label} (пасивний)")
            }
        }
    }

    // ─────────────────────────────────────────────────────
    // ПОМІЧНИКИ
    // ─────────────────────────────────────────────────────
    private fun WsBlock.numParam(index: Int): Float {
        val p = params.getOrNull(index)
        return (p as? BlockParam.NumberInput)?.value ?: 0f
    }

    private fun WsBlock.dropParam(index: Int): String {
        val p = params.getOrNull(index)
        return (p as? BlockParam.DropdownInput)?.selected ?: ""
    }

    private fun WsBlock.textParam(index: Int): String {
        val p = params.getOrNull(index)
        return (p as? BlockParam.TextInput)?.value ?: ""
    }

    private fun getSensor(port: Int): Float {
        val s = sensorData.value
        return when (port) {
            0 -> s.p1.toFloat()
            1 -> s.p2.toFloat()
            2 -> s.p3.toFloat()
            3 -> s.p4.toFloat()
            else -> 0f
        }
    }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)
}
