package com.robocar.app.scratch

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robocar.app.MainViewModel
import com.robocar.app.ble.BleState

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

    var showLog   by remember { mutableStateOf(false) }
    var contextId by remember { mutableStateOf<String?>(null) }
    var ctxX      by remember { mutableStateOf(0f) }
    var ctxY      by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
    ) {
        WorkspaceCanvas(
            state            = state,
            dragState        = dragState,
            onPan            = { dx, dy -> vm.pan(dx, dy) },
            onZoom           = { f, px, py -> vm.zoom(f, px, py) },
            onBlockTap       = { id ->
                if (state.selectedId == id) vm.openEdit(id)
                else vm.selectBlock(id)
            },
            onBlockLongPress = { id, sx, sy -> contextId = id; ctxX = sx; ctxY = sy },
            onBlockDragStart = { id, sx, sy -> vm.startDragFromWorkspace(id, sx, sy) },
            onWorkspaceTap   = { vm.selectBlock(null); contextId = null },
            modifier         = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 130.dp),
        )

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

        SensorDashboard(
            p1 = sensorData.p1, p2 = sensorData.p2,
            p3 = sensorData.p3, p4 = sensorData.p4,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 75.dp),
        )

        RunFab(
            isRunning = state.isRunning,
            onRun     = { vm.runProgram(mainViewModel) },
            onStop    = { vm.stopProgram() },
            modifier  = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 200.dp),
        )

        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            ScratchToolbox(onDragBlockStart = { type, sx, sy -> vm.startDragFromToolbar(type, sx, sy) })
        }

        contextId?.let { id ->
            state.blocks[id]?.let { block ->
                BlockContextMenu(
                    block = block, offsetX = ctxX, offsetY = ctxY,
                    onDismiss   = { contextId = null },
                    onEdit      = { vm.openEdit(id); contextId = null },
                    onDuplicate = { vm.duplicateBlock(id); contextId = null },
                    onDelete    = { vm.deleteBlock(id); contextId = null },
                )
            }
        }

        editingBlk?.let { block ->
            BlockParamEditorDialog(
                block         = block,
                onDismiss     = { vm.closeEdit() },
                onUpdateParam = { idx, v -> vm.updateParam(block.id, idx, v) },
                onDelete      = { vm.deleteBlock(block.id); vm.closeEdit() },
                onDuplicate   = { vm.duplicateBlock(block.id) },
            )
        }

        AnimatedVisibility(
            visible  = showLog,
            enter    = slideInVertically(initialOffsetY = { it }),
            exit     = slideOutVertically(targetOffsetY  = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            LogPanel(logs = state.logs, onClose = { showLog = false })
        }

        if (state.blocks.isEmpty() && dragState is DragState.Idle) {
            EmptyWorkspaceHint(modifier = Modifier.align(Alignment.Center))
        }

        AnimatedVisibility(
            visible  = state.isRunning,
            enter    = fadeIn() + scaleIn(),
            exit     = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 135.dp),
        ) {
            RunningBadge()
        }
    }
}

// ─────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────
@Composable
private fun ScratchHeader(
    isConnected: Boolean, isRunning: Boolean,
    onConnect: () -> Unit, onRun: () -> Unit, onStop: () -> Unit,
    onLog: () -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier,
) {
    var showClear by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth().height(64.dp)
            .background(Color(0xFF0F172A))
            .drawBehind {
                drawLine(Color(0x14FFFFFF),
                    Offset(0f, size.height), Offset(size.width, size.height), 1f)
            }
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Status dot + text
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dotColor by animateColorAsState(
                    if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444), label = "dot")
                Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Text(if (isConnected) "ONLINE" else "OFFLINE",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B), letterSpacing = 2.sp)
            }

            // Nav pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0x80334155), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavBtn(Icons.Default.Extension, true) {}
                NavBtn(
                    if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    false,
                    if (isRunning) Color(0xFFEF4444) else Color(0xFF22C55E),
                    if (isRunning) onStop else onRun,
                )
                Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))
                NavBtn(Icons.Default.Article, false, Color(0xCCEAB308), onLog)
                NavBtn(Icons.Default.DeleteSweep, false, Color(0xFF64748B)) { showClear = true }
            }

            // BT button
            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape)
                    .background(if (isConnected) Color(0xFF16A34A) else Color(0xFF2563EB))
                    .border(1.dp, Color(0x4D60A5FA), CircleShape)
                    .clickable(onClick = onConnect),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Bluetooth, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            containerColor = Color(0xFF1E293B), titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            title = { Text("Очистити?") }, text = { Text("Видалити всі блоки?") },
            confirmButton = { TextButton(onClick = { onClear(); showClear = false }) { Text("Так", color = Color(0xFFEF4444)) } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("Скасувати", color = Color(0xFF64748B)) } }
        )
    }
}

