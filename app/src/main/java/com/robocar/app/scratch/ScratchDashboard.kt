package com.robocar.app.scratch

import androidx.compose.animation.*
import com.robocar.app.model.BlockType
import com.robocar.app.model.BlockCategory
import com.robocar.app.model.BlockParam
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.robocar.app.MainViewModel

// ─────────────────────────────────────────────────────────────
// SENSOR DASHBOARD — смуга зверху scratch view
// Точно як в оригінальному веб-додатку:
// Port 1 | Port 2 | Port 3 | Port 4
// Зелений якщо > 30 (активний сенсор)
// ─────────────────────────────────────────────────────────────
@Composable
fun SensorDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val sensorData by viewModel.sensorData.collectAsState()

    Surface(
        color    = Color(0xDD0A0E1A),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Port 1
            SensorItem(label = "Port 1", value = sensorData.p1)
            SensorDivider()
            // Port 2
            SensorItem(label = "Port 2", value = sensorData.p2)
            SensorDivider()
            // Port 3
            SensorItem(label = "Port 3", value = sensorData.p3)
            SensorDivider()
            // Port 4
            SensorItem(label = "Port 4", value = sensorData.p4)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SENSOR ITEM — один порт: мітка + значення
// Точна копія .sensor-item з CSS:
//   .sensor-label { font-size: 10px; color: #94a3b8; uppercase }
//   .sensor-value { font-family: Courier; font-size: 14px; color: #34d399 }
// ─────────────────────────────────────────────────────────────
@Composable
private fun SensorItem(label: String, value: Int) {
    val active  = value > 30
    val valColor by animateColorAsState(
        targetValue   = if (active) Color(0xFF34D399) else Color(0xFF475569),
        animationSpec = tween(200),
        label         = "sensor_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Мітка: "Port 1" — text-slate-400, uppercase, 10sp
        Text(
            text          = label,
            fontSize      = 10.sp,
            color         = Color(0xFF94A3B8),
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )

        // Значення: зелене якщо активне
        Text(
            text          = value.toString(),
            fontSize      = 14.sp,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            color         = valColor,
        )

        // Маленький індикатор — крапка активності
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF34D399) else Color(0xFF1E293B))
        )
    }
}

@Composable
private fun SensorDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color(0xFF334155))
    )
}

// ─────────────────────────────────────────────────────────────
// RUN FAB — велика кругла кнопка Play/Stop
// Точно як #globalRunBtn у CSS:
//   position: fixed, bottom: 24px, right: 24px
//   width: 64px, height: 64px, border-radius: 50%
//   background: green (play) / red (stop)
//   animation: pulse-red при виконанні
// ─────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────
// GENERATED CODE PANEL — панель з псевдокодом програми
// Аналог showGeneratedCode() в оригіналі
// ─────────────────────────────────────────────────────────────
@Composable
fun GeneratedCodePanel(
    blocks: Map<String, WsBlock>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val code = remember(blocks) { generatePseudoCode(blocks) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        color    = Color(0xFF0A0E1A),
        shape    = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column {
            // Заголовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "КОД ПРОГРАМИ",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color(0xFF60A5FA),
                    letterSpacing = 1.5.sp,
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close, null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Divider(color = Color(0xFF1E293B))

            // Код
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(16.dp),
            ) {
                if (code.isBlank()) {
                    Text(
                        "// Немає блоків на workspace",
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color      = Color(0xFF475569),
                    )
                } else {
                    code.lines().forEachIndexed { idx, line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Номер рядка
                            Text(
                                "${idx + 1}".padStart(3),
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color      = Color(0xFF334155),
                                modifier   = Modifier.width(30.dp),
                            )
                            // Код
                            Text(
                                line,
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color      = codeLineColor(line),
                            )
                        }
                    }
                }
            }
        }
    }
}

// Колір рядка коду за вмістом
private fun codeLineColor(line: String): Color = when {
    line.trimStart().startsWith("//")   -> Color(0xFF475569)
    line.contains("await") ||
    line.contains("sendDrivePacket")    -> Color(0xFF60A5FA)
    line.contains("if") ||
    line.contains("while") ||
    line.contains("for")                -> Color(0xFFC084FC)
    line.contains("var ") ||
    line.contains("let ")               -> Color(0xFFFF8C1A)
    line.contains("delay") ||
    line.contains("setTimeout")         -> Color(0xFFFFBF00)
    else                                -> Color(0xFF94A3B8)
}

