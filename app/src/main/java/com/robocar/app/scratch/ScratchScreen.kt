package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// SCRATCH SCREEN — 1в1 копія оригінального view-builder
//
// Оригінал HTML структура:
//   <header class="h-16 glass-header"> — висота 64dp, #0f172a
//     <span status-dot>              — 8px крапка ліворуч
//     <nav bg-slate-800/50 rounded-xl border-slate-700/50>
//       nav-btn (gamepad, puzzle, lock, book)
//     <button w-10 h-10 rounded-full bg-blue-600> — BT кнопка
//
//   <section view-builder>
//     <div class="sensor-dashboard">
//       Port 1 | Port 2 | Port 3 | Port 4
//     <div id="blocklyDiv">  — workspace canvas
//     <button class="run-fab">  — 80×80 зелена/червона FAB
//   </section>
//   <div class="blocklyToolboxDiv">  — toolbox внизу
// ═══════════════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitPointerEvent
import com.robocar.app.ble.BleState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robocar.app.MainViewModel
import com.robocar.app.ble.BleState
import com.robocar.app.model.BlockType

// ───────────────────────────────────────────────────────────────────────
// ГОЛОВНИЙ ЕКРАН
// ───────────────────────────────────────────────────────────────────────
@Composable
fun ScratchScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val vm: WorkspaceViewModel = viewModel()
    val state      by vm.state.collectAsState()
    val dragState  by vm.dragState.collectAsState()
    val editingBlk by vm.editingBlock.collectAsState()
    val sensorData by mainViewModel.sensorData.collectAsState()
    val bleState   by mainViewModel.bleState.collectAsState()
    val isConnected = bleState is BleState.Connected

    var showLog    by remember { mutableStateOf(false) }
    var contextId  by remember { mutableStateOf<String?>(null) }
    var ctxX       by remember { mutableStateOf(0f) }
    var ctxY       by remember { mutableStateOf(0f) }

    // background-color: #0f172a — точно як в оригіналі
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
    ) {

        // ════════════════════════════════════════════
        // BLOCKLY DIV — workspace (top:64px, bottom:64px)
        // ════════════════════════════════════════════
        WorkspaceCanvas(
            state            = state,
            dragState        = dragState,
            onPan            = { dx, dy -> vm.pan(dx, dy) },
            onZoom           = { f, px, py -> vm.zoom(f, px, py) },
            onBlockTap       = { id ->
                if (state.selectedId == id) vm.openEdit(id)
                else vm.selectBlock(id)
            },
            onBlockLongPress = { id, sx, sy ->
                contextId = id; ctxX = sx; ctxY = sy
            },
            onBlockDragStart = { id, sx, sy ->
                vm.startDragFromWorkspace(id, sx, sy)
            },
            onWorkspaceTap   = { vm.selectBlock(null); contextId = null },
            modifier         = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 130.dp),
        )

        // Drag interceptor
        if (dragState !is DragState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dragState) {
                        awaitEachGesture {
                            while (true) {
                                val ev  = awaitPointerEvent()
                                val pos = ev.changes.firstOrNull()?.position ?: break
                                ev.changes.forEach { it.consume() }
                                vm.updateDrag(pos.x, pos.y)
                                if (!ev.changes.any { it.pressed }) {
                                    vm.endDrag(pos.x, pos.y, size.height.toFloat())
                                    break
                                }
                            }
                        }
                    }
            )
        }

        // ════════════════════════════════════════════
        // HEADER — h-16 (64dp), #0f172a (без скла)
        // ════════════════════════════════════════════
        ScratchHeader(
            isConnected = isConnected,
            isRunning   = state.isRunning,
            onConnect   = { mainViewModel.onConnectClicked() },
            onRun       = { vm.runProgram(mainViewModel) },
            onStop      = { vm.stopProgram() },
            onLog       = { showLog = !showLog },
            onClear     = { vm.clearAll() },
            modifier    = Modifier.align(Alignment.TopCenter),
        )

        // ════════════════════════════════════════════
        // SENSOR DASHBOARD — position: absolute; top: 75px
        // background: rgba(15,23,42,0.95); border-radius: 12px
        // padding: 4px 12px
        // ════════════════════════════════════════════
        SensorDashboard(
            p1 = sensorData.p1,
            p2 = sensorData.p2,
            p3 = sensorData.p3,
            p4 = sensorData.p4,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (64 + 11).dp),   // top: 75px
        )

        // ════════════════════════════════════════════
        // RUN FAB — bottom: 180px, right: 20px
        // width: 80px; height: 80px; border-radius: 50%
        // background: #22c55e  (running → #ef4444)
        // border: 4px solid rgba(255,255,255,0.1)
        // box-shadow: 0 10px 30px rgba(0,0,0,0.5)
        // ════════════════════════════════════════════
        RunFab(
            isRunning = state.isRunning,
            onRun     = { vm.runProgram(mainViewModel) },
            onStop    = { vm.stopProgram() },
            modifier  = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 200.dp),
        )

        // ════════════════════════════════════════════
        // TOOLBOX — blocklyToolboxDiv (внизу)
        // ════════════════════════════════════════════
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            ScratchToolbox(
                onDragBlockStart = { type, sx, sy ->
                    vm.startDragFromToolbar(type, sx, sy)
                },
            )
        }

        // ════════════════════════════════════════════
        // CONTEXT MENU
        // ════════════════════════════════════════════
        contextId?.let { id ->
            state.blocks[id]?.let { block ->
                BlockContextMenu(
                    block       = block,
                    offsetX     = ctxX,
                    offsetY     = ctxY,
                    onDismiss   = { contextId = null },
                    onEdit      = { vm.openEdit(id); contextId = null },
                    onDuplicate = { vm.duplicateBlock(id); contextId = null },
                    onDelete    = { vm.deleteBlock(id); contextId = null },
                )
            }
        }

        // ════════════════════════════════════════════
        // PARAM EDITOR
        // ════════════════════════════════════════════
        editingBlk?.let { block ->
            BlockParamEditorDialog(
                block         = block,
                onDismiss     = { vm.closeEdit() },
                onUpdateParam = { idx, v -> vm.updateParam(block.id, idx, v) },
                onDelete      = { vm.deleteBlock(block.id); vm.closeEdit() },
                onDuplicate   = { vm.duplicateBlock(block.id) },
            )
        }

        // ════════════════════════════════════════════
        // LOG PANEL
        // ════════════════════════════════════════════
        AnimatedVisibility(
            visible  = showLog,
            enter    = slideInVertically(initialOffsetY = { it }),
            exit     = slideOutVertically(targetOffsetY  = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            LogPanel(
                logs    = state.logs,
                onClose = { showLog = false },
            )
        }

        // ════════════════════════════════════════════
        // EMPTY HINT
        // ════════════════════════════════════════════
        if (state.blocks.isEmpty() && dragState is DragState.Idle) {
            EmptyWorkspaceHint(
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ════════════════════════════════════════════
        // RUNNING BADGE
        // ════════════════════════════════════════════
        AnimatedVisibility(
            visible  = state.isRunning,
            enter    = fadeIn() + scaleIn(),
            exit     = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (75 + 48 + 8).dp),
        ) {
            RunningBadge()
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// HEADER — точна копія <header class="h-16 glass-header">
//
// Оригінал: h-16 = 64px, flex items-center justify-between px-4
// Без скла: background: #0f172a (solid, без blur)
// Border bottom: 1px solid rgba(255,255,255,0.08)
//
// Ліворуч:  status-dot (8px circle) + "OFFLINE" текст
// По центру: nav pill — bg-slate-800/50 p-1 rounded-xl border-slate-700/50
//   кнопки: gamepad | puzzle | lock | book
// Праворуч: BT кнопка w-10 h-10 rounded-full bg-blue-600
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun ScratchHeader(
    isConnected: Boolean,
    isRunning: Boolean,
    onConnect: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onLog: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    // h-16 = 64dp, без скла — просто solid #0f172a
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF0F172A))   // без скла!
            .drawBehind {
                // border-bottom: 1px solid rgba(255,255,255,0.08)
                drawLine(
                    color       = Color(0x14FFFFFF),
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = 16.dp),   // px-4
    ) {
        Row(
            modifier              = Modifier.fillMaxSize(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Лівий блок: status-dot + "OFFLINE/ONLINE" ────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // .status-dot { width:8px; height:8px; border-radius:50% }
                // background: #ef4444 (offline) / #22c55e (connected)
                // .connected { box-shadow: 0 0 8px #22c55e }
                StatusDot(isConnected = isConnected)

                // text-[10px] text-slate-500 font-bold uppercase tracking-widest
                Text(
                    text          = if (isConnected) "ONLINE" else "OFFLINE",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = Color(0xFF64748B),  // text-slate-500
                    letterSpacing = 2.sp,
                )
            }

            // ── Nav pill по центру ────────────────────────────────
            // bg-slate-800/50 p-1 rounded-xl border border-slate-700/50
            // Без скла — bg-slate-800 (#1e293b)
            NavPill(
                isRunning = isRunning,
                onRun     = onRun,
                onStop    = onStop,
                onLog     = onLog,
                onClear   = { showClearConfirm = true },
            )

            // ── BT кнопка праворуч ────────────────────────────────
            // w-10 h-10 rounded-full bg-blue-600
            // border border-blue-400/30
            Box(
                modifier = Modifier
                    .size(40.dp)                            // w-10 h-10
                    .clip(CircleShape)                      // rounded-full
                    .background(
                        if (isConnected) Color(0xFF16A34A)
                        else Color(0xFF2563EB)              // bg-blue-600
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x4D60A5FA),         // border-blue-400/30
                        shape = CircleShape,
                    )
                    .clickable(onClick = onConnect),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Bluetooth,
                    contentDescription = "BT",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }

    // Confirm clear dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor   = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor  = Color(0xFF94A3B8),
            title   = { Text("Очистити?") },
            text    = { Text("Видалити всі блоки з workspace?") },
            confirmButton = {
                TextButton(onClick = { onClear(); showClearConfirm = false }) {
                    Text("Так", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Скасувати", color = Color(0xFF64748B))
                }
            }
        )
    }
}

// ── Status Dot ────────────────────────────────────────────────────────
// .status-dot { width:8px; height:8px; border-radius:50%; background:#ef4444 }
// .connected { background:#22c55e; box-shadow:0 0 8px #22c55e }
@Composable
private fun StatusDot(isConnected: Boolean) {
    val dotColor by animateColorAsState(
        targetValue   = if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444),
        animationSpec = tween(300),
        label         = "dot_color",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(dotColor),
    )
}

