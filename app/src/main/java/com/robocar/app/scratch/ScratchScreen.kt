package com.robocar.app.scratch

// ════════════════════════════════════════════════════════════════════════════
// SCRATCH SCREEN  v4  —  CLEANUP + REDESIGN
//
// ПРИБРАНО (за вимогою, червоне на скріншоті):
//  ✗  Вторинний Header-бар всередині Scratch (OFFLINE + кнопки + BT)
//  ✗  Sensor Dashboard (Port 1..4)
//  ✗  EmptyWorkspaceHint ("Вибери категорію знизу...")
//  ✗  Великий зелений FAB Play-кнопка
//
// ЗАЛИШЕНО / ДОДАНО:
//  ✔  WorkspaceCanvas — основне полотно
//  ✔  Toolbox знизу (нові категорії-кружки)
//  ✔  Маленька floating pill-кнопка Run/Stop (вгорі зліва, над toolbox)
//  ✔  TrashZone (з'являється тільки коли drag)
//  ✔  ZoomControls (справа по центру)
//  ✔  Variables overlay (під час виконання)
//  ✔  Block Context Menu
//  ✔  Param Editor Dialog
//  ✔  Log Panel (знизу)
//  ✔  SaveLoad Panel (overlay)
//  ✔  Examples Panel (overlay)
//  ✔  Running Badge (маленький, вгорі по центру)
//
// ════════════════════════════════════════════════════════════════════════════

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robocar.app.MainViewModel
import com.robocar.app.ble.BleState