// ─────────────────────────────────────────────────────────────
// PSEUDO CODE GENERATOR — генерує псевдокод як в оригіналі
// (оригінал використовує Blockly JS generator, ми робимо схоже)
// ─────────────────────────────────────────────────────────────
fun generatePseudoCode(blocks: Map<String, WsBlock>): String {
    val sb = StringBuilder()
    val roots = rootBlocks(blocks)
    val variables = mutableSetOf<String>()

    // Collect variables first
    for (b in blocks.values) {
        if (b.type == com.robocar.app.model.BlockType.VAR_SET ||
            b.type == com.robocar.app.model.BlockType.VAR_GET ||
            b.type == com.robocar.app.model.BlockType.VAR_CHANGE) {
            val name = b.params.firstOrNull()
                ?.let { it as? com.robocar.app.model.BlockParam.TextInput }?.value
                ?: "x"
            variables.add(name)
        }
    }

    if (variables.isNotEmpty()) {
        sb.appendLine("// Змінні")
        variables.forEach { v ->
            sb.appendLine("var $v = 0;")
        }
        sb.appendLine()
    }

    for (root in roots) {
        if (root.type == com.robocar.app.model.BlockType.START_HAT) {
            sb.appendLine("// ─── Програма ───")
            sb.appendLine("(async () => {")
            generateChainCode(root.nextId, blocks, sb, indent = "  ")
            sb.appendLine("})();")
            sb.appendLine()
        }
    }

    return sb.toString().trimEnd()
}

private fun generateChainCode(
    startId: String?,
    blocks: Map<String, WsBlock>,
    sb: StringBuilder,
    indent: String,
) {
    var id: String? = startId
    while (id != null) {
        val block = blocks[id] ?: break
        generateBlockCode(block, blocks, sb, indent)
        id = block.nextId
    }
}

