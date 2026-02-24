package com.robocar.app.scratch

import com.robocar.app.model.BlockParam
import com.robocar.app.model.BlockType
import com.robocar.app.model.createBlock
import java.util.UUID

// ─────────────────────────────────────────────────────────────
// WORKSPACE BLOCK — блок з позицією на полотні та з'єднаннями
// ─────────────────────────────────────────────────────────────
data class WsBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val params: List<BlockParam> = emptyList(),

    // Позиція на workspace (у пікселях workspace-координат)
    val x: Float = 100f,
    val y: Float = 100f,

    // З'єднання: ID блоку що йде знизу (наступний у ланцюгу)
    val nextId: String? = null,

    // Вкладені блоки для C-блоків (LOOP, IF)
    val subChainId: String? = null,   // перший блок у DO секції
    val sub2ChainId: String? = null,  // перший блок у ELSE секції

    // Прапор: чи знаходиться блок у вкладеній секції іншого блоку
    val isSubBlock: Boolean = false,

    // Чи виділений (активний під час виконання)
    val isExecuting: Boolean = false,
)

// ─────────────────────────────────────────────────────────────
// Фабрика: з ProgramBlock → WsBlock
// ─────────────────────────────────────────────────────────────
fun WsBlock(type: BlockType, x: Float = 100f, y: Float = 100f): WsBlock {
    val prog = createBlock(type)
    return WsBlock(
        id     = prog.id,
        type   = type,
        params = prog.params,
        x      = x,
        y      = y
    )
}

// ─────────────────────────────────────────────────────────────
// З'ЄДНАННЯ — описує snap між двома блоками
// ─────────────────────────────────────────────────────────────
enum class ConnectionSide {
    BOTTOM,    // низ блоку → верх наступного
    SUB_TOP,   // вхід у C-блок (DO секція)
    SUB2_TOP,  // вхід у ELSE секцію
}

data class SnapTarget(
    val blockId: String,
    val side: ConnectionSide,
    val x: Float,
    val y: Float,
)

// ─────────────────────────────────────────────────────────────
// DRAG STATE — стан перетягування блоку
// ─────────────────────────────────────────────────────────────
sealed class DragState {
    object Idle : DragState()

    // Перетягується блок з тулбару (ще не на workspace)
    data class FromToolbar(
        val type: BlockType,
        val screenX: Float,
        val screenY: Float,
    ) : DragState()

    // Перетягується існуючий блок з workspace
    data class FromWorkspace(
        val blockId: String,
        val screenX: Float,
        val screenY: Float,
        val offsetX: Float,  // зміщення від центру блоку при захопленні
        val offsetY: Float,
    ) : DragState()
}

// ─────────────────────────────────────────────────────────────
// РОЗМІРИ БЛОКІВ (в px workspace-координат)
// ─────────────────────────────────────────────────────────────
object BlockDimensions {
    // Розміри відповідають Blockly zelos renderer
    const val WIDTH         = 240f   // ширина блоку (zelos ширший)
    const val HEIGHT        = 56f    // висота тіла блоку
    const val HAT_EXTRA     = 24f    // висота шапки СТАРТ (hat block)
    const val C_ARM_WIDTH   = 32f    // ширина лівого рукава C-блоку
    const val C_INNER_H     = 52f    // мінімальна висота секції
    const val C_BOTTOM_H    = 24f    // висота нижньої планки
    const val NOTCH_X       = 28f    // X-зміщення notch (від лівого краю)
    const val NOTCH_W       = 32f    // ширина notch/tab
    const val NOTCH_H       = 10f    // висота notch/tab
    const val CORNER_R      = 8f     // радіус кутів (zelos: більші)
    const val STRIPE_W      = 9f     // ширина лівої темної смужки
    const val SHADOW_DY     = 3f     // зсув тіні
    const val SNAP_RADIUS   = 52f    // радіус snap притягування
    const val MIN_C_INNER   = 60f    // мінімальна висота C-секції
}