// ── Nav Pill ─────────────────────────────────────────────────────────
// bg-slate-800/50 p-1 rounded-xl border border-slate-700/50
// Без скла → bg: #1E293B (slate-800), border: #334155 (slate-700)
@Composable
private fun NavPill(
    isRunning: Boolean,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onLog: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))         // rounded-xl
            .background(Color(0xFF1E293B))            // bg-slate-800 (без /50)
            .border(
                width = 1.dp,
                color = Color(0x80334155),            // border-slate-700/50
                shape = RoundedCornerShape(12.dp),
            )
            .padding(4.dp),                           // p-1
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // puzzle-piece — Блоки (активна кнопка — завжди бо ми в блоках)
        NavBtn(
            icon     = Icons.Default.Extension,
            active   = true,
            onClick  = { },
        )

        // play/stop
        NavBtn(
            icon   = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            active = false,
            tint   = if (isRunning) Color(0xFFEF4444) else Color(0xFF22C55E),
            onClick = if (isRunning) onStop else onRun,
        )

        // Роздільник — w-px h-6 bg-slate-700 mx-1
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(Color(0xFF334155)),
        )

        // book — Лог (text-yellow-500/80)
        NavBtn(
            icon    = Icons.Default.Article,
            active  = false,
            tint    = Color(0xCCEAB308),              // text-yellow-500/80
            onClick = onLog,
        )

        // trash — Очистити
        NavBtn(
            icon    = Icons.Default.DeleteSweep,
            active  = false,
            tint    = Color(0xFF64748B),
            onClick = onClear,
        )
    }
}