private fun generateBlockCode(
    block: WsBlock,
    blocks: Map<String, WsBlock>,
    sb: StringBuilder,
    indent: String,
) {
    val p = block.params

    fun num(idx: Int): String =
        (p.getOrNull(idx) as? com.robocar.app.model.BlockParam.NumberInput)?.value?.let {
            if (it == it.toLong().toFloat()) it.toInt().toString() else it.toString()
        } ?: "0"

    fun drop(idx: Int): String =
        (p.getOrNull(idx) as? com.robocar.app.model.BlockParam.DropdownInput)?.selected ?: ""

    fun txt(idx: Int): String =
        (p.getOrNull(idx) as? com.robocar.app.model.BlockParam.TextInput)?.value ?: ""

    fun sensorExpr(portIdx: Int = 0): String =
        "(window.sensorData ? window.sensorData[${drop(portIdx)}] : 0)"

    fun condExpr(portIdx: Int = 0, condIdx: Int = 1, valIdx: Int = 2): String {
        val sensor = sensorExpr(portIdx)
        val cond   = when (drop(condIdx)) { "LT" -> "<"; "GT" -> ">"; else -> "==" }
        return "$sensor $cond ${num(valIdx)}"
    }

    when (block.type) {
        com.robocar.app.model.BlockType.ROBOT_MOVE ->
            sb.appendLine("${indent}await sendDrivePacket(${num(0)}, ${num(1)}, ${num(0)}, ${num(1)});")

        com.robocar.app.model.BlockType.ROBOT_MOVE_SOFT -> {
            sb.appendLine("${indent}// Плавний старт до ${num(0)} за ${num(1)}с")
            sb.appendLine("${indent}for(let _i=0;_i<=${num(1)}*20;_i++){")
            sb.appendLine("${indent}  let _s=Math.round(${num(0)}*_i/(${num(1)}*20));")
            sb.appendLine("${indent}  await sendDrivePacket(_s,_s,0,0);")
            sb.appendLine("${indent}  await new Promise(r=>setTimeout(r,50));")
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.ROBOT_TURN -> {
            val isLeft = drop(0) == "LEFT"
            val l = if (isLeft) -60 else 60
            val r = if (isLeft) 60 else -60
            sb.appendLine("${indent}await sendDrivePacket($l,$r,0,0);")
            sb.appendLine("${indent}await new Promise(r=>setTimeout(r,${num(1)}*1000));")
            sb.appendLine("${indent}await sendDrivePacket(0,0,0,0);")
        }

        com.robocar.app.model.BlockType.ROBOT_STOP ->
            sb.appendLine("${indent}await sendDrivePacket(0,0,0,0);")

        com.robocar.app.model.BlockType.MOTOR_4 ->
            sb.appendLine("${indent}await sendDrivePacket(${num(0)},${num(1)},${num(2)},${num(3)});")

        com.robocar.app.model.BlockType.MOTOR_SINGLE -> {
            val m = drop(0)
            sb.appendLine("${indent}{ let _s=${num(1)};")
            sb.appendLine("${indent}  let _c=window.motorState||{m1:0,m2:0,m3:0,m4:0};")
            sb.appendLine("${indent}  if('$m'=='1')_c.m1=_s; if('$m'=='2')_c.m2=_s;")
            sb.appendLine("${indent}  if('$m'=='3')_c.m3=_s; if('$m'=='4')_c.m4=_s;")
            sb.appendLine("${indent}  await sendDrivePacket(_c.m1,_c.m2,_c.m3,_c.m4); }")
        }

        com.robocar.app.model.BlockType.WAIT_SECONDS ->
            sb.appendLine("${indent}await new Promise(r=>setTimeout(r,${num(0)}*1000));")

        com.robocar.app.model.BlockType.LOOP_FOREVER -> {
            sb.appendLine("${indent}while(true){")
            generateChainCode(block.subChainId, blocks, sb, "$indent  ")
            sb.appendLine("${indent}  await new Promise(r=>setTimeout(r,10));")
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.LOOP_REPEAT -> {
            sb.appendLine("${indent}for(let _i=0;_i<${num(0)};_i++){")
            generateChainCode(block.subChainId, blocks, sb, "$indent  ")
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.LOOP_REPEAT_PAUSE -> {
            sb.appendLine("${indent}for(let _i=0;_i<${num(0)};_i++){")
            generateChainCode(block.subChainId, blocks, sb, "$indent  ")
            sb.appendLine("${indent}  await new Promise(r=>setTimeout(r,${num(1)}*1000));")
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.LOOP_WHILE -> {
            val isWhile = drop(0) == "WHILE"
            val cond    = condExpr(1, 2, 3)
            val whileCond = if (isWhile) cond else "!($cond)"
            sb.appendLine("${indent}while($whileCond){")
            generateChainCode(block.subChainId, blocks, sb, "$indent  ")
            sb.appendLine("${indent}  await new Promise(r=>setTimeout(r,50));")
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.LOOP_FOR -> {
            val varName = txt(0)
            sb.appendLine("${indent}for(let $varName=${num(1)};$varName<=${num(2)};$varName+=${num(3)}){")
            generateChainCode(block.subChainId, blocks, sb, "$indent  ")
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.LOGIC_IF -> {
            sb.appendLine("${indent}if(${condExpr()}){")
            generateChainCode(block.subChainId, blocks, sb, "$indent  ")
            if (block.sub2ChainId != null) {
                sb.appendLine("${indent}} else {")
                generateChainCode(block.sub2ChainId, blocks, sb, "$indent  ")
            }
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.WAIT_UNTIL_SENSOR -> {
            sb.appendLine("${indent}while(!(${condExpr()})){")
            sb.appendLine("${indent}  await new Promise(r=>setTimeout(r,50));")
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.STATE_SET ->
            sb.appendLine("${indent}currentState = '${txt(0)}';")

        com.robocar.app.model.BlockType.STATE_IF -> {
            sb.appendLine("${indent}if(currentState==='${txt(0)}'){")
            generateChainCode(block.subChainId, blocks, sb, "$indent  ")
            if (block.sub2ChainId != null) {
                sb.appendLine("${indent}} else {")
                generateChainCode(block.sub2ChainId, blocks, sb, "$indent  ")
            }
            sb.appendLine("${indent}}")
        }

        com.robocar.app.model.BlockType.VAR_SET ->
            sb.appendLine("${indent}${txt(0)} = ${num(1)};")

        com.robocar.app.model.BlockType.VAR_CHANGE ->
            sb.appendLine("${indent}${txt(0)} += ${num(1)};")

        com.robocar.app.model.BlockType.CONSOLE_LOG ->
            sb.appendLine("${indent}log('LOG: ${txt(0)}', 'info');")

        com.robocar.app.model.BlockType.TIMER_RESET ->
            sb.appendLine("${indent}_timerStart = Date.now();")

        com.robocar.app.model.BlockType.LATCH_SET ->
            sb.appendLine("${indent}latches['${txt(0)}'] = true;")

        com.robocar.app.model.BlockType.LATCH_RESET ->
            sb.appendLine("${indent}latches['${txt(0)}'] = false;")

        com.robocar.app.model.BlockType.ROBOT_STOP ->
            sb.appendLine("${indent}await sendDrivePacket(0,0,0,0);")

        com.robocar.app.model.BlockType.GO_HOME ->
            sb.appendLine("${indent}await goHomeSequence();")

        com.robocar.app.model.BlockType.AUTOPILOT -> {
            sb.appendLine("${indent}// Autopilot: port=${drop(0)}, dir=${drop(1)}, thr=${num(2)}, spd=${num(3)}")
            sb.appendLine("${indent}while(true){")
            sb.appendLine("${indent}  const _s=${sensorExpr()};")
            val dir = drop(1)
            val lSign = if (dir == "RIGHT") "" else "-"
            val rSign = if (dir == "RIGHT") "-" else ""
            sb.appendLine("${indent}  if(_s<${num(2)}){await sendDrivePacket(${lSign}${num(3)},${rSign}${num(3)},0,0);}")
            sb.appendLine("${indent}  else{await sendDrivePacket(${num(3)},${num(3)},0,0);}")
            sb.appendLine("${indent}  await new Promise(r=>setTimeout(r,50));")
            sb.appendLine("${indent}}")
        }

        else -> {
            sb.appendLine("${indent}// ${block.type.label}")
        }
    }
}
