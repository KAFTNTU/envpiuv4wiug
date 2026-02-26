package com.robocar.app.scratch

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.TextMeasurer
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
    onDragUpdate: (Float, Float) -> Unit,
    onDragEnd: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    val execAnim = rememberInfiniteTransition(label = "exec")
    val execPulse by execAnim.animateFloat(
        0f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ep")

    val snapAnim = rememberInfiniteTransition(label = "snap")
    val snapPulse by snapAnim.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "sp")

    var canvasHeight by remember { mutableStateOf(0f) }

    // Active drag: intercept pointer for dragging
    val isDragging = dragState !is DragState.Idle

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasHeight = it.height.toFloat() }
            // Drag tracking when active
            .pointerInput(isDragging) {
                if (isDragging) {
                    awaitEachGesture {
                        // consume and track movement
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            change.consume()
                            onDragUpdate(change.position.x, change.position.y)
                            if (!change.pressed) {
                                onDragEnd(change.position.x, change.position.y, canvasHeight)
                                break
                            }
                        }
                    }
                }
            }
            // Tap / long press / drag start when idle
            .pointerInput(state.blocks, state.scale, state.panX, state.panY, isDragging) {
                if (!isDragging) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val wsX = (down.position.x - state.panX) / state.scale
                        val wsY = (down.position.y - state.panY) / state.scale
                        val hitId = hitTestBlock(wsX, wsY, state.blocks)

                        var dragged = false
                        var longPressed = false
                        var downPos = down.position

                        // Long press via delay
                        val startTime = System.currentTimeMillis()

                        while (true) {
                            val event = awaitPointerEvent()
                            val ch = event.changes.firstOrNull() ?: break

                            if (!ch.pressed) {
                                // Released — tap
                                if (!dragged && !longPressed) {
                                    if (hitId != null) onBlockTap(hitId)
                                    else onWorkspaceTap()
                                }
                                break
                            }

                            val delta = ch.position - down.position

                            // Check long press (400ms)
                            if (!dragged && !longPressed &&
                                System.currentTimeMillis() - startTime > 400 &&
                                kotlin.math.abs(delta.x) < 10f && kotlin.math.abs(delta.y) < 10f) {
                                longPressed = true
                                if (hitId != null) onBlockLongPress(hitId, ch.position.x, ch.position.y)
                            }

                            // Check drag start
                            if (!dragged &&
                                (kotlin.math.abs(delta.x) > 10f || kotlin.math.abs(delta.y) > 10f)) {
                                dragged = true
                                if (hitId != null) {
                                    onBlockDragStart(hitId, ch.position.x, ch.position.y)
                                } else {
                                    // pan workspace
                                }
                            }

                            if (dragged && hitId == null) {
                                // Пан: delta від попередньої позиції (не від down)
                                val drag = ch.position - downPos
                                downPos  = ch.position
                                onPan(drag.x, drag.y)
                            }
                            ch.consume()
                        }
                    }
                }
            }
            // Pinch zoom
            .pointerInput(Unit) {
                detectTransformGestures { centroid, _, zoom, _ ->
                    if (zoom != 1f) onZoom(zoom, centroid.x, centroid.y)
                }
            }
    ) {
        canvasHeight = size.height

        // 1. Background #0f172a
        drawRect(Color(0xFF0F172A))

        // 2. Grid spacing:50 colour:#475569
        drawWorkspaceGrid(state.panX, state.panY, state.scale, size.width, size.height)

        // 3. Blocks
        withTransform({
            translate(state.panX, state.panY)
            scale(state.scale, state.scale, Offset.Zero)
        }) {
            val allBlocks  = state.blocks
            val roots      = rootBlocks(allBlocks)
            val selectedId = state.selectedId
            val execId     = state.executingId
            val snapId     = state.snapHighlightId

            for (root in roots) {
                drawChain(root.id, allBlocks, textMeasurer,
                    selectedId, execId, snapId, execPulse, state.scale, false)
            }

            // Selected on top
            if (selectedId != null) {
                val b = allBlocks[selectedId]
                if (b != null) drawWsBlock(b, allBlocks, textMeasurer, true, false, false, state.scale)
            }

            // Snap indicator
            if (snapId != null && isDragging) {
                val sb = allBlocks[snapId]
                if (sb != null) {
                    val sc = Color(0xFF00FFEE).copy(snapPulse)
                    drawSnapIndicator(sb.x + BlockDimensions.NOTCH_X + 16f,
                        sb.y + BlockDimensions.HEIGHT, sc)
                }
            }
        }

        // 4. Drag ghost (screen coords)
        when (val ds = dragState) {
            is DragState.FromToolbar   -> drawDragGhost(ds.type, ds.screenX, ds.screenY, textMeasurer)
            is DragState.FromWorkspace -> {
                val t = state.blocks[ds.blockId]?.type
                if (t != null) drawDragGhost(t, ds.screenX, ds.screenY, textMeasurer)
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawChain(
    startId: String?,
    allBlocks: Map<String, WsBlock>,
    textMeasurer: TextMeasurer,
    selectedId: String?,
    executingId: String?,
    snapId: String?,
    execPulse: Float,
    scale: Float,
    drawSelected: Boolean,
) {
    var id: String? = startId
    while (id != null) {
        val b = allBlocks[id] ?: break
        val isSel = b.id == selectedId
        if (!isSel || drawSelected) {
            val isExec = b.id == executingId
            val isSnap = b.id == snapId
            drawWsBlock(b, allBlocks, textMeasurer, isSel, isExec, isSnap, scale)
            if (isExec) drawExecutionGlow(b, allBlocks, execPulse)
            if (b.subChainId != null)
                drawChain(b.subChainId, allBlocks, textMeasurer,
                    selectedId, executingId, snapId, execPulse, scale, drawSelected)
            if (b.sub2ChainId != null)
                drawChain(b.sub2ChainId, allBlocks, textMeasurer,
                    selectedId, executingId, snapId, execPulse, scale, drawSelected)
        }
        id = b.nextId
    }
}

private fun hitTestBlock(wsX: Float, wsY: Float, blocks: Map<String, WsBlock>): String? {
    for (block in blocks.values.sortedByDescending { it.y }) {
        val bh = BlockDimensions.HEIGHT + if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f
        val totalH = totalBlockH(block, blocks)
        if (wsX >= block.x && wsX <= block.x + BlockDimensions.WIDTH &&
            wsY >= block.y && wsY <= block.y + totalH) {
            return block.id
        }
    }
    return null
}