// ── Nav Button ────────────────────────────────────────────────────────
// .nav-btn { color:#64748b; padding:8px 12px; border-radius:8px }
// .nav-btn.active { color:#3b82f6; background:rgba(59,130,246,0.1) }
@Composable
private fun NavBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    tint: Color = if (active) Color(0xFF3B82F6) else Color(0xFF64748B),
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) Color(0x1A3B82F6)         // rgba(59,130,246,0.1)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),  // padding: 8px 12px
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ───────────────────────────────────────────────────────────────────────
// SENSOR DASHBOARD
// .sensor-dashboard {
//   position: absolute; top: 75px; left: 50%;
//   transform: translateX(-50%);
//   background: rgba(15,23,42,0.95); border-radius: 12px;
//   padding: 4px 12px; display: flex; gap: 12px;
//   box-shadow: 0 4px 15px rgba(0,0,0,0.5);
// }
// .sensor-label { font-size:8px; color:#94a3b8; font-weight:bold; uppercase }
// .sensor-value { font-family:'Courier New'; font-size:14px; color:#34d399 }
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun SensorDashboard(
    p1: Int, p2: Int, p3: Int, p4: Int,
    modifier: Modifier = Modifier,
) {
    // background: rgba(15,23,42,0.95) — без скла, просто темний фон
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xF20F172A))    // rgba(15,23,42,0.95) без blur
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            SensorItem(label = "Port 1", value = p1)

            // Роздільник — w-px bg-slate-700
            Box(Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))

            SensorItem(label = "Port 2", value = p2)
            Box(Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))

            SensorItem(label = "Port 3", value = p3)
            Box(Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))

            SensorItem(label = "Port 4", value = p4)
        }
    }
}

