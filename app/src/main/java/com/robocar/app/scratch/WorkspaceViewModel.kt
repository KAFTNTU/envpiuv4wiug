package com.robocar.app.scratch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robocar.app.MainViewModel
import com.robocar.app.model.BlockParam
import com.robocar.app.model.BlockType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// ─────────────────────────────────────────────────────────────
// WORKSPACE STATE — весь стан скретч-workspace
// ─────────────────────────────────────────────────────────────
data class WorkspaceState(
    val blocks: Map<String, WsBlock> = emptyMap(),  // всі блоки на canvas
    val panX: Float = 0f,                            // зсув viewport по X
    val panY: Float = 0f,                            // зсув viewport по Y
    val scale: Float = 1f,                           // масштаб (zoom)
    val selectedId: String? = null,                  // виділений блок
    val executingId: String? = null,                 // блок що зараз виконується
    val isRunning: Boolean = false,                  // програма запущена
    val snapHighlightId: String? = null,             // підсвічений блок при snap
    val trashHighlighted: Boolean = false,           // корзина підсвічена (блок над нею)
    val logs: List<String> = emptyList(),            // лог виконання
)

class WorkspaceViewModel : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()

    // Поточний стан drag
    private val _dragState = MutableStateFlow<DragState>(DragState.Idle)
    val dragState: StateFlow<DragState> = _dragState.asStateFlow()

    // Стан редагування параметрів
    private val _editingBlock = MutableStateFlow<WsBlock?>(null)
    val editingBlock: StateFlow<WsBlock?> = _editingBlock.asStateFlow()

    private var execJob: Job? = null

    // ─────────────────── PAN / ZOOM ────────────────────────
    fun pan(dx: Float, dy: Float) {
        _state.value = _state.value.copy(
            panX = _state.value.panX + dx,
            panY = _state.value.panY + dy,
        )
    }

    fun zoom(factor: Float, pivotX: Float, pivotY: Float) {
        val s = _state.value
        val newScale = (s.scale * factor).coerceIn(0.3f, 2.5f)
        // Зміщення так щоб pivot залишився на місці
        val dx = pivotX - pivotX * (newScale / s.scale)
        val dy = pivotY - pivotY * (newScale / s.scale)
        _state.value = s.copy(
            scale = newScale,
            panX  = s.panX + dx,
            panY  = s.panY + dy,
        )
    }

    fun resetView() {
        _state.value = _state.value.copy(panX = 0f, panY = 0f, scale = 1f)
    }

    // ─────────────────── DRAG FROM TOOLBAR ─────────────────
    fun startDragFromToolbar(type: BlockType, screenX: Float, screenY: Float) {
        _dragState.value = DragState.FromToolbar(type, screenX, screenY)
    }

    fun updateDrag(screenX: Float, screenY: Float) {
        val d = _dragState.value
        _dragState.value = when (d) {
            is DragState.FromToolbar -> d.copy(screenX = screenX, screenY = screenY)
            is DragState.FromWorkspace -> d.copy(screenX = screenX, screenY = screenY)
            else -> d
        }
        // Оновити snap-highlight
        updateSnapHighlight(screenX, screenY)
        // Перевірити корзину
        updateTrashHighlight(screenX, screenY)
    }

    fun startDragFromWorkspace(blockId: String, screenX: Float, screenY: Float) {
        val s = _state.value
        val block = s.blocks[blockId] ?: return
        // Відключити від ланцюгу
        detachBlock(blockId)
        val wsX = screenToWsX(screenX, s)
        val wsY = screenToWsY(screenY, s)
        _dragState.value = DragState.FromWorkspace(
            blockId = blockId,
            screenX = screenX,
            screenY = screenY,
            offsetX = wsX - block.x,
            offsetY = wsY - block.y,
        )
        _state.value = _state.value.copy(selectedId = blockId)
    }

    // Завершення drag — відпустили блок
    fun endDrag(screenX: Float, screenY: Float, screenHeight: Float) {
        val d = _dragState.value
        val s = _state.value

        // Якщо над корзиною — видаляємо
        if (isOverTrash(screenX, screenY, screenHeight)) {
            when (d) {
                is DragState.FromWorkspace -> deleteBlock(d.blockId)
                else -> {}
            }
            _dragState.value = DragState.Idle
            _state.value = _state.value.copy(trashHighlighted = false, snapHighlightId = null)
            return
        }

        // Шукаємо snap-ціль
        val snapTarget = findSnapTarget(screenX, screenY, s)

        when (d) {
            is DragState.FromToolbar -> {
                // Створюємо новий блок
                val wsX = screenToWsX(screenX, s)
                val wsY = screenToWsY(screenY, s)
                val newBlock = WsBlock(d.type, wsX, wsY)
                val blocks = s.blocks.toMutableMap()
                blocks[newBlock.id] = newBlock
                _state.value = s.copy(
                    blocks          = blocks,
                    selectedId      = newBlock.id,
                    snapHighlightId = null,
                    trashHighlighted = false,
                )
                if (snapTarget != null) {
                    performSnap(newBlock.id, snapTarget)
                }
            }
            is DragState.FromWorkspace -> {
                // Переміщуємо блок на нову позицію
                val wsX = screenToWsX(screenX, s) - d.offsetX
                val wsY = screenToWsY(screenY, s) - d.offsetY
                val blocks = _state.value.blocks.toMutableMap()
                val block = blocks[d.blockId]
                if (block != null) {
                    blocks[d.blockId] = block.copy(x = wsX, y = wsY)
                    relayoutChain(d.blockId, wsX, wsY, blocks)
                    _state.value = _state.value.copy(
                        blocks          = blocks,
                        snapHighlightId = null,
                        trashHighlighted = false,
                    )
                }
                if (snapTarget != null) {
                    performSnap(d.blockId, snapTarget)
                }
            }
            else -> {}
        }
        _dragState.value = DragState.Idle
    }

    // ─────────────────── SNAP ЛОГІКА ───────────────────────
    private fun findSnapTarget(screenX: Float, screenY: Float, s: WorkspaceState): SnapTarget? {
        val wsX = screenToWsX(screenX, s)
        val wsY = screenToWsY(screenY, s)
        val radius = BlockDimensions.SNAP_RADIUS
        var best: SnapTarget? = null
        var bestDist = Float.MAX_VALUE

        val dragId = when (val d = _dragState.value) {
            is DragState.FromWorkspace -> d.blockId
            else -> null
        }

        for (block in s.blocks.values) {
            if (block.id == dragId) continue
            val bH = blockBodyHeight(block, s.blocks)

            // Точка snap знизу (нижня сторона блоку)
            if (block.type.hasNext) {
                val tx = block.x + BlockDimensions.NOTCH_X + BlockDimensions.NOTCH_W / 2
                val ty = block.y + bH
                val dist = dist(wsX, wsY, tx, ty)
                if (dist < radius && dist < bestDist) {
                    bestDist = dist
                    best = SnapTarget(block.id, ConnectionSide.BOTTOM, tx, ty)
                }
            }
            // Точка snap у DO секцію C-блоку
            if (block.type.hasSub && block.subChainId == null) {
                val armH = BlockDimensions.HEIGHT + if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f
                val tx = block.x + BlockDimensions.C_ARM_WIDTH + BlockDimensions.NOTCH_X
                val ty = block.y + armH
                val dist = dist(wsX, wsY, tx, ty)
                if (dist < radius && dist < bestDist) {
                    bestDist = dist
                    best = SnapTarget(block.id, ConnectionSide.SUB_TOP, tx, ty)
                }
            }
            // Точка snap у ELSE секцію
            if (block.type.hasSub2 && block.sub2ChainId == null) {
                val sub1H = chainHeight(block.subChainId, s.blocks)
                val armH  = BlockDimensions.HEIGHT + if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f
                val ty = block.y + armH + maxOf(sub1H, BlockDimensions.MIN_C_INNER) + BlockDimensions.HEIGHT
                val tx = block.x + BlockDimensions.C_ARM_WIDTH + BlockDimensions.NOTCH_X
                val dist = dist(wsX, wsY, tx, ty)
                if (dist < radius && dist < bestDist) {
                    bestDist = dist
                    best = SnapTarget(block.id, ConnectionSide.SUB2_TOP, tx, ty)
                }
            }
        }
        return best
    }

    private fun performSnap(draggingId: String, target: SnapTarget) {
        val blocks = _state.value.blocks.toMutableMap()
        val target_block = blocks[target.blockId] ?: return
        val dragging = blocks[draggingId] ?: return

        when (target.side) {
            ConnectionSide.BOTTOM -> {
                // Під'єднати dragging після target
                val oldNext = target_block.nextId
                blocks[target.blockId] = target_block.copy(nextId = draggingId)
                // Якщо у dragging вже є next — прив'язати до кінця ланцюга dragging
                if (oldNext != null) {
                    val lastInChain = lastBlockInChain(draggingId, blocks)
                    if (lastInChain != null && blocks[lastInChain]?.type?.hasNext == true) {
                        blocks[lastInChain] = blocks[lastInChain]!!.copy(nextId = oldNext)
                    }
                }
            }
            ConnectionSide.SUB_TOP -> {
                blocks[target.blockId] = target_block.copy(subChainId = draggingId)
                blocks[draggingId] = dragging.copy(isSubBlock = true)
            }
            ConnectionSide.SUB2_TOP -> {
                blocks[target.blockId] = target_block.copy(sub2ChainId = draggingId)
                blocks[draggingId] = dragging.copy(isSubBlock = true)
            }
        }

        // Перерахувати позиції
        val root = findRoot(target.blockId, blocks)
        if (root != null) {
            relayoutChain(root.id, root.x, root.y, blocks)
        }
        _state.value = _state.value.copy(blocks = blocks, snapHighlightId = null)
    }

    private fun detachBlock(blockId: String) {
        val blocks = _state.value.blocks.toMutableMap()
        // Знайти parent і відключити
        for (b in blocks.values) {
            if (b.nextId == blockId) {
                blocks[b.id] = b.copy(nextId = null)
            }
            if (b.subChainId == blockId) {
                blocks[b.id] = b.copy(subChainId = null)
            }
            if (b.sub2ChainId == blockId) {
                blocks[b.id] = b.copy(sub2ChainId = null)
            }
        }
        _state.value = _state.value.copy(blocks = blocks)
    }

    private fun findRoot(blockId: String, blocks: Map<String, WsBlock>): WsBlock? {
        val hasParent = mutableSetOf<String>()
        for (b in blocks.values) {
            b.nextId?.let { hasParent.add(it) }
            b.subChainId?.let { hasParent.add(it) }
            b.sub2ChainId?.let { hasParent.add(it) }
        }
        // Йдемо по nextId від blockId вверх
        var id: String? = blockId
        while (id != null) {
            if (id !in hasParent) return blocks[id]
            id = blocks.values.find { it.nextId == id }?.id
                ?: blocks.values.find { it.subChainId == id }?.id
                ?: blocks.values.find { it.sub2ChainId == id }?.id
        }
        return blocks[blockId]
    }

    private fun lastBlockInChain(startId: String, blocks: Map<String, WsBlock>): String? {
        var id: String? = startId
        var last: String? = null
        while (id != null) {
            last = id
            id = blocks[id]?.nextId
        }
        return last
    }

    private fun updateSnapHighlight(screenX: Float, screenY: Float) {
        val s = _state.value
        val target = findSnapTarget(screenX, screenY, s)
        _state.value = s.copy(snapHighlightId = target?.blockId)
    }

    private fun updateTrashHighlight(screenX: Float, screenY: Float) {
        // Корзина в нижньому правому куті
    }

    private fun isOverTrash(screenX: Float, screenY: Float, screenHeight: Float): Boolean {
        // Корзина: нижній правий кут 80x80
        return false // буде реалізовано у ScratchScreen через координати
    }

    // ─────────────────── РЕДАГУВАННЯ ПАРАМЕТРІВ ────────────
    fun selectBlock(blockId: String?) {
        _state.value = _state.value.copy(selectedId = blockId)
    }

    fun openEdit(blockId: String) {
        _editingBlock.value = _state.value.blocks[blockId]
    }

    fun closeEdit() {
        _editingBlock.value = null
    }

    fun updateParam(blockId: String, paramIndex: Int, newValue: Any) {
        val blocks = _state.value.blocks.toMutableMap()
        val block = blocks[blockId] ?: return
        val params = block.params.toMutableList()
        if (paramIndex >= params.size) return
        val old = params[paramIndex]
        params[paramIndex] = when {
            old is BlockParam.NumberInput && newValue is Float ->
                old.copy(value = newValue.coerceIn(old.min, old.max))
            old is BlockParam.NumberInput && newValue is String ->
                old.copy(value = newValue.toFloatOrNull()?.coerceIn(old.min, old.max) ?: old.value)
            old is BlockParam.TextInput && newValue is String ->
                old.copy(value = newValue)
            old is BlockParam.DropdownInput && newValue is String ->
                old.copy(selected = newValue)
            else -> old
        }
        val updated = block.copy(params = params)
        blocks[blockId] = updated
        _state.value = _state.value.copy(blocks = blocks)
        _editingBlock.value = updated
    }

    // ─────────────────── ВИДАЛЕННЯ ─────────────────────────
    fun deleteBlock(blockId: String) {
        val blocks = _state.value.blocks.toMutableMap()
        // Відключити від parent
        for (b in blocks.values.toList()) {
            if (b.nextId == blockId)      blocks[b.id] = b.copy(nextId = null)
            if (b.subChainId == blockId)  blocks[b.id] = b.copy(subChainId = null)
            if (b.sub2ChainId == blockId) blocks[b.id] = b.copy(sub2ChainId = null)
        }
        // Видалити весь ланцюг рекурсивно
        deleteChainRecursive(blockId, blocks)
        _state.value = _state.value.copy(blocks = blocks, selectedId = null)
    }

    private fun deleteChainRecursive(id: String, blocks: MutableMap<String, WsBlock>) {
        val block = blocks.remove(id) ?: return
        block.nextId?.let { deleteChainRecursive(it, blocks) }
        block.subChainId?.let { deleteChainRecursive(it, blocks) }
        block.sub2ChainId?.let { deleteChainRecursive(it, blocks) }
    }

    fun clearAll() {
        _state.value = WorkspaceState()
        execJob?.cancel()
    }

    fun duplicateBlock(blockId: String) {
        val block = _state.value.blocks[blockId] ?: return
        val newBlock = block.copy(
            id = UUID.randomUUID().toString(),
            x  = block.x + 30f,
            y  = block.y + 30f,
            nextId = null, subChainId = null, sub2ChainId = null
        )
        val blocks = _state.value.blocks.toMutableMap()
        blocks[newBlock.id] = newBlock
        _state.value = _state.value.copy(blocks = blocks, selectedId = newBlock.id)
    }

    // ─────────────────── КООРДИНАТНІ ПЕРЕТВОРЕННЯ ──────────
    fun screenToWsX(screenX: Float, s: WorkspaceState): Float =
        (screenX - s.panX) / s.scale

    fun screenToWsY(screenY: Float, s: WorkspaceState): Float =
        (screenY - s.panY) / s.scale

    fun wsToScreenX(wsX: Float, s: WorkspaceState): Float =
        wsX * s.scale + s.panX

    fun wsToScreenY(wsY: Float, s: WorkspaceState): Float =
        wsY * s.scale + s.panY

    // ─────────────────── ВИКОНАННЯ ПРОГРАМИ ────────────────
    fun runProgram(mainViewModel: MainViewModel) {
        if (_state.value.isRunning) return
        val s = _state.value
        // Знайти START_HAT блок
        val startBlock = s.blocks.values.find {
            it.type == com.robocar.app.model.BlockType.START_HAT
        } ?: run {
            addLog("❌ Немає блоку СТАРТ")
            return
        }
        _state.value = s.copy(isRunning = true, logs = emptyList())
        execJob = viewModelScope.launch {
            try {
                WsExecutor(
                    blocks       = _state.value.blocks,
                    bleManager   = mainViewModel.bleManager,
                    sensorData   = mainViewModel.sensorData,
                    onLog        = { addLog(it) },
                    onHighlight  = { id -> _state.value = _state.value.copy(executingId = id) },
                    isRunning    = { _state.value.isRunning },
                ).executeChain(startBlock.id)
            } finally {
                _state.value = _state.value.copy(
                    isRunning   = false,
                    executingId = null
                )
                mainViewModel.bleManager.sendDrivePacket(0, 0, 0, 0)
            }
        }
    }

    fun stopProgram() {
        execJob?.cancel()
        _state.value = _state.value.copy(isRunning = false, executingId = null)
    }

    private fun addLog(msg: String) {
        val logs = _state.value.logs.takeLast(49).toMutableList()
        logs.add(msg)
        _state.value = _state.value.copy(logs = logs)
    }

    // ─────────────────── ПРИКЛАДИ ──────────────────────────
    fun loadExample(name: String) {
        clearAll()
        val blocks = mutableMapOf<String, WsBlock>()
        when (name) {
            "follow_line" -> {
                val hat   = WsBlock(com.robocar.app.model.BlockType.START_HAT, 80f, 80f)
                val loop  = WsBlock(com.robocar.app.model.BlockType.LOOP_FOREVER, 80f, 160f)
                val sense = WsBlock(com.robocar.app.model.BlockType.WAIT_UNTIL_SENSOR, 108f, 220f)
                val turn  = WsBlock(com.robocar.app.model.BlockType.ROBOT_TURN, 108f, 280f)
                blocks[hat.id]   = hat.copy(nextId = loop.id)
                blocks[loop.id]  = loop.copy(subChainId = sense.id)
                blocks[sense.id] = sense.copy(nextId = turn.id, isSubBlock = true)
                blocks[turn.id]  = turn.copy(isSubBlock = true)
            }
            "simple_drive" -> {
                val hat   = WsBlock(com.robocar.app.model.BlockType.START_HAT, 80f, 80f)
                val drive = WsBlock(com.robocar.app.model.BlockType.ROBOT_MOVE, 80f, 160f)
                val wait  = WsBlock(com.robocar.app.model.BlockType.WAIT_SECONDS, 80f, 220f)
                val stop  = WsBlock(com.robocar.app.model.BlockType.ROBOT_STOP, 80f, 280f)
                blocks[hat.id]   = hat.copy(nextId = drive.id)
                blocks[drive.id] = drive.copy(nextId = wait.id)
                blocks[wait.id]  = wait.copy(nextId = stop.id)
                blocks[stop.id]  = stop
            }
        }
        _state.value = WorkspaceState(blocks = blocks)
    }
}

// ─────────────────────────────────────────────────────────────
// УТИЛІТА ВІДСТАНІ
// ─────────────────────────────────────────────────────────────
private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
