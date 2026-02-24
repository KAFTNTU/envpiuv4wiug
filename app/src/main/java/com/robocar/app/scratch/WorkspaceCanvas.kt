package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// WORKSPACE CANVAS — точна копія #blocklyDiv + Blockly zelos renderer
//
// Оригінал:
//   #blocklyDiv { top:64px; left:0; right:0; bottom:64px;
//     background-color: #0f172a }
//   grid: { spacing:50, length:3, colour:'#475569', snap:true }
//   zoom: { startScale:0.65, maxScale:10, minScale:0.1, pinch:true }
//   renderer: 'zelos'
// ═══════════════════════════════════════════════════════════════════════

import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.robocar.app.model.BlockType
import kotlinx.coroutines.delay

@Composable
fun WorkspaceCanvas(
    state: WorkspaceState,
    dragState: DragState,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float, Float, Float) -> Unit,
    onBlockTap: (String) -> Unit,
    onBlockLongPress: (String, Float, Float) -> Unit,
    onBlockDragStart: (String, Float, Float) -> Unit,
    onWorkspaceTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    // Execution pulse animation
    val execPulseAnim = rememberInfiniteTransition(label = "exec")
    val execPulse by execPulseAnim.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "ep",
    )

    // Snap pulse
    val snapAnim = rememberInfiniteTransition(label = "snap")
    val snapPulse by snapAnim.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label         = "sp",
    )

    // Tap + long press + drag gesture handling
    var lastTapId by remember { mutableStateOf<String?>(null) }
    var lastTapTime by remember { mutableStateOf(0L) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .detectWorkspaceGestures(
                state            = state,
                dragState        = dragState,
                onPan            = onPan,
                onZoom           = onZoom,
                onBlockTap       = { id ->
                    val now = System.currentTimeMillis()
                    if (id == lastTapId && now - lastTapTime < 400) {
                        // Double tap → edit
                        onBlockTap(id)
                        lastTapId   = null
                        lastTapTime = 0
                    } else {
                        onBlockTap(id)
                        lastTapId   = id
                        lastTapTime = now
                    }
                },
                onBlockLongPress = onBlockLongPress,
                onBlockDragStart = onBlockDragStart,
                onWorkspaceTap   = onWorkspaceTap,
            )
    ) {
        val canvasW = size.width
        val canvasH = size.height

        // ── 1. Фон #0f172a ─────────────────────────────────
        drawRect(Color(0xFF0F172A))

        // ── 2. Grid — spacing:50, colour:'#475569' ─────────
        drawWorkspaceGrid(state.panX, state.panY, state.scale, canvasW, canvasH)

        // ── 3. Трансформація workspace ──────────────────────
        withTransform({
            translate(state.panX, state.panY)
            scale(state.scale, state.scale, Offset.Zero)
        }) {

            // Визначаємо порядок рендеру (виділений зверху)
            val allBlocks  = state.blocks
            val roots      = rootBlocks(allBlocks)
            val selectedId = state.selectedId
            val execId     = state.executingId
            val snapId     = state.snapHighlightId

            // Рендер всіх ланцюгів
            for (root in roots) {
                drawBlockChain(
                    startId         = root.id,
                    allBlocks       = allBlocks,
                    textMeasurer    = textMeasurer,
                    selectedId      = selectedId,
                    executingId     = execId,
                    snapHighlightId = snapId,
                    execPulse       = execPulse,
                    scale           = state.scale,
                    drawSelected    = false,
                )
            }

            // Виділений блок малюємо останнім (зверху)
            if (selectedId != null) {
                val selBlock = allBlocks[selectedId]
                if (selBlock != null) {
                    drawWsBlock(
                        block           = selBlock,
                        allBlocks       = allBlocks,
                        textMeasurer    = textMeasurer,
                        isSelected      = true,
                        isExecuting     = false,
                        isSnapHighlight = false,
                        scale           = state.scale,
                    )
                }
            }

            // Snap indicator
            if (snapId != null && dragState !is DragState.Idle) {
                val snapBlock = allBlocks[snapId]
                if (snapBlock != null) {
                    val snapColor = Color(0xFF00FFEE).copy(snapPulse)
                    drawSnapIndicator(snapBlock.x + BlockDimensions.NOTCH_X + 16f,
                        snapBlock.y + BlockDimensions.HEIGHT, snapColor)
                }
            }
        }

        // ── 4. Drag ghost — screen coordinates ─────────────
        val ds = dragState
        if (ds is DragState.FromToolbar) {
            drawDragGhost(ds.type, ds.screenX, ds.screenY, textMeasurer)
        } else if (ds is DragState.FromWorkspace) {
            val type = state.blocks[ds.blockId]?.type
            if (type != null) {
                drawDragGhost(type, ds.screenX, ds.screenY, textMeasurer)
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// DRAW BLOCK CHAIN — рекурсивний рендер ланцюга
// ───────────────────────────────────────────────────────────────────────
private fun DrawScope.drawBlockChain(
    startId: String?,
    allBlocks: Map<String, WsBlock>,
    textMeasurer: TextMeasurer,
    selectedId: String?,
    executingId: String?,
    snapHighlightId: String?,
    execPulse: Float,
    scale: Float,
    drawSelected: Boolean,
) {
    var id: String? = startId
    while (id != null) {
        val block = allBlocks[id] ?: break
        val isSel  = block.id == selectedId

        if (!isSel || drawSelected) {
            val isExec = block.id == executingId
            val isSnap = block.id == snapHighlightId

            drawWsBlock(
                block           = block,
                allBlocks       = allBlocks,
                textMeasurer    = textMeasurer,
                isSelected      = isSel,
                isExecuting     = isExec,
                isSnapHighlight = isSnap,
                scale           = scale,
            )

            if (isExec) {
                drawExecutionGlow(block, allBlocks, execPulse)
            }

            // Дочірні ланцюги (C-блоки)
            if (block.subChainId != null) {
                drawBlockChain(block.subChainId, allBlocks, textMeasurer,
                    selectedId, executingId, snapHighlightId, execPulse, scale, drawSelected)
            }
            if (block.sub2ChainId != null) {
                drawBlockChain(block.sub2ChainId, allBlocks, textMeasurer,
                    selectedId, executingId, snapHighlightId, execPulse, scale, drawSelected)
            }
        }

        id = block.nextId
    }
}

// ───────────────────────────────────────────────────────────────────────
// GESTURE DETECTION — pan, zoom, tap, long press, drag
// ───────────────────────────────────────────────────────────────────────
private fun Modifier.detectWorkspaceGestures(
    state: WorkspaceState,
    dragState: DragState,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float, Float, Float) -> Unit,
    onBlockTap: (String) -> Unit,
    onBlockLongPress: (String, Float, Float) -> Unit,
    onBlockDragStart: (String, Float, Float) -> Unit,
    onWorkspaceTap: () -> Unit,
): Modifier = this
    .pointerInput(state.blocks, state.scale, state.panX, state.panY) {
        var panActive  = false
        
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()

            val wsX = (down.position.x - state.panX) / state.scale
            val wsY = (down.position.y - state.panY) / state.scale
            val hitId = hitTestBlock(wsX, wsY, state.blocks)

            // Long press timer
                        if (hitId != null && dragState is DragState.Idle) {
                // Long press handled by drag gesture, skip coroutine-based timer
            }

            var didDrag   = false
            var pointerUp = false

            while (!pointerUp) {
                val event = awaitPointerEvent()
                val ptrs  = event.changes.filter { it.pressed }

                if (ptrs.isEmpty()) {
                    pointerUp = true
                                        if (!didDrag) {
                        if (hitId != null) onBlockTap(hitId)
                        else onWorkspaceTap()
                    }
                    break
                }

                // Pinch zoom (2 пальці)
                if (ptrs.size >= 2) {
                                        val d  = ptrs[0].position - ptrs[1].position
                    val pd = ptrs[0].previousPosition - ptrs[1].previousPosition
                    val cur = kotlin.math.sqrt((d.x * d.x + d.y * d.y).toDouble()).toFloat()
                    val prv = kotlin.math.sqrt((pd.x * pd.x + pd.y * pd.y).toDouble()).toFloat()
                    if (prv > 0f) {
                        val factor = cur / prv
                        val cx = (ptrs[0].position.x + ptrs[1].position.x) / 2f
                        val cy = (ptrs[0].position.y + ptrs[1].position.y) / 2f
                        onZoom(factor, cx, cy)
                    }
                    ptrs.forEach { it.consume() }
                    continue
                }

                // Pan або drag
                val ptr  = ptrs.first()
                val drag = ptr.positionChange()

                if (!didDrag && (kotlin.math.abs(drag.x) > 8f || kotlin.math.abs(drag.y) > 8f)) {
                                        didDrag = true
                    if (hitId != null && dragState is DragState.Idle) {
                        onBlockDragStart(hitId, ptr.position.x, ptr.position.y)
                    }
                }

                if (didDrag && hitId == null) {
                    onPan(drag.x, drag.y)
                }

                ptr.consume()
            }
        }
    }

// ───────────────────────────────────────────────────────────────────────
// HIT TEST — знайти блок за workspace-координатами
// ───────────────────────────────────────────────────────────────────────
private fun hitTestBlock(
    wsX: Float,
    wsY: Float,
    blocks: Map<String, WsBlock>,
): String? {
    // Перевіряємо від верху Z-стеку (вкладені блоки зверху)
    val sorted = blocks.values.sortedByDescending { b ->
        if (b.nextId != null || b.subChainId != null) 0 else 1
    }
    for (block in sorted) {
        val bw = BlockDimensions.WIDTH
        val bh = BlockDimensions.HEIGHT +
                 if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f
        val totalH = totalBlockH(block, blocks)
        if (wsX >= block.x && wsX <= block.x + bw &&
            wsY >= block.y && wsY <= block.y + totalH
        ) {
            return block.id
        }
    }
    return null
}
