package com.robocar.app.scratch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

data class WorkspaceState(
    val blocks: Map<String, WsBlock> = emptyMap(),
    val panX: Float = 0f, val panY: Float = 0f, val scale: Float = 1f,
    val selectedId: String? = null, val executingId: String? = null,
    val isRunning: Boolean = false, val snapHighlightId: String? = null,
    val trashHighlighted: Boolean = false, val logs: List<String> = emptyList(),
)

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()
    private val _dragState = MutableStateFlow<DragState>(DragState.Idle)
    val dragState: StateFlow<DragState> = _dragState.asStateFlow()
    private val _editingBlock = MutableStateFlow<WsBlock?>(null)
    val editingBlock: StateFlow<WsBlock?> = _editingBlock.asStateFlow()

    // Undo/Redo
    private val undoManager = UndoManager()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    private fun pushUndo() { undoManager.push(_state.value); refreshUndoRedo() }
    private fun refreshUndoRedo() { _canUndo.value = undoManager.canUndo; _canRedo.value = undoManager.canRedo }
    fun undo() { val p = undoManager.undo(_state.value) ?: return; _state.value = p.copy(panX=_state.value.panX, panY=_state.value.panY, scale=_state.value.scale); refreshUndoRedo() }
    fun redo() { val n = undoManager.redo(_state.value) ?: return; _state.value = n.copy(panX=_state.value.panX, panY=_state.value.panY, scale=_state.value.scale); refreshUndoRedo() }

    // Save/Load
    private val saveManager = WorkspaceSaveManager(application)
    val saveSlots = saveManager.slotsFlow
    fun saveSlot(index: Int, name: String) { viewModelScope.launch { saveManager.save(index, name, _state.value) } }
    fun loadSlot(index: Int) {
        viewModelScope.launch {
            val loaded = saveManager.loadDirect(index) ?: return@launch
            pushUndo(); _state.value = loaded; undoManager.clear(); refreshUndoRedo(); addLog("📂 Завантажено зі слоту ${index+1}")
        }
    }
    fun clearSlot(index: Int) { viewModelScope.launch { saveManager.clear(index) } }
    private fun autoSave() { viewModelScope.launch { saveManager.autoSave(_state.value) } }

    // Variables
    val variables = VariablesState()
    private val _variablesSnapshot = MutableStateFlow<Map<String, Float>>(emptyMap())
    val variablesSnapshot: StateFlow<Map<String, Float>> = _variablesSnapshot.asStateFlow()

    private var execJob: Job? = null

    fun pan(dx: Float, dy: Float) { _state.value = _state.value.copy(panX = _state.value.panX + dx, panY = _state.value.panY + dy) }
    fun zoom(factor: Float, pivotX: Float, pivotY: Float) {
        val s = _state.value; val ns = (s.scale * factor).coerceIn(0.3f, 2.5f)
        _state.value = s.copy(scale=ns, panX=s.panX + pivotX - pivotX*(ns/s.scale), panY=s.panY + pivotY - pivotY*(ns/s.scale))
    }
    fun resetView() { _state.value = _state.value.copy(panX=0f, panY=0f, scale=1f) }

    fun startDragFromToolbar(type: BlockType, screenX: Float, screenY: Float) { _dragState.value = DragState.FromToolbar(type, screenX, screenY) }
    fun updateDrag(screenX: Float, screenY: Float) {
        _dragState.value = when (val d = _dragState.value) {
            is DragState.FromToolbar   -> d.copy(screenX=screenX, screenY=screenY)
            is DragState.FromWorkspace -> d.copy(screenX=screenX, screenY=screenY)
            else -> d
        }
        updateSnapHighlight(screenX, screenY)
    }
    fun startDragFromWorkspace(blockId: String, screenX: Float, screenY: Float) {
        val s = _state.value; val block = s.blocks[blockId] ?: return
        pushUndo(); detachBlock(blockId)
        _dragState.value = DragState.FromWorkspace(blockId, screenX, screenY, screenToWsX(screenX,s)-block.x, screenToWsY(screenY,s)-block.y)
        _state.value = _state.value.copy(selectedId = blockId)
    }
    fun endDrag(screenX: Float, screenY: Float, screenHeight: Float) {
        val d = _dragState.value; val s = _state.value
        if (isOverTrash(screenX, screenY, screenHeight)) {
            if (d is DragState.FromWorkspace) deleteBlock(d.blockId)
            _dragState.value = DragState.Idle; _state.value = _state.value.copy(trashHighlighted=false, snapHighlightId=null); autoSave(); return
        }
        val snap = findSnapTarget(screenX, screenY, s)
        when (d) {
            is DragState.FromToolbar -> {
                pushUndo()
                val newBlock = WsBlock(d.type, screenToWsX(screenX,s), screenToWsY(screenY,s))
                val blocks = s.blocks.toMutableMap(); blocks[newBlock.id] = newBlock
                _state.value = s.copy(blocks=blocks, selectedId=newBlock.id, snapHighlightId=null, trashHighlighted=false)
                if (snap != null) performSnap(newBlock.id, snap)
            }
            is DragState.FromWorkspace -> {
                val wsX = screenToWsX(screenX,s)-d.offsetX; val wsY = screenToWsY(screenY,s)-d.offsetY
                val blocks = _state.value.blocks.toMutableMap(); val block = blocks[d.blockId]
                if (block != null) { blocks[d.blockId]=block.copy(x=wsX,y=wsY); relayoutChain(d.blockId,wsX,wsY,blocks); _state.value=_state.value.copy(blocks=blocks,snapHighlightId=null,trashHighlighted=false) }
                if (snap != null) performSnap(d.blockId, snap)
            }
            else -> {}
        }
        _dragState.value = DragState.Idle; autoSave()
    }

    private fun findSnapTarget(screenX: Float, screenY: Float, s: WorkspaceState): SnapTarget? {
        val wsX=screenToWsX(screenX,s); val wsY=screenToWsY(screenY,s); val r=BlockDimensions.SNAP_RADIUS
        var best: SnapTarget?=null; var bestDist=Float.MAX_VALUE
        val dragId=(_dragState.value as? DragState.FromWorkspace)?.blockId
        for (block in s.blocks.values) {
            if (block.id==dragId) continue; val bH=blockBodyHeight(block,s.blocks)
            if (block.type.hasNext) { val tx=block.x+BlockDimensions.NOTCH_X+BlockDimensions.NOTCH_W/2; val ty=block.y+bH; val dist=dist(wsX,wsY,tx,ty); if (dist<r&&dist<bestDist) { bestDist=dist; best=SnapTarget(block.id,ConnectionSide.BOTTOM,tx,ty) } }
            if (block.type.hasSub&&block.subChainId==null) { val armH=BlockDimensions.HEIGHT+if(!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f; val tx=block.x+BlockDimensions.C_ARM_WIDTH+BlockDimensions.NOTCH_X; val ty=block.y+armH; val dist=dist(wsX,wsY,tx,ty); if (dist<r&&dist<bestDist) { bestDist=dist; best=SnapTarget(block.id,ConnectionSide.SUB_TOP,tx,ty) } }
            if (block.type.hasSub2&&block.sub2ChainId==null) { val s1H=chainHeight(block.subChainId,s.blocks); val armH=BlockDimensions.HEIGHT+if(!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f; val ty=block.y+armH+maxOf(s1H,BlockDimensions.MIN_C_INNER)+BlockDimensions.HEIGHT; val tx=block.x+BlockDimensions.C_ARM_WIDTH+BlockDimensions.NOTCH_X; val dist=dist(wsX,wsY,tx,ty); if (dist<r&&dist<bestDist) { bestDist=dist; best=SnapTarget(block.id,ConnectionSide.SUB2_TOP,tx,ty) } }
        }
        return best
    }
    private fun performSnap(draggingId: String, target: SnapTarget) {
        pushUndo(); val blocks=_state.value.blocks.toMutableMap(); val tb=blocks[target.blockId]?:return; val db=blocks[draggingId]?:return
        when (target.side) {
            ConnectionSide.BOTTOM -> { val old=tb.nextId; blocks[target.blockId]=tb.copy(nextId=draggingId); if (old!=null) { val last=lastBlockInChain(draggingId,blocks); if (last!=null&&blocks[last]?.type?.hasNext==true) blocks[last]=blocks[last]!!.copy(nextId=old) } }
            ConnectionSide.SUB_TOP  -> { blocks[target.blockId]=tb.copy(subChainId=draggingId);  blocks[draggingId]=db.copy(isSubBlock=true) }
            ConnectionSide.SUB2_TOP -> { blocks[target.blockId]=tb.copy(sub2ChainId=draggingId); blocks[draggingId]=db.copy(isSubBlock=true) }
        }
        val root=findRoot(target.blockId,blocks); if (root!=null) relayoutChain(root.id,root.x,root.y,blocks)
        _state.value=_state.value.copy(blocks=blocks,snapHighlightId=null)
    }
    private fun detachBlock(blockId: String) { val blocks=_state.value.blocks.toMutableMap(); for (b in blocks.values) { if (b.nextId==blockId) blocks[b.id]=b.copy(nextId=null); if (b.subChainId==blockId) blocks[b.id]=b.copy(subChainId=null); if (b.sub2ChainId==blockId) blocks[b.id]=b.copy(sub2ChainId=null) }; _state.value=_state.value.copy(blocks=blocks) }
    private fun findRoot(blockId: String, blocks: Map<String, WsBlock>): WsBlock? {
        val hp=mutableSetOf<String>(); for (b in blocks.values) { b.nextId?.let{hp.add(it)}; b.subChainId?.let{hp.add(it)}; b.sub2ChainId?.let{hp.add(it)} }
        var id: String?=blockId
        while (id!=null) { if (id !in hp) return blocks[id]; id=blocks.values.find{it.nextId==id}?.id ?: blocks.values.find{it.subChainId==id}?.id ?: blocks.values.find{it.sub2ChainId==id}?.id }
        return blocks[blockId]
    }
    private fun lastBlockInChain(startId: String, blocks: Map<String, WsBlock>): String? { var id: String?=startId; var last: String?=null; while (id!=null) { last=id; id=blocks[id]?.nextId }; return last }
    private fun updateSnapHighlight(sx: Float, sy: Float) { _state.value=_state.value.copy(snapHighlightId=findSnapTarget(sx,sy,_state.value)?.blockId) }
    fun isOverTrash(sx: Float, sy: Float, sh: Float) = sx<90f&&sy>sh-200f

    fun selectBlock(blockId: String?) { _state.value=_state.value.copy(selectedId=blockId) }
    fun openEdit(blockId: String) { _editingBlock.value=_state.value.blocks[blockId] }
    fun closeEdit() { _editingBlock.value=null }
    fun updateParam(blockId: String, paramIndex: Int, newValue: Any) {
        pushUndo(); val blocks=_state.value.blocks.toMutableMap(); val block=blocks[blockId]?:return; val params=block.params.toMutableList()
        if (paramIndex>=params.size) return; val old=params[paramIndex]
        params[paramIndex]=when { old is BlockParam.NumberInput&&newValue is Float->old.copy(value=newValue.coerceIn(old.min,old.max)); old is BlockParam.NumberInput&&newValue is String->old.copy(value=newValue.toFloatOrNull()?.coerceIn(old.min,old.max)?:old.value); old is BlockParam.TextInput&&newValue is String->old.copy(value=newValue); old is BlockParam.DropdownInput&&newValue is String->old.copy(selected=newValue); else->old }
        val updated=block.copy(params=params); blocks[blockId]=updated; _state.value=_state.value.copy(blocks=blocks); _editingBlock.value=updated; autoSave()
    }
    fun deleteBlock(blockId: String) {
        pushUndo(); val blocks=_state.value.blocks.toMutableMap()
        for (b in blocks.values.toList()) { if (b.nextId==blockId) blocks[b.id]=b.copy(nextId=null); if (b.subChainId==blockId) blocks[b.id]=b.copy(subChainId=null); if (b.sub2ChainId==blockId) blocks[b.id]=b.copy(sub2ChainId=null) }
        deleteChainRecursive(blockId,blocks); _state.value=_state.value.copy(blocks=blocks,selectedId=null); autoSave()
    }
    private fun deleteChainRecursive(id: String, blocks: MutableMap<String, WsBlock>) { val b=blocks.remove(id)?:return; b.nextId?.let{deleteChainRecursive(it,blocks)}; b.subChainId?.let{deleteChainRecursive(it,blocks)}; b.sub2ChainId?.let{deleteChainRecursive(it,blocks)} }
    fun clearAll() { pushUndo(); _state.value=WorkspaceState(); undoManager.clear(); refreshUndoRedo(); execJob?.cancel() }
    fun duplicateBlock(blockId: String) {
        pushUndo(); val block=_state.value.blocks[blockId]?:return
        val nb=block.copy(id=UUID.randomUUID().toString(),x=block.x+30f,y=block.y+30f,nextId=null,subChainId=null,sub2ChainId=null)
        val blocks=_state.value.blocks.toMutableMap(); blocks[nb.id]=nb; _state.value=_state.value.copy(blocks=blocks,selectedId=nb.id)
    }

    fun screenToWsX(sx: Float, s: WorkspaceState)=(sx-s.panX)/s.scale
    fun screenToWsY(sy: Float, s: WorkspaceState)=(sy-s.panY)/s.scale
    fun wsToScreenX(wx: Float, s: WorkspaceState)=wx*s.scale+s.panX
    fun wsToScreenY(wy: Float, s: WorkspaceState)=wy*s.scale+s.panY

    fun runProgram(mainViewModel: MainViewModel) {
        if (_state.value.isRunning) return
        val sb=_state.value.blocks.values.find{it.type==BlockType.START_HAT}?:run{addLog("❌ Немає блоку СТАРТ");return}
        _state.value=_state.value.copy(isRunning=true,logs=emptyList()); variables.clear()
        execJob=viewModelScope.launch {
            try { WsExecutor(_state.value.blocks,mainViewModel.bleManager,mainViewModel.sensorData,{addLog(it)},{id->_state.value=_state.value.copy(executingId=id)},{_state.value.isRunning},variables).executeChain(sb.id) }
            finally { _state.value=_state.value.copy(isRunning=false,executingId=null); mainViewModel.bleManager.sendDrivePacket(0,0,0,0); _variablesSnapshot.value=variables.all() }
        }
        viewModelScope.launch { while(_state.value.isRunning){_variablesSnapshot.value=variables.all();delay(200)} }
    }
    fun stopProgram() { execJob?.cancel(); _state.value=_state.value.copy(isRunning=false,executingId=null); _variablesSnapshot.value=variables.all() }

    private fun addLog(msg: String) { val logs=_state.value.logs.takeLast(99).toMutableList(); logs.add(msg); _state.value=_state.value.copy(logs=logs) }

    fun loadExample(name: String) {
        pushUndo(); clearAll()
        val blocks=mutableMapOf<String,WsBlock>()
        when (name) {
            "simple_drive" -> { val hat=WsBlock(BlockType.START_HAT,80f,80f); val move=WsBlock(BlockType.ROBOT_MOVE,80f,160f); val wait=WsBlock(BlockType.WAIT_SECONDS,80f,220f); val stop=WsBlock(BlockType.ROBOT_STOP,80f,280f); blocks[hat.id]=hat.copy(nextId=move.id); blocks[move.id]=move.copy(nextId=wait.id); blocks[wait.id]=wait.copy(nextId=stop.id); blocks[stop.id]=stop }
            "follow_line" -> { val hat=WsBlock(BlockType.START_HAT,80f,80f); val loop=WsBlock(BlockType.LOOP_FOREVER,80f,160f); val sense=WsBlock(BlockType.WAIT_UNTIL_SENSOR,112f,220f); val turn=WsBlock(BlockType.ROBOT_TURN,112f,280f); blocks[hat.id]=hat.copy(nextId=loop.id); blocks[loop.id]=loop.copy(subChainId=sense.id); blocks[sense.id]=sense.copy(nextId=turn.id,isSubBlock=true); blocks[turn.id]=turn.copy(isSubBlock=true) }
            "square" -> { val hat=WsBlock(BlockType.START_HAT,80f,80f); val loop=WsBlock(BlockType.LOOP_REPEAT,80f,160f); val move=WsBlock(BlockType.ROBOT_MOVE,112f,220f); val wait=WsBlock(BlockType.WAIT_SECONDS,112f,280f); val turn=WsBlock(BlockType.ROBOT_TURN,112f,340f); val stop=WsBlock(BlockType.ROBOT_STOP,80f,480f); blocks[hat.id]=hat.copy(nextId=loop.id); blocks[loop.id]=loop.copy(subChainId=move.id,nextId=stop.id); blocks[move.id]=move.copy(nextId=wait.id,isSubBlock=true); blocks[wait.id]=wait.copy(nextId=turn.id,isSubBlock=true); blocks[turn.id]=turn.copy(isSubBlock=true); blocks[stop.id]=stop }
            "autopilot" -> { val hat=WsBlock(BlockType.START_HAT,80f,80f); val auto=WsBlock(BlockType.AUTOPILOT,80f,160f); blocks[hat.id]=hat.copy(nextId=auto.id); blocks[auto.id]=auto }
            "pid_line" -> { val hat=WsBlock(BlockType.START_HAT,80f,80f); val loop=WsBlock(BlockType.LOOP_FOREVER,80f,160f); val pid=WsBlock(BlockType.MATH_PID,112f,220f); val move=WsBlock(BlockType.ROBOT_MOVE,112f,280f); blocks[hat.id]=hat.copy(nextId=loop.id); blocks[loop.id]=loop.copy(subChainId=pid.id); blocks[pid.id]=pid.copy(nextId=move.id,isSubBlock=true); blocks[move.id]=move.copy(isSubBlock=true) }
            "state_machine" -> { val hat=WsBlock(BlockType.START_HAT,80f,80f); val init=WsBlock(BlockType.STATE_SET,80f,160f); val loop=WsBlock(BlockType.LOOP_FOREVER,80f,220f); val ifb=WsBlock(BlockType.STATE_IF,112f,280f); val srch=WsBlock(BlockType.AUTOPILOT,144f,340f); val atk=WsBlock(BlockType.ROBOT_MOVE,144f,440f); blocks[hat.id]=hat.copy(nextId=init.id); blocks[init.id]=init.copy(nextId=loop.id); blocks[loop.id]=loop.copy(subChainId=ifb.id); blocks[ifb.id]=ifb.copy(subChainId=srch.id,sub2ChainId=atk.id,isSubBlock=true); blocks[srch.id]=srch.copy(isSubBlock=true); blocks[atk.id]=atk.copy(isSubBlock=true) }
        }
        if (blocks.isNotEmpty()) { _state.value=WorkspaceState(blocks=blocks); addLog("📂 Приклад '$name' завантажено") }
        refreshUndoRedo()
    }
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float { val dx=x1-x2; val dy=y1-y2; return kotlin.math.sqrt(dx*dx+dy*dy) }