@Composable
private fun NavBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    tint: Color = if (active) Color(0xFF3B82F6) else Color(0xFF64748B),
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0x1A3B82F6) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────
// SENSOR DASHBOARD
// top:75px; background:rgba(15,23,42,0.95); border-radius:12px; padding:4px 12px
// ─────────────────────────────────────────────────────────
@Composable
private fun SensorDashboard(p1: Int, p2: Int, p3: Int, p4: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xF20F172A))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SensorItem("Port 1", p1)
            Box(Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))
            SensorItem("Port 2", p2)
            Box(Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))
            SensorItem("Port 3", p3)
            Box(Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))
            SensorItem("Port 4", p4)
        }
    }
}

@Composable
private fun SensorItem(label: String, value: Int) {
    val col by animateColorAsState(
        if (value > 30) Color(0xFF34D399) else Color(0xFF475569), label = "sv")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8), letterSpacing = 0.5.sp)
        Text(value.toString(), fontSize = 14.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = col)
    }
}

// ─────────────────────────────────────────────────────────
// RUN FAB
// width:80px height:80px border-radius:50%
// background:#22c55e → #ef4444
// border:4px solid rgba(255,255,255,0.1)
// animation: pulse-red 2s infinite
// ─────────────────────────────────────────────────────────
@Composable
private fun RunFab(isRunning: Boolean, onRun: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "fab")
    val pulseR by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "pr")
    val pulseA by inf.animateFloat(0.7f, 0f,
        infiniteRepeatable(keyframes { durationMillis = 2000; 0.7f at 0; 0f at 1400; 0f at 2000 },
            RepeatMode.Restart), label = "pa")
    val bg by animateColorAsState(
        if (isRunning) Color(0xFFEF4444) else Color(0xFF22C55E), tween(300), label = "fbg")

    Box(modifier = modifier.size(80.dp), contentAlignment = Alignment.Center) {
        if (isRunning) {
            Box(Modifier.size((80 + pulseR * 40).dp).clip(CircleShape)
                .background(Color(0xFFEF4444).copy(pulseA * 0.4f)))
        }
        Box(
            modifier = Modifier
                .size(80.dp).clip(CircleShape).background(bg)
                .border(4.dp, Color(0x1AFFFFFF), CircleShape)
                .clickable(onClick = if (isRunning) onStop else onRun),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// RUNNING BADGE
// ─────────────────────────────────────────────────────────
@Composable
private fun RunningBadge() {
    val a = rememberInfiniteTransition(label = "b")
    val alpha by a.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "ba")
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF16A34A).copy(alpha))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
        Text("ВИКОНУЄТЬСЯ", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            color = Color.White, letterSpacing = 1.5.sp)
    }
}

// ─────────────────────────────────────────────────────────
// EMPTY HINT
// ─────────────────────────────────────────────────────────
@Composable
private fun EmptyWorkspaceHint(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("⬆", fontSize = 32.sp, color = Color(0xFF1E293B))
        Text("Вибери категорію знизу", fontSize = 14.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        Text("і натисни або перетягни блок", fontSize = 12.sp, color = Color(0xFF1E293B))
    }
}

// ─────────────────────────────────────────────────────────
// LOG PANEL
// .log-tx:#60a5fa .log-rx:#34d399 .log-err:#f87171
// font-family:Courier New font-size:11px
// ─────────────────────────────────────────────────────────
@Composable
private fun LogPanel(logs: List<String>, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val ls = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) ls.animateScrollToItem(logs.lastIndex) }
    Surface(modifier = modifier.fillMaxWidth().heightIn(max = 260.dp), color = Color(0xFF0A0E1A)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ЛОГ", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEAB308), letterSpacing = 2.sp)
                Box(Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF1E293B))
                    .clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0DFFFFFF)))
            LazyColumn(state = ls, modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                itemsIndexed(logs) { _, log ->
                    Text(log, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        color = when {
                            log.contains("❌") || log.contains("Err") -> Color(0xFFF87171)
                            log.contains("▶")  || log.contains("TX")  -> Color(0xFF60A5FA)
                            log.contains("✓")  || log.contains("RX")  -> Color(0xFF34D399)
                            else -> Color(0xFF94A3B8)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                }
            }
        }
    }
}