// ─────────────────────────────────────────────────────────────────────────
// ГОЛОВНИЙ ЕКРАН
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun ScratchScreen(mainViewModel: MainViewModel, modifier: Modifier = Modifier) {
    // AndroidViewModel — Compose надає Application context автоматично
    val vm: WorkspaceViewModel = viewModel()

    val state      by vm.state.collectAsState()
    val dragState  by vm.dragState.collectAsState()
    val editingBlk by vm.editingBlock.collectAsState()
    val bleState   by mainViewModel.bleState.collectAsState()
    val canUndo    by vm.canUndo.collectAsState()
    val canRedo    by vm.canRedo.collectAsState()
    val varsSnap   by vm.variablesSnapshot.collectAsState()
    val saveSlots  by vm.saveSlots.collectAsState(initial = emptyList())
    val isConnected = bleState is BleState.Connected

    var showLog      by remember { mutableStateOf(false) }
    var contextId    by remember { mutableStateOf<String?>(null) }
    var ctxX         by remember { mutableStateOf(0f) }
    var ctxY         by remember { mutableStateOf(0f) }
    var showSaveLoad by remember { mutableStateOf(false) }
    var showExamples by remember { mutableStateOf(false) }

    // ─────────────────────────────────────────────────────
    // Кількість блоків у toolbox (для анімації трешу)
    // ─────────────────────────────────────────────────────
    val isDragging = dragState !is DragState.Idle

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
    ) {
        // ════════════════════════════════════════════════
        // 1. WORKSPACE CANVAS
        // ════════════════════════════════════════════════
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
            onWorkspaceTap   = {
                vm.selectBlock(null)
                contextId = null
            },
            onDragUpdate     = { x, y -> vm.updateDrag(x, y) },
            onDragEnd        = { x, y, h -> vm.endDrag(x, y, h) },
            modifier         = Modifier
                .fillMaxSize()
                // top padding: місце для floating controls (~52dp)
                // bottom padding: toolbox (~60dp) + ще трохи
                .padding(top = 52.dp, bottom = 68.dp),
        )

        // ════════════════════════════════════════════════
        // 2. FLOATING MINI-CONTROLS  (вгорі ліворуч)
        //    Run/Stop + Undo/Redo + SaveLoad + Clear
        //    — компактна таблетка, не займає багато місця
        // ════════════════════════════════════════════════
        ScratchMiniControls(
            isRunning  = state.isRunning,
            canUndo    = canUndo,
            canRedo    = canRedo,
            onRun      = { vm.runProgram(mainViewModel) },
            onStop     = { vm.stopProgram() },
            onUndo     = { vm.undo() },
            onRedo     = { vm.redo() },
            onLog      = { showLog = !showLog },
            onSaveLoad = { showSaveLoad = true },
            onExamples = { showExamples = true },
            onClear    = { vm.clearAll() },
            modifier   = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 8.dp),
        )

        // ════════════════════════════════════════════════
        // 3. RUNNING BADGE — маленький, по центру зверху
        // ════════════════════════════════════════════════
        AnimatedVisibility(
            visible  = state.isRunning,
            enter    = fadeIn() + scaleIn(),
            exit     = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
        ) {
            RunningBadge()
        }

        // ════════════════════════════════════════════════
        // 4. ZOOM CONTROLS (справа по центру)
        // ════════════════════════════════════════════════
        ZoomControls(
            onZoomIn     = { vm.zoom(1.2f, 200f, 400f) },
            onZoomOut    = { vm.zoom(0.8f, 200f, 400f) },
            onReset      = { vm.resetView() },
            currentScale = state.scale,
            modifier     = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
        )

        // ════════════════════════════════════════════════
        // 5. VARIABLES OVERLAY (під час виконання)
        // ════════════════════════════════════════════════
        if (state.isRunning && varsSnap.isNotEmpty()) {
            VariablesPanel(
                vars     = varsSnap,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 52.dp),
            )
        }

        // ════════════════════════════════════════════════
        // 6. TOOLBOX (знизу) — з повними drag callbacks
        // ════════════════════════════════════════════════
        var screenH by remember { mutableStateOf(1000f) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    // Запам'ятовуємо висоту екрана для trash detection
                    screenH = coords.positionInRoot().y +
                        coords.size.height.toFloat()
                },
        ) {
            ScratchToolbox(
                onDragBlockStart = { type, sx, sy ->
                    vm.startDragFromToolbar(type, sx, sy)
                },
                onDragUpdate = { x, y ->
                    vm.updateDrag(x, y)
                },
                onDragEnd = { x, y, h ->
                    vm.endDrag(x, y, h)
                },
                screenHeight = screenH,
            )
        }

        // ════════════════════════════════════════════════
        // 7. TRASH ZONE (з'являється при drag)
        // ════════════════════════════════════════════════
        TrashZone(
            isActive  = isDragging,
            highlight = state.trashHighlighted,
            modifier  = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 72.dp),
        )

        // ════════════════════════════════════════════════
        // 8. BLOCK CONTEXT MENU
        // ════════════════════════════════════════════════
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

        // ════════════════════════════════════════════════
        // 9. PARAM EDITOR DIALOG
        // ════════════════════════════════════════════════
        editingBlk?.let { block ->
            BlockParamEditorDialog(
                block         = block,
                onDismiss     = { vm.closeEdit() },
                onUpdateParam = { idx, v -> vm.updateParam(block.id, idx, v) },
                onDelete      = { vm.deleteBlock(block.id); vm.closeEdit() },
                onDuplicate   = { vm.duplicateBlock(block.id) },
            )
        }

        // ════════════════════════════════════════════════
        // 10. LOG PANEL (знизу, виїжджає вгору)
        // ════════════════════════════════════════════════
        AnimatedVisibility(
            visible  = showLog,
            enter    = slideInVertically { it },
            exit     = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            LogPanel(logs = state.logs, onClose = { showLog = false })
        }

        // ════════════════════════════════════════════════
        // 11. SAVE/LOAD OVERLAY
        // ════════════════════════════════════════════════
        if (showSaveLoad) {
            SaveLoadPanel(
                slots     = saveSlots,
                onSave    = { idx, name -> vm.saveSlot(idx, name) },
                onLoad    = { idx -> vm.loadSlot(idx) },
                onDelete  = { idx -> vm.clearSlot(idx) },
                onExport  = { /* TODO: share intent */ },
                onDismiss = { showSaveLoad = false },
            )
        }

        // ════════════════════════════════════════════════
        // 12. EXAMPLES OVERLAY
        // ════════════════════════════════════════════════
        if (showExamples) {
            ExamplesPanel(
                onLoad    = { name -> vm.loadExample(name) },
                onDismiss = { showExamples = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// SCRATCH MINI CONTROLS
// Компактна floating pill — Run/Stop | Undo/Redo | Save | Examples | Log | Clear
//
// Дизайн: темний #1E293B фон, border-radius pill
// Розмір: невеликий, не перекриває canvas
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ScratchMiniControls(
    isRunning: Boolean, canUndo: Boolean, canRedo: Boolean,
    onRun: () -> Unit, onStop: () -> Unit,
    onUndo: () -> Unit, onRedo: () -> Unit,
    onLog: () -> Unit, onSaveLoad: () -> Unit,
    onExamples: () -> Unit, onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xDD1E293B))
            .border(
                width = 1.dp,
                color = Color(0x30FFFFFF),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Run / Stop ──────────────────────────────────
        val runBg by animateColorAsState(
            if (isRunning) Color(0xFFEF4444) else Color(0xFF22C55E),
            tween(250), label = "run_bg",
        )
        MiniBtn(
            icon = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            tint = Color.White,
            bg   = runBg,
            onClick = if (isRunning) onStop else onRun,
        )

        VSep()

        // ── Undo ──────────────────────────────────────
        MiniBtn(
            icon    = Icons.Default.Undo,
            tint    = if (canUndo) Color(0xFF94A3B8) else Color(0xFF334155),
            enabled = canUndo,
            onClick = onUndo,
        )
        // ── Redo ──────────────────────────────────────
        MiniBtn(
            icon    = Icons.Default.Redo,
            tint    = if (canRedo) Color(0xFF94A3B8) else Color(0xFF334155),
            enabled = canRedo,
            onClick = onRedo,
        )

        VSep()

        // ── Save/Load ─────────────────────────────────
        MiniBtn(Icons.Default.FolderOpen, Color(0xFF60A5FA), onClick = onSaveLoad)
        // ── Examples ──────────────────────────────────
        MiniBtn(Icons.Default.AutoAwesome, Color(0xFFEAB308), onClick = onExamples)
        // ── Log ───────────────────────────────────────
        MiniBtn(Icons.Default.Article, Color(0xFF64748B), onClick = onLog)
        // ── Clear ─────────────────────────────────────
        MiniBtn(Icons.Default.DeleteSweep, Color(0xFF64748B)) { showClearConfirm = true }
    }

    // Confirm clear dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor   = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor  = Color(0xFF94A3B8),
            title            = { Text("Очистити?") },
            text             = { Text("Видалити всі блоки з полотна?") },
            confirmButton    = {
                TextButton(onClick = { onClear(); showClearConfirm = false }) {
                    Text("Так", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Скасувати", color = Color(0xFF64748B))
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// MINI BTN — кнопка в mini controls
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun MiniBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color(0xFF94A3B8),
    bg: Color = Color.Transparent,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector         = icon,
            contentDescription  = null,
            tint                = tint.copy(alpha = if (enabled) 1f else 0.3f),
            modifier            = Modifier.size(17.dp),
        )
    }
}

// Вертикальний роздільник
@Composable
private fun VSep() {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(20.dp)
            .background(Color(0xFF334155)),
    )
}

// ─────────────────────────────────────────────────────────────────────────
// TRASH ZONE
// З'являється тільки коли є активний drag
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun TrashZone(
    isActive: Boolean,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible  = isActive,
        enter    = fadeIn(tween(200)) + scaleIn(tween(200)),
        exit     = fadeOut(tween(300)) + scaleOut(tween(300)),
        modifier = modifier,
    ) {
        val bg by animateColorAsState(
            if (highlight) Color(0xFFEF4444) else Color(0xFF1E293B),
            tween(180), label = "trash_bg",
        )
        val borderColor by animateColorAsState(
            if (highlight) Color(0xFFFF6B6B) else Color(0xFF334155),
            tween(180), label = "trash_border",
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bg)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.Delete,
                contentDescription = null,
                tint               = if (highlight) Color.White else Color(0xFF475569),
                modifier           = Modifier.size(24.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// RUNNING BADGE
// Маленький індикатор "виконується" по центру зверху
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun RunningBadge() {
    val anim = rememberInfiniteTransition(label = "rb")
    val alpha by anim.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "rba",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF16A34A).copy(alpha = alpha))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        Text(
            text          = "ВИКОНУЄТЬСЯ",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = Color.White,
            letterSpacing = 1.5.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// LOG PANEL
// Виїжджає знизу при натисканні на кнопку Log у mini controls
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun LogPanel(
    logs: List<String>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Auto-scroll to bottom при новому логу
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
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
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text          = "ЛОГ",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = Color(0xFFEAB308),
                        letterSpacing = 2.sp,
                    )
                    // Лічильник
                    if (logs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text     = "${logs.size}",
                                fontSize = 9.sp,
                                color    = Color(0xFF475569),
                            )
                        }
                    }
                }
                // Кнопка закрити
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "×",
                        fontSize   = 14.sp,
                        color      = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Лінія-роздільник
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x0DFFFFFF)),
            )

            // Список логів
            LazyColumn(
                state           = listState,
                modifier        = Modifier.fillMaxWidth(),
                contentPadding  = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                itemsIndexed(logs) { _, log ->
                    val textColor = when {
                        log.contains("❌") || log.contains("Err")  -> Color(0xFFF87171)
                        log.contains("▶")  || log.contains("TX")  -> Color(0xFF60A5FA)
                        log.contains("✓")  || log.contains("RX")  -> Color(0xFF34D399)
                        log.contains("⚠")                          -> Color(0xFFFBBF24)
                        else                                        -> Color(0xFF94A3B8)
                    }
                    Text(
                        text       = log,
                        fontSize   = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color      = textColor,
                        modifier   = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

