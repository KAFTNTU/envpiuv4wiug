package com.robocar.app.ui.blocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robocar.app.ble.BleManager
import com.robocar.app.ble.SensorData
import com.robocar.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BlockViewModel : ViewModel() {

    // Програма — список блоків
    private val _program = MutableStateFlow<List<ProgramBlock>>(
        listOf(createBlock(BlockType.START_HAT))
    )
    val program: StateFlow<List<ProgramBlock>> = _program.asStateFlow()

    // Виконання
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Активний блок (підсвічення)
    private val _activeBlockId = MutableStateFlow<String?>(null)
    val activeBlockId: StateFlow<String?> = _activeBlockId.asStateFlow()

    // Редагування параметру
    private val _editingBlock = MutableStateFlow<ProgramBlock?>(null)
    val editingBlock: StateFlow<ProgramBlock?> = _editingBlock.asStateFlow()

    private var executor: BlockExecutor? = null
    private var runJob: Job? = null

    // ===== ПРОГРАМА =====

    fun addBlock(type: BlockType) {
        val newBlock = createBlock(type)
        _program.update { it + newBlock }
    }

    fun addBlockAfter(afterId: String, type: BlockType) {
        val newBlock = createBlock(type)
        _program.update { list ->
            val idx = list.indexOfFirst { it.id == afterId }
            if (idx == -1) list + newBlock
            else list.toMutableList().also { it.add(idx + 1, newBlock) }
        }
    }

    fun removeBlock(id: String) {
        _program.update { list -> list.filter { it.id != id } }
    }

    fun moveBlockUp(id: String) {
        _program.update { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx <= 0) return@update list
            list.toMutableList().also {
                val tmp = it[idx]
                it[idx] = it[idx - 1]
                it[idx - 1] = tmp
            }
        }
    }

    fun moveBlockDown(id: String) {
        _program.update { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx == -1 || idx >= list.size - 1) return@update list
            list.toMutableList().also {
                val tmp = it[idx]
                it[idx] = it[idx + 1]
                it[idx + 1] = tmp
            }
        }
    }

    fun clearProgram() {
        _program.value = listOf(createBlock(BlockType.START_HAT))
    }

    // ===== ПІДБЛОКИ (цикли/умови) =====

    fun addSubBlock(parentId: String, type: BlockType, isElse: Boolean = false) {
        val newBlock = createBlock(type)
        _program.update { list ->
            list.map { block ->
                if (block.id == parentId) {
                    if (isElse) {
                        val newSubs2 = block.subBlocks2.toMutableList().also { it.add(newBlock) }
                        block.copy(subBlocks2 = newSubs2)
                    } else {
                        val newSubs = block.subBlocks.toMutableList().also { it.add(newBlock) }
                        block.copy(subBlocks = newSubs)
                    }
                } else block
            }
        }
    }

    fun removeSubBlock(parentId: String, subId: String, isElse: Boolean = false) {
        _program.update { list ->
            list.map { block ->
                if (block.id == parentId) {
                    if (isElse) {
                        val newSubs2 = block.subBlocks2.filter { it.id != subId }.toMutableList()
                        block.copy(subBlocks2 = newSubs2)
                    } else {
                        val newSubs = block.subBlocks.filter { it.id != subId }.toMutableList()
                        block.copy(subBlocks = newSubs)
                    }
                } else block
            }
        }
    }

    // ===== ПАРАМЕТРИ =====

    fun startEditing(block: ProgramBlock) {
        _editingBlock.value = block
    }

    fun stopEditing() {
        _editingBlock.value = null
    }

    fun updateParam(blockId: String, paramIndex: Int, newValue: Any) {
        _program.update { list ->
            list.map { block ->
                if (block.id == blockId) {
                    val oldParam = block.params.getOrNull(paramIndex) ?: return@map block
                    val newParam = when {
                        oldParam is BlockParam.NumberInput && newValue is Float ->
                            oldParam.copy(value = newValue.coerceIn(oldParam.min, oldParam.max))
                        oldParam is BlockParam.DropdownInput && newValue is String ->
                            oldParam.copy(selected = newValue)
                        oldParam is BlockParam.TextInput && newValue is String ->
                            oldParam.copy(value = newValue)
                        else -> oldParam
                    }
                    val newParams = block.params.toMutableList().also { it[paramIndex] = newParam }
                    block.copy(params = newParams)
                } else block
            }
        }
        // Оновити editingBlock якщо він є
        if (_editingBlock.value?.id == blockId) {
            _editingBlock.value = _program.value.find { it.id == blockId }
        }
    }

    // ===== ВИКОНАННЯ =====

    fun runProgram(bleManager: BleManager, sensorData: StateFlow<SensorData>) {
        if (_isRunning.value) return
        executor = BlockExecutor(bleManager, sensorData)
        _isRunning.value = true
        runJob = viewModelScope.launch {
            try {
                executor?.execute(_program.value)
            } finally {
                _isRunning.value = false
                _activeBlockId.value = null
                bleManager.sendCarPacket(0, 0)
            }
        }
    }

    fun stopProgram() {
        executor?.stop()
        runJob?.cancel()
        _isRunning.value = false
        _activeBlockId.value = null
    }

    // ===== ПРИКЛАДИ =====

    fun loadExample(name: String) {
        val blocks = when (name) {
            "autopilot" -> listOf(
                createBlock(BlockType.START_HAT),
                run {
                    val sensor = createBlock(BlockType.WAIT_UNTIL_SENSOR).let { b ->
                        val p = b.params.toMutableList()
                        (p[2] as? BlockParam.NumberInput)?.let { p[2] = it.copy(value = 25f) }
                        b.copy(params = p)
                    }
                    val turn = createBlock(BlockType.ROBOT_TURN).let { b ->
                        val p = b.params.toMutableList()
                        (p[0] as? BlockParam.DropdownInput)?.let { p[0] = it.copy(selected = "RIGHT") }
                        b.copy(params = p)
                    }
                    createBlock(BlockType.LOOP_FOREVER).copy(
                        subBlocks = listOf(sensor, createBlock(BlockType.ROBOT_STOP), turn)
                    )
                }
            )
            "square" -> listOf(
                createBlock(BlockType.START_HAT),
                run {
                    val wait = createBlock(BlockType.WAIT_SECONDS).let { b ->
                        val p = b.params.toMutableList()
                        (p[0] as? BlockParam.NumberInput)?.let { p[0] = it.copy(value = 1f) }
                        b.copy(params = p)
                    }
                    val turn = createBlock(BlockType.ROBOT_TURN).let { b ->
                        val p = b.params.toMutableList()
                        (p[0] as? BlockParam.DropdownInput)?.let { p[0] = it.copy(selected = "RIGHT") }
                        (p[1] as? BlockParam.NumberInput)?.let { p[1] = it.copy(value = 0.6f) }
                        b.copy(params = p)
                    }
                    val loopParams = createBlock(BlockType.LOOP_REPEAT).params.toMutableList()
                    (loopParams[0] as? BlockParam.NumberInput)?.let { loopParams[0] = it.copy(value = 4f) }
                    createBlock(BlockType.LOOP_REPEAT).copy(
                        params = loopParams,
                        subBlocks = listOf(createBlock(BlockType.ROBOT_MOVE), wait, turn)
                    )
                },
                createBlock(BlockType.ROBOT_STOP)
            )
            "record" -> listOf(
                createBlock(BlockType.START_HAT),
                createBlock(BlockType.RECORD_START),
                run {
                    val p = createBlock(BlockType.WAIT_SECONDS).params.toMutableList()
                    (p[0] as? BlockParam.NumberInput)?.let { p[0] = it.copy(value = 5f) }
                    createBlock(BlockType.WAIT_SECONDS).copy(params = p)
                },
                createBlock(BlockType.ROBOT_STOP),
                run {
                    val p = createBlock(BlockType.REPLAY_LOOP).params.toMutableList()
                    (p[0] as? BlockParam.NumberInput)?.let { p[0] = it.copy(value = 3f) }
                    createBlock(BlockType.REPLAY_LOOP).copy(params = p)
                }
            )
            else -> listOf(createBlock(BlockType.START_HAT))
        }
        _program.value = blocks.map { it.copy(id = java.util.UUID.randomUUID().toString()) }
    }
}