// ── Один сенсор ───────────────────────────────────────────────────────
@Composable
private fun SensorItem(label: String, value: Int) {
    val active = value > 30
    val valColor by animateColorAsState(
        targetValue   = if (active) Color(0xFF34D399) else Color(0xFF475569),
        animationSpec = tween(180),
        label         = "sv_color",
    )

    // .sensor-item { display:flex; flex-direction:column; align-items:center }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // font-size:8px; color:#94a3b8; font-weight:bold; text-transform:uppercase
        Text(
            text          = label,
            fontSize      = 8.sp,
            fontWeight    = FontWeight.Bold,
            color         = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp,
        )
        // font-family:'Courier New'; font-size:14px; color:#34d399
        Text(
            text       = value.toString(),
            fontSize   = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color      = valColor,
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// RUN FAB
// .run-fab {
//   position:absolute; bottom:180px; right:20px;
//   width:80px; height:80px; border-radius:50%;
//   border: 4px solid rgba(255,255,255,0.1);
//   background: #22c55e;
//   box-shadow: 0 10px 30px rgba(0,0,0,0.5);
//   transition: all 0.3s cubic-bezier(0.175,0.885,0.32,1.275);
// }
// .run-fab.running { background:#ef4444; animation:pulse-red 2s infinite }
// @keyframes pulse-red {
//   0%   { box-shadow: 0 0 0 0   rgba(239,68,68,0.7) }
//   70%  { box-shadow: 0 0 0 20px rgba(239,68,68,0)   }
//   100% { box-shadow: 0 0 0 0   rgba(239,68,68,0)   }
// }
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun RunFab(
    isRunning: Boolean,
    onRun: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteAnim = rememberInfiniteTransition(label = "fab")

    // pulse-red: 0→70%→100%  box-shadow radius 0→20px→0
    val pulseRadius by infiniteAnim.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = keyframes {
                durationMillis = 2000
                0f  at 0    with LinearEasing
                1f  at 1400 with LinearEasing
                0f  at 2000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_r",
    )
    val pulseAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.7f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = keyframes {
                durationMillis = 2000
                0.7f at 0    with LinearEasing
                0f   at 1400
                0f   at 2000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_a",
    )

    val bgColor by animateColorAsState(
        targetValue   = if (isRunning) Color(0xFFEF4444) else Color(0xFF22C55E),
        animationSpec = tween(300),
        label         = "fab_bg",
    )

    Box(
        modifier         = modifier.size(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        // pulse-red ring — box-shadow 0→20px
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size((80 + pulseRadius * 40).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = pulseAlpha * 0.4f))
            )
        }

        // Сама кнопка — width:80px; height:80px; border-radius:50%
        // border: 4px solid rgba(255,255,255,0.1)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(width = 4.dp, color = Color(0x1AFFFFFF), shape = CircleShape)
                .clickable(onClick = if (isRunning) onStop else onRun),
            contentAlignment = Alignment.Center,
        ) {
            // font-size:32px color:white text-shadow:0 2px 4px rgba(0,0,0,0.3)
            Icon(
                imageVector        = if (isRunning) Icons.Default.Stop
                                     else Icons.Default.PlayArrow,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(34.dp),
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// RUNNING BADGE — мигаючий індикатор
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun RunningBadge() {
    val anim = rememberInfiniteTransition(label = "badge")
    val alpha by anim.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "badge_alpha",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF16A34A).copy(alpha))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
        Text(
            "ВИКОНУЄТЬСЯ",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = Color.White,
            letterSpacing = 1.5.sp,
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// EMPTY WORKSPACE HINT
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyWorkspaceHint(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("⬆", fontSize = 32.sp, color = Color(0xFF1E293B))
        Text(
            "Вибери категорію знизу",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF334155),
        )
        Text(
            "і натисни або перетягни блок",
            fontSize = 12.sp,
            color    = Color(0xFF1E293B),
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            null,
            tint     = Color(0xFF1E293B),
            modifier = Modifier.size(24.dp),
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// LOG PANEL
// .log-line { font-family:'Courier New'; font-size:11px; padding:2px 0;
//   border-bottom: 1px solid rgba(255,255,255,0.05) }
// .log-tx { color:#60a5fa }
// .log-rx { color:#34d399 }
// .log-err { color:#f87171 }
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun LogPanel(
    logs: List<String>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
        color    = Color(0xFF0A0E1A),
    ) {
        Column {
            // Заголовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // text-[10px] font-bold uppercase tracking-widest
                Text(
                    "ЛОГ",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color(0xFFEAB308),  // text-yellow-500
                    letterSpacing = 2.sp,
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", fontSize = 14.sp, color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold)
                }
            }

            // border-bottom: 1px solid rgba(255,255,255,0.05)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0DFFFFFF)))

            LazyColumn(
                state          = listState,
                modifier       = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) {
                itemsIndexed(logs) { _, log ->
                    // .log-line { padding:2px 0; border-bottom:1px solid rgba(255,255,255,0.05) }
                    Text(
                        text       = log,
                        fontSize   = 11.sp,               // font-size:11px
                        fontFamily = FontFamily.Monospace, // font-family:'Courier New'
                        color      = when {
                            log.contains("❌") || log.contains("Err") -> Color(0xFFF87171) // .log-err
                            log.contains("▶")  || log.contains("TX")  -> Color(0xFF60A5FA) // .log-tx
                            log.contains("✓")  || log.contains("RX")  -> Color(0xFF34D399) // .log-rx
                            else                                       -> Color(0xFF94A3B8)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .drawBehind {
                                drawLine(
                                    Color(0x0DFFFFFF),
                                    Offset(0f, size.height),
                                    Offset(size.width, size.height),
                                    1f,
                                )
                            },
                    )
                }
            }
        }
    }
}