// ─────────────────────────────────────────────────────────────
// УТИЛІТИ: обчислення висоти блоку з урахуванням підблоків
// ─────────────────────────────────────────────────────────────
fun blockBodyHeight(block: WsBlock, allBlocks: Map<String, WsBlock>): Float {
    val base = if (!block.type.hasPrev) BlockDimensions.HEIGHT + BlockDimensions.HAT_EXTRA
               else BlockDimensions.HEIGHT
    return if (block.type.hasSub) {
        val subH   = chainHeight(block.subChainId,  allBlocks)
        val sub2H  = if (block.type.hasSub2) chainHeight(block.sub2ChainId, allBlocks) else 0f
        val inner1 = maxOf(subH,  BlockDimensions.MIN_C_INNER)
        val inner2 = if (block.type.hasSub2) maxOf(sub2H, BlockDimensions.MIN_C_INNER) else 0f
        base + inner1 + inner2 + BlockDimensions.C_BOTTOM_H
    } else base
}

fun chainHeight(startId: String?, allBlocks: Map<String, WsBlock>): Float {
    if (startId == null) return 0f
    var total = 0f
    var cur: WsBlock? = allBlocks[startId]
    while (cur != null) {
        total += blockBodyHeight(cur, allBlocks)
        if (cur.type.hasNext) total += BlockDimensions.NOTCH_H * 0.6f
        cur = cur.nextId?.let { allBlocks[it] }
    }
    return total
}

// Загальна висота блоку включно з виступом знизу
fun totalBlockH(block: WsBlock, allBlocks: Map<String, WsBlock>): Float {
    val body = blockBodyHeight(block, allBlocks)
    return if (block.type.hasNext) body + BlockDimensions.NOTCH_H * 0.6f else body
}

// Знайти ланцюг блоків починаючи з id (порядок зверху вниз)
fun buildChain(startId: String?, allBlocks: Map<String, WsBlock>): List<WsBlock> {
    val result = mutableListOf<WsBlock>()
    var id: String? = startId ?: return result
    while (id != null) {
        val b = allBlocks[id] ?: break
        result.add(b)
        id = b.nextId
    }
    return result
}

// Знайти всі кореневі блоки (не є підблоками і не мають попередника)
fun rootBlocks(allBlocks: Map<String, WsBlock>): List<WsBlock> {
    val hasParent = mutableSetOf<String>()
    for (b in allBlocks.values) {
        b.nextId?.let { hasParent.add(it) }
        b.subChainId?.let { hasParent.add(it) }
        b.sub2ChainId?.let { hasParent.add(it) }
    }
    return allBlocks.values
        .filter { it.id !in hasParent }
        .sortedWith(compareBy({ it.y }, { it.x }))
}

// Абсолютна позиція блоку (якщо він дочірній, позиція вже вписана у нього)
fun absolutePos(blockId: String, allBlocks: Map<String, WsBlock>): Pair<Float, Float> {
    val b = allBlocks[blockId] ?: return 0f to 0f
    return b.x to b.y
}

// Оновити позиції всіх блоків у ланцюгу після переміщення кореня
fun relayoutChain(
    startId: String?,
    startX: Float,
    startY: Float,
    allBlocks: MutableMap<String, WsBlock>
) {
    if (startId == null) return
    var currentId: String? = startId
    var y = startY
    while (currentId != null) {
        val block = allBlocks[currentId] ?: break
        allBlocks[currentId] = block.copy(x = startX, y = y)
        // Розмістити вкладені блоки
        if (block.type.hasSub) {
            val innerX = startX + BlockDimensions.C_ARM_WIDTH
            val innerY = y + BlockDimensions.HEIGHT +
                if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f
            relayoutChain(block.subChainId, innerX, innerY, allBlocks)
            if (block.type.hasSub2) {
                val sub1H = chainHeight(block.subChainId, allBlocks)
                val y2 = innerY + maxOf(sub1H, BlockDimensions.MIN_C_INNER)
                relayoutChain(block.sub2ChainId, innerX, y2 + BlockDimensions.HEIGHT, allBlocks)
            }
        }
        y += blockBodyHeight(block, allBlocks)
        if (block.type.hasNext) y += BlockDimensions.NOTCH_H * 0.6f
        currentId = block.nextId
    }
}
