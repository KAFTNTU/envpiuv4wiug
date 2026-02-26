package com.robocar.app.scratch

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.robocar.app.model.BlockType

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
    val execAnim  = rememberInfiniteTransition(label = "exec")
    val execPulse by execAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ep")
    val snapAnim  = rememberInfiniteTransition(label = "snap")
    val snapPulse by snapAnim.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "sp")

    var canvasH by remember { mutableStateOf(0f) }
    val isDragging = dragState !is DragState.Idle

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasH = it.height.toFloat() }

            // ── Drag tracking (when block/toolbar drag active) ──────────
            .pointerInput(isDragging) {
                if (isDragging) {
                    awaitEachGesture {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull() ?: break
                            ch.consume()
                            val pos = ch.position
                            onDragUpdate(pos.x, pos.y)
                            if (!ch.pressed) {
                                onDragEnd(pos.x, pos.y, canvasH)
                                break
                            }
                        }
                    }
                }
            }

            // ── Tap / pan / drag-start (idle) ────────────────────────────
            .pointerInput(state.blocks, state.scale, state.panX, state.panY, isDragging) {
                if (!isDragging) {
                    awaitEachGesture {
                        val down  = awaitFirstDown(requireUnconsumed = false)
                        val wsX   = (down.position.x - state.panX) / state.scale
                        val wsY   = (down.position.y - state.panY) / state.scale
                        val hitId = hitTestBlock(wsX, wsY, state.blocks)

                        var dragged   = false
                        var prevPos   = down.position
                        val startTime = System.currentTimeMillis()

                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull() ?: break

                            if (!ch.pressed) {
                                if (!dragged) {
                                    if (hitId != null) onBlockTap(hitId)
                                    else onWorkspaceTap()
                                }
                                break
                            }

                            val absD = ch.position - down.position

                            // Long press 500ms без руху
                            if (!dragged &&
                                System.currentTimeMillis() - startTime > 500 &&
                                kotlin.math.abs(absD.x) < 12f && kotlin.math.abs(absD.y) < 12f) {
                                if (hitId != null) onBlockLongPress(hitId, ch.position.x, ch.position.y)
                                break
                            }

                            // Drag threshold 8px
                            if (!dragged && (kotlin.math.abs(absD.x) > 8f || kotlin.math.abs(absD.y) > 8f)) {
                                dragged = true
                                if (hitId != null) {
                                    onBlockDragStart(hitId, ch.position.x, ch.position.y)
                                }
                            }

                            // Pan workspace (без блоку)
                            if (dragged && hitId == null) {
                                val delta = ch.position - prevPos
                                prevPos   = ch.position
                                onPan(delta.x, delta.y)
                            }

                            ch.consume()
                        }
                    }
                }
            }

            // ── Pinch zoom ───────────────────────────────────────────────
            .pointerInput(Unit) {
                detectTransformGestures { centroid: Offset, _: Offset, zoom: Float, _: Float ->
                    if (zoom != 1f) onZoom(zoom, centroid.x, centroid.y)
                }
            }
    ) {
        canvasH = size.height

        drawRect(Color(0xFF0F172A))
        drawWorkspaceGrid(state.panX, state.panY, state.scale, size.width, size.height)

        withTransform({
            translate(state.panX, state.panY)
            scale(state.scale, state.scale, Offset.Zero)
        }) {
            val all    = state.blocks
            val roots  = rootBlocks(all)
            val selId  = state.selectedId
            val execId = state.executingId
            val snapId = state.snapHighlightId

            for (root in roots)
                drawChain(root.id, all, textMeasurer, selId, execId, snapId, execPulse, state.scale, false)

            selId?.let { all[it] }?.let { b ->
                drawWsBlock(b, all, textMeasurer, true, false, false, state.scale)
            }

            if (snapId != null && isDragging) {
                all[snapId]?.let { sb ->
                    drawSnapIndicator(
                        sb.x + BlockDimensions.NOTCH_X + 16f,
                        sb.y + BlockDimensions.HEIGHT,
                        Color(0xFF00FFEE).copy(snapPulse),
                    )
                }
            }
        }

        when (val ds = dragState) {
            is DragState.FromToolbar   -> drawDragGhost(ds.type, ds.screenX, ds.screenY, textMeasurer)
            is DragState.FromWorkspace ->
                state.blocks[ds.blockId]?.type?.let { drawDragGhost(it, ds.screenX, ds.screenY, textMeasurer) }
            else -> {}
        }
    }
}

private fun DrawScope.drawChain(
    startId: String?, all: Map<String, WsBlock>, tm: TextMeasurer,
    selId: String?, execId: String?, snapId: String?,
    execPulse: Float, scale: Float, drawSel: Boolean,
) {
    var id = startId
    while (id != null) {
        val b = all[id] ?: break
        if (b.id != selId || drawSel) {
            drawWsBlock(b, all, tm, b.id == selId, b.id == execId, b.id == snapId, scale)
            if (b.id == execId) drawExecutionGlow(b, all, execPulse)
            b.subChainId?.let  { drawChain(it, all, tm, selId, execId, snapId, execPulse, scale, drawSel) }
            b.sub2ChainId?.let { drawChain(it, all, tm, selId, execId, snapId, execPulse, scale, drawSel) }
        }
        id = b.nextId
    }
}

private fun hitTestBlock(wsX: Float, wsY: Float, blocks: Map<String, WsBlock>): String? {
    for (b in blocks.values.sortedByDescending { it.y }) {
        val totalH = totalBlockH(b, blocks)
        if (wsX >= b.x && wsX <= b.x + BlockDimensions.WIDTH &&
            wsY >= b.y && wsY <= b.y + totalH) return b.id
    }
    return null
}
