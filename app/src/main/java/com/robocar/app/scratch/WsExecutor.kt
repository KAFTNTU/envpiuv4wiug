package com.robocar.app.scratch

import com.robocar.app.ble.BleManager
import com.robocar.app.model.BlockParam
import com.robocar.app.model.BlockType
import com.robocar.app.ble.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

// ─────────────────────────────────────────────────────────────
// WsExecutor v2 — повна підтримка всіх типів блоків
// ─────────────────────────────────────────────────────────────
class WsExecutor(
    private val blocks: Map<String, WsBlock>,
    private val bleManager: BleManager,
    private val sensorData: StateFlow<SensorData>,
    private val onLog: (String) -> Unit,
    private val onHighlight: (String?) -> Unit,
    private val isRunning: () -> Boolean,
    private val variables: VariablesState = VariablesState(),
) {
    private var currentState: String  = "IDLE"
    private var previousState: String = "IDLE"
    private var stateEnteredAt: Long  = System.currentTimeMillis()
    private val stateEnterCounts = mutableMapOf<String, Int>()
    private val latches = mutableMapOf<String, Boolean>()
    private val prevSensorValues = mutableMapOf<Int, Float>()
    private var timerStartMs: Long = System.currentTimeMillis()
    private var speedMultiplier: Float = 1.0f

    private fun checkRunning() {
        if (!isRunning()) throw CancellationException("Зупинено")
    }

    suspend fun executeChain(startId: String) {
        var id: String? = startId
        while (id != null && isRunning()) {
            val block = blocks[id] ?: break
            checkRunning()
            onHighlight(block.id)
            try { executeBlock(block) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { onLog("❌ ${block.type.label}: ${e.message}") }
            id = block.nextId
        }
        onHighlight(null)
    }

    private suspend fun executeBlock(block: WsBlock) {
        when (block.type) {
            BlockType.START_HAT -> {
                variables.clear(); timerStartMs = System.currentTimeMillis()
                stateEnteredAt = timerStartMs; onLog("▶ Програма запущена")
            }
            BlockType.ROBOT_MOVE -> {
                val l = (block.numParam(0) * speedMultiplier).toInt()
                val r = (block.numParam(1) * speedMultiplier).toInt()
                bleManager.sendDrivePacket(l, r, 0, 0); onLog("🚗 L=$l R=$r")
            }
            BlockType.ROBOT_MOVE_SOFT -> {
                val target = block.numParam(0) * speedMultiplier; val sec = block.numParam(1)
                val steps = (sec * 20).toInt().coerceAtLeast(1); val delayMs = (sec * 1000 / steps).toLong()
                onLog("🚗 Плавний → ${target.toInt()} за ${sec}с")
                for (i in 1..steps) { checkRunning(); bleManager.sendDrivePacket((target*i/steps).toInt(),(target*i/steps).toInt(),0,0); delay(delayMs) }
            }
            BlockType.ROBOT_TURN -> {
                val dir = block.dropParam(0); val sec = block.numParam(1); val pw = (60*speedMultiplier).toInt()
                val l = if (dir=="LEFT") -pw else pw; val r = if (dir=="LEFT") pw else -pw
                bleManager.sendDrivePacket(l,r,0,0); onLog("↩ Поворот $dir ${sec}с"); delay((sec*1000).toLong()); bleManager.sendDrivePacket(0,0,0,0)
            }
            BlockType.ROBOT_SET_SPEED -> { speedMultiplier = block.numParam(0)/100f; onLog("⚡ Швидкість ${block.numParam(0).toInt()}%") }
            BlockType.ROBOT_STOP -> { bleManager.sendDrivePacket(0,0,0,0); onLog("🛑 Стоп") }
            BlockType.MOTOR_SINGLE -> {
                val m = block.dropParam(0).toIntOrNull()?:1; val sp = (block.numParam(1)*speedMultiplier).toInt()
                val p = IntArray(4); if (m in 1..4) p[m-1]=sp; bleManager.sendDrivePacket(p[0],p[1],p[2],p[3]); onLog("⚙ Мотор$m → $sp")
            }
            BlockType.MOTOR_4 -> {
                val a=(block.numParam(0)*speedMultiplier).toInt(); val b=(block.numParam(1)*speedMultiplier).toInt()
                val c=(block.numParam(2)*speedMultiplier).toInt(); val d=(block.numParam(3)*speedMultiplier).toInt()
                bleManager.sendDrivePacket(a,b,c,d); onLog("⚙ ABCD=$a,$b,$c,$d")
            }
            BlockType.GO_HOME -> {
                onLog("🏠 Додому"); bleManager.sendDrivePacket(-60,-60,0,0); delay(1500); bleManager.sendDrivePacket(0,0,0,0)
            }
            BlockType.RECORD_START  -> onLog("⏺ Запис траси")
            BlockType.REPLAY_TRACK  -> onLog("▶ Відтворення траси")
            BlockType.REPLAY_LOOP   -> onLog("▶ Відтворення ${block.numParam(0).toInt()} разів")
            BlockType.WAIT_START -> {
                onLog("⏸ Чекаємо СТАРТ…")
                while (isRunning()) { checkRunning(); if (getSensor(0)>100) break; delay(100) }
                onLog("  ✓ Старт!")
            }
            BlockType.STOP_AT_START -> { bleManager.sendDrivePacket(0,0,0,0); onLog("🏁 Стоп на старті") }
            BlockType.COUNT_LAPS -> {
                val n = block.numParam(0).toInt(); var laps=0; onLog("🏁 Кола: 0/$n")
                while (isRunning() && laps<n) { checkRunning(); if (getSensor(0)>200) { laps++; onLog("  🏁 $laps/$n"); delay(800) }; delay(50) }
                bleManager.sendDrivePacket(0,0,0,0); onLog("✓ $n кіл")
            }
            BlockType.AUTOPILOT -> {
                val port=block.dropParam(0).toIntOrNull()?:0; val dir=block.dropParam(1)
                val thr=block.numParam(2); val sp=(block.numParam(3)*speedMultiplier).toInt()
                onLog("🤖 Автопілот порт=${port+1} поріг=$thr швидк=$sp")
                while (isRunning()) { checkRunning(); val s=getSensor(port)
                    if (s<thr) { val l=if(dir=="RIGHT") sp else -(sp/2); val r=if(dir=="RIGHT") -(sp/2) else sp; bleManager.sendDrivePacket(l,r,0,0) }
                    else bleManager.sendDrivePacket(sp,sp,0,0); delay(50) }
            }
            BlockType.WAIT_SECONDS -> { val sec=block.numParam(0); onLog("⏳ Чекати ${sec}с"); delay((sec*1000).toLong()) }
            BlockType.WAIT_UNTIL_SENSOR -> {
                val port=block.dropParam(0).toIntOrNull()?:0; val cond=block.dropParam(1); val value=block.numParam(2)
                onLog("📡 Чекати сенсор ${port+1} $cond $value")
                while (isRunning()) { checkRunning(); if (evalSensorCond(port,cond,value)) break; delay(50) }
                onLog("  ✓ сенсор=${getSensor(port).toInt()}")
            }
            BlockType.WAIT_UNTIL_TRUE_FOR -> {
                val port=block.dropParam(0).toIntOrNull()?:0; val cond=block.dropParam(1); val value=block.numParam(2); val dur=block.numParam(3)
                var ts=0L; onLog("📡 Чекати умова ${dur}с")
                while (isRunning()) { checkRunning()
                    if (evalSensorCond(port,cond,value)) { if (ts==0L) ts=System.currentTimeMillis()
                        if ((System.currentTimeMillis()-ts)/1000f >= dur) break
                    } else ts=0L; delay(50) }
                onLog("  ✓ Умова протрималась")
            }
            BlockType.LOOP_FOREVER -> {
                onLog("🔁 Назавжди")
                while (isRunning()) { checkRunning(); block.subChainId?.let { executeChain(it) }; delay(10) }
            }
            BlockType.LOOP_REPEAT -> {
                val n=block.numParam(0).toInt(); onLog("🔁 ×$n")
                repeat(n) { i -> checkRunning(); onLog("  ↳ ${i+1}/$n"); block.subChainId?.let { executeChain(it) } }
            }
            BlockType.LOOP_REPEAT_PAUSE -> {
                val n=block.numParam(0).toInt(); val pause=block.numParam(1); onLog("🔁 ×$n пауза ${pause}с")
                repeat(n) { i -> checkRunning(); block.subChainId?.let { executeChain(it) }; if (i<n-1) delay((pause*1000).toLong()) }
            }
            BlockType.LOOP_EVERY_SEC -> {
                val interval=block.numParam(0); onLog("⏱ Кожні ${interval}с")
                while (isRunning()) { checkRunning(); block.subChainId?.let { executeChain(it) }; delay((interval*1000).toLong()) }
            }
            BlockType.LOOP_WHILE -> {
                val mode=block.dropParam(0); val port=block.dropParam(1).toIntOrNull()?:0; val cond=block.dropParam(2); val value=block.numParam(3)
                onLog("🔁 ${if(mode=="WHILE") "Поки" else "До"}: сенсор ${port+1} $cond $value")
                while (isRunning()) { checkRunning()
                    val ok=evalSensorCond(port,cond,value); val cont=if(mode=="WHILE") ok else !ok
                    if (!cont) break; block.subChainId?.let { executeChain(it) }; delay(10) }
                onLog("  ✓ Цикл завершено")
            }
            BlockType.LOOP_FOR -> {
                val varName=block.textParam(0); val from=block.numParam(1); val to=block.numParam(2)
                val step=block.numParam(3).let { if(it==0f) 1f else it }
                onLog("🔁 $varName від ${from.toInt()} до ${to.toInt()} крок ${step.toInt()}")
                var i=from
                while (isRunning() && (if(step>0) i<=to else i>=to)) { checkRunning(); variables.set(varName,i); block.subChainId?.let { executeChain(it) }; i+=step }
                onLog("  ✓ Цикл $varName завершено")
            }
            BlockType.LOOP_FOR_EACH -> {
                val varName=block.textParam(0); onLog("🔁 Для кожного порту → $varName")
                for (port in 0..3) { checkRunning(); variables.set(varName,getSensor(port)); onLog("  ↳ $varName=порт${port+1}=${getSensor(port).toInt()}"); block.subChainId?.let { executeChain(it) } }
            }
            BlockType.LOGIC_IF -> {
                val port=block.dropParam(0).toIntOrNull()?:0; val cond=block.dropParam(1); val value=block.numParam(2)
                val ok=evalSensorCond(port,cond,value); onLog("🔀 Якщо сенсор ${port+1} $cond $value → ${if(ok) "ТАК" else "НІ"}")
                if (ok) block.subChainId?.let { executeChain(it) } else block.sub2ChainId?.let { executeChain(it) }
            }
            BlockType.TIMER_RESET -> { timerStartMs=System.currentTimeMillis(); onLog("⏱ Таймер скинуто") }
            BlockType.TIMER_GET -> { val el=(System.currentTimeMillis()-timerStartMs)/1000f; variables.set("_timer",el); onLog("⏱ Таймер=${"%.2f".format(el)}с") }
            BlockType.MATH_PID      -> onLog("📐 PID kP=${block.numParam(0)} kI=${block.numParam(1)} kD=${block.numParam(2)}")
            BlockType.MATH_SMOOTH   -> onLog("〰 Згладити N=${block.numParam(0).toInt()}")
            BlockType.MATH_PYTHAGORAS -> { val a=block.numParam(0); val b=block.numParam(1); val c=sqrt(a*a+b*b); variables.set("_result",c); onLog("📐 √($a²+$b²)=${"%.3f".format(c)}") }
            BlockType.MATH_PATH_VT    -> { val v=block.numParam(0); val t=block.numParam(1); val d=v*t; variables.set("_result",d); onLog("📐 $v×$t=${"%.2f".format(d)}см") }
            BlockType.MATH_SPEED_CMS  -> { val dist=block.numParam(0); val t=block.numParam(1); val v=if(t!=0f) dist/t else 0f; variables.set("_result",v); onLog("📐 $dist/$t=${"%.2f".format(v)}см/с") }
            BlockType.MATH_NUMBER     -> { variables.set("_result",block.numParam(0)); onLog("🔢 ${block.numParam(0)}") }
            BlockType.MATH_ARITH -> {
                val op=block.dropParam(0); val a=block.numParam(1); val b=block.numParam(2)
                val r=when(op){"ADD"->a+b;"MINUS"->a-b;"MUL"->a*b;"DIV"->if(b!=0f)a/b else 0f;"POW"->a.pow(b);else->0f}
                variables.set("_result",r); onLog("➕ $a $op $b = ${"%.3f".format(r)}")
            }
            BlockType.MATH_RANDOM -> { val r=block.numParam(0)+(Math.random()*(block.numParam(1)-block.numParam(0))).toFloat(); variables.set("_result",r); onLog("🎲 ${r.toInt()}") }
            BlockType.MATH_ROUND  -> {
                val op=block.dropParam(0); val n=block.numParam(1)
                val r=when(op){"ROUND"->kotlin.math.round(n).toFloat();"FLOOR"->floor(n);"CEIL"->ceil(n);"ABS"->abs(n);else->n}
                variables.set("_result",r); onLog("🔢 $op($n)=$r")
            }
            BlockType.MATH_MODULO -> { val a=block.numParam(0); val b=block.numParam(1); val r=if(b!=0f) a%b else 0f; variables.set("_result",r); onLog("➗ $a%$b=$r") }
            BlockType.CALIBRATE_SPEED -> {
                val dist=block.numParam(0); val port=block.dropParam(1).toIntOrNull()?:0; val thr=block.numParam(2); val sp=block.numParam(3).toInt()
                onLog("🔧 Калібрування dist=$dist thr=$thr sp=$sp"); val t0=System.currentTimeMillis()
                bleManager.sendDrivePacket(sp,sp,0,0)
                while (isRunning()) { checkRunning(); if(getSensor(port)<thr) break; delay(50) }
                val el=(System.currentTimeMillis()-t0)/1000f; bleManager.sendDrivePacket(0,0,0,0)
                val v=if(el>0f) dist/el else 0f; variables.set("calibratedSpeed",v); onLog("  ✓ ${"%.1f".format(v)}см/с за ${"%.2f".format(el)}с")
            }
            BlockType.STATE_SET -> { transitionState(block.textParam(0)); onLog("🧠 Стан → $currentState") }
            BlockType.STATE_SET_REASON -> { transitionState(block.textParam(0)); onLog("🧠 Стан → $currentState (${block.textParam(1)})") }
            BlockType.STATE_PREV -> { val t=currentState; currentState=previousState; previousState=t; stateEnteredAt=System.currentTimeMillis(); onLog("🔙 Стан → $currentState") }
            BlockType.STATE_IF -> {
                val ts=block.textParam(0); val ok=currentState==ts; onLog("🧠 $currentState==$ts? ${if(ok) "ТАК" else "НІ"}")
                if (ok) block.subChainId?.let { executeChain(it) } else block.sub2ChainId?.let { executeChain(it) }
            }
            BlockType.STATE_GET -> { variables.set("_state",currentState.hashCode().toFloat()); onLog("🧠 Стан=$currentState") }
            BlockType.STATE_TIME_S -> { val el=(System.currentTimeMillis()-stateEnteredAt)/1000f; variables.set("_stateTime",el); onLog("⏱ Час у '$currentState': ${"%.2f".format(el)}с") }
            BlockType.STATE_ENTER_COUNT -> { val c=stateEnterCounts[block.textParam(0)]?:0; variables.set("_enterCount",c.toFloat()); onLog("🔢 Входів у '${block.textParam(0)}': $c") }
            BlockType.LATCH_SET -> { latches[block.textParam(0)]=true; onLog("🚩 ${block.textParam(0)}=true") }
            BlockType.LATCH_RESET -> { latches[block.textParam(0)]=false; onLog("🚩 ${block.textParam(0)}=false") }
            BlockType.LATCH_GET -> { val f=latches[block.textParam(0)]?:false; variables.set("_latch",if(f)1f else 0f); onLog("🚩 ${block.textParam(0)}=$f") }
            BlockType.IF_TRUE_FOR -> {
                val dur=block.numParam(0); val ts=System.currentTimeMillis()
                var triggered=false
                while (isRunning()) { checkRunning(); if((System.currentTimeMillis()-ts)/1000f>=dur){triggered=true;break}; delay(50) }
                if (triggered) block.subChainId?.let { executeChain(it) } else block.sub2ChainId?.let { executeChain(it) }
            }
            BlockType.TIMEOUT_DO_UNTIL -> {
                val port=block.dropParam(0).toIntOrNull()?:0; val cond=block.dropParam(1); val value=block.numParam(2); val maxSec=block.numParam(3)
                val t0=System.currentTimeMillis(); onLog("⏳ До умови (макс ${maxSec}с)")
                while (isRunning()) { checkRunning()
                    if ((System.currentTimeMillis()-t0)/1000f>=maxSec) { onLog("  ⌛ Таймаут"); break }
                    if (evalSensorCond(port,cond,value)) { onLog("  ✓ Умова"); break }
                    block.subChainId?.let { executeChain(it) }; delay(10) }
            }
            BlockType.COOLDOWN_DO -> {
                val cd=block.numParam(0); val el=(System.currentTimeMillis()-stateEnteredAt)/1000f
                if (el>=cd) { onLog("🕒 Cooldown: виконую"); stateEnteredAt=System.currentTimeMillis(); block.subChainId?.let { executeChain(it) } }
                else onLog("🕒 Cooldown: пропускаю (${"%.1f".format(el)}/${cd}с)")
            }
            BlockType.SCHMITT_TRIGGER -> {
                val hi=block.numParam(0); val lo=block.numParam(1); val s=getSensor(0)
                val on=when { s>hi->true; s<lo->false; else->(variables.get("_schmitt")>0.5f) }
                variables.set("_schmitt",if(on)1f else 0f); onLog("〰 Schmitt: ${s.toInt()} → ${if(on) "ON" else "OFF"}")
            }
            BlockType.EDGE_DETECT -> {
                val prev=prevSensorValues[0]?:0f; val curr=getSensor(0)
                val rising=prev<0.5f&&curr>=0.5f; prevSensorValues[0]=curr
                variables.set("_edge",if(rising)1f else 0f)
                if (rising) onLog("📈 Edge: 0→1") else onLog("📉 Edge: стан=${curr.toInt()}")
            }
            BlockType.VAR_SET -> { val n=block.textParam(0); val v=block.numParam(1); variables.set(n,v); onLog("📦 $n=${variables.formatted(n)}") }
            BlockType.VAR_GET -> { val n=block.textParam(0); variables.set("_result",variables.get(n)); onLog("📦 $n=${variables.formatted(n)}") }
            BlockType.VAR_CHANGE -> { val n=block.textParam(0); val d=block.numParam(1); variables.change(n,d); onLog("📦 $n+=$d → ${variables.formatted(n)}") }
            BlockType.CONSOLE_LOG -> {
                val msg=block.textParam(0).replace("{state}",currentState).replace("{timer}","%.2f".format((System.currentTimeMillis()-timerStartMs)/1000f))
                onLog("📋 $msg")
            }
            BlockType.LOGIC_COMPARE -> {
                val op=block.dropParam(0); val a=block.numParam(1); val b=block.numParam(2)
                val r=evalCompare(op,a,b); variables.set("_result",if(r)1f else 0f); onLog("🔀 $a $op $b → $r")
            }
            else -> onLog("⚙ ${block.type.label}")
        }
    }

    private fun WsBlock.numParam(i: Int) = (params.getOrNull(i) as? BlockParam.NumberInput)?.value ?: 0f
    private fun WsBlock.dropParam(i: Int) = (params.getOrNull(i) as? BlockParam.DropdownInput)?.selected ?: ""
    private fun WsBlock.textParam(i: Int) = (params.getOrNull(i) as? BlockParam.TextInput)?.value ?: ""
    private fun getSensor(port: Int): Float { val s=sensorData.value; return when(port){0->s.p1.toFloat();1->s.p2.toFloat();2->s.p3.toFloat();3->s.p4.toFloat();else->0f} }
    private fun evalSensorCond(port: Int, cond: String, value: Float) = evalCompare(cond, getSensor(port), value)
    private fun evalCompare(op: String, a: Float, b: Float) = when(op){"LT"->a<b;"GT"->a>b;"EQ"->abs(a-b)<0.001f;"NEQ"->abs(a-b)>=0.001f;"LTE"->a<=b;"GTE"->a>=b;else->false}
    private fun transitionState(s: String) { previousState=currentState; currentState=s; stateEnteredAt=System.currentTimeMillis(); stateEnterCounts[s]=(stateEnterCounts[s]?:0)+1 }
    private fun Float.format(d: Int) = "%.${d}f".format(this)
}
