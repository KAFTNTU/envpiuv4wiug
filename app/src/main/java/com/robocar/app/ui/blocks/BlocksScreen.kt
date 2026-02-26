package com.robocar.app.ui.blocks

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robocar.app.MainViewModel
import com.robocar.app.ble.BleState
import com.robocar.app.model.BlockCategory
import com.robocar.app.model.BlockType
import com.robocar.app.model.ProgramBlock

private val BgPage  = Color(0xFF060A16)
private val Green   = Color(0xFF22C55E)
private val Red     = Color(0xFFEF4444)

@Composable
fun BlocksScreen(viewModel: MainViewModel, blockViewModel: BlockViewModel) {
    val program     by blockViewModel.program.collectAsState()
    val isRunning   by blockViewModel.isRunning.collectAsState()
    val activeId    by blockViewModel.activeBlockId.collectAsState()
    val bleState    by viewModel.bleState.collectAsState()
    val isConnected = bleState is BleState.Connected
    val sensorData  by viewModel.sensorData.collectAsState()

    var openCategory by remember { mutableStateOf<BlockCategory?>(null) }
    var editingBlock by remember { mutableStateOf<ProgramBlock?>(null) }
    var editingParamIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(BgPage)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Топбар ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x700E1628))
                    .border(1.dp, Color(0x240FFFFFF), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScratchTopBtn(Icons.Default.ArrowBack) { viewModel.setTab(0) }

                ScratchTopBtn(
                    icon   = Icons.Default.Bluetooth,
                    bg     = if (isConnected) Color(0x2222C55E) else Color(0x22EF4444),
                    border = if (isConnected) Color(0x5522C55E) else Color(0x55EF4444),
                    tint   = if (isConnected) Green else Red,
                    onClick = { viewModel.onConnectClicked() }
                )

                ScratchTopBtn(
                    icon   = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    bg     = if (isRunning) Color(0x22EF4444) else Color(0x2222C55E),
                    border = if (isRunning) Color(0x55EF4444) else Color(0x5522C55E),
                    tint   = if (isRunning) Red else Green,
                    onClick = {
                        if (isRunning) blockViewModel.stopProgram()
                        else blockViewModel.runProgram(viewModel.bleManager, viewModel.sensorData)
                    }
                )

                ScratchTopBtn(Icons.Default.DeleteSweep) { blockViewModel.clearProgram() }

                Spacer(Modifier.weight(1f))

                // Сенсори P1–P4
                listOf(
                    sensorData.p1, sensorData.p2, sensorData.p3, sensorData.p4
                ).forEachIndexed { i, v ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "P${i+1} $v",
                            fontSize = 9.sp,
                            color = if (v > 30) Green else Color(0xFF475569),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Список блоків ──────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                itemsIndexed(program) { index, block ->
                    PuzzleBlockRow(
                        block      = block,
                        index      = index,
                        total      = program.size,
                        isActive   = block.id == activeId,
                        onEdit     = { editingBlock = block },
                        onDelete   = { blockViewModel.removeBlock(block.id) },
                        onMoveUp   = { blockViewModel.moveBlockUp(block.id) },
                        onMoveDown = { blockViewModel.moveBlockDown(block.id) },
                    )
                    // Вкладені DO блоки
                    if (block.type.hasSub && block.subBlocks.isNotEmpty()) {
                        SubBlockList(
                            blocks   = block.subBlocks,
                            activeId = activeId,
                            label    = "виконати",
                        )
                    }
                    // ELSE блоки
                    if (block.type.hasSub2 && block.subBlocks2.isNotEmpty()) {
                        SubBlockList(
                            blocks   = block.subBlocks2,
                            activeId = activeId,
                            label    = "інакше",
                        )
                    }
                }
            }

            // ── Нижній тулбар — кольорові плитки ──────────────────
            BottomCategoryBar(
                openCategory = openCategory,
                onSelect     = { cat ->
                    openCategory = if (openCategory == cat) null else cat
                }
            )
        }

        // ── Панель блоків категорії (виїжджає знизу) ──────────────
        AnimatedVisibility(
            visible  = openCategory != null,
            enter    = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
            exit     = slideOutVertically(tween(180)) { it } + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            openCategory?.let { cat ->
                CategoryBlocksPanel(
                    category  = cat,
                    onSelect  = { type ->
                        blockViewModel.addBlock(type)
                        openCategory = null
                    },
                    onDismiss = { openCategory = null }
                )
            }
        }

        // ── Редактор параметрів ────────────────────────────────────
        if (editingBlock != null) {
            BlockParamEditor(
                block     = editingBlock!!,
                onDismiss = { editingBlock = null },
                onUpdate  = { paramIdx, value ->
                    blockViewModel.updateParam(editingBlock!!.id, paramIdx, value)
                }
            )
        }
    }
}

// ── Один рядок пазлу з кнопками ────────────────────────────────
@Composable
private fun PuzzleBlockRow(
    block: ProgramBlock,
    index: Int,
    total: Int,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        PuzzleBlock(
            block    = block,
            isActive = isActive,
            onClick  = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 72.dp)
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (index > 0)
                MiniBtn(Icons.Default.KeyboardArrowUp, onClick = onMoveUp)
            if (index < total - 1)
                MiniBtn(Icons.Default.KeyboardArrowDown, onClick = onMoveDown)
            MiniBtn(Icons.Default.Delete, tint = Color(0xFFFC8181), onClick = onDelete)
        }
    }
}

@Composable
private fun SubBlockList(
    blocks: List<ProgramBlock>,
    activeId: String?,
    label: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
    ) {
        Text(
            text     = label.uppercase(),
            fontSize = 8.sp,
            color    = Color(0xFF475569),
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 1.dp),
            letterSpacing = 1.sp
        )
        blocks.forEach { sub ->
            PuzzleBlock(
                block    = sub,
                isActive = sub.id == activeId,
                onClick  = {},
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp)
            )
        }
    }
}

// ── Нижній тулбар ──────────────────────────────────────────────
@Composable
private fun BottomCategoryBar(
    openCategory: BlockCategory?,
    onSelect: (BlockCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B1525), Color(0xFF060A16)))
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BlockCategory.values().forEach { cat ->
            val catColor = Color(cat.color.toInt())
            val isOpen   = cat == openCategory
            Box(
                modifier = Modifier
                    .size(if (isOpen) 52.dp else 46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isOpen) catColor.copy(0.9f) else catColor.copy(0.22f)
                    )
                    .border(
                        if (isOpen) 2.dp else 1.dp,
                        catColor.copy(if (isOpen) 0.9f else 0.35f),
                        RoundedCornerShape(13.dp)
                    )
                    .clickable { onSelect(cat) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isOpen) 20.dp else 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isOpen) Color.White.copy(0.3f) else catColor)
                )
            }
        }
    }
}

// ── Панель блоків конкретної категорії ─────────────────────────
@Composable
private fun CategoryBlocksPanel(
    category: BlockCategory,
    onSelect: (BlockType) -> Unit,
    onDismiss: () -> Unit,
) {
    val catColor    = Color(category.color.toInt())
    val blocksInCat = BlockType.values().filter { it.category == category }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xFF0B1525))
            .border(1.dp, catColor.copy(0.3f),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(28.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(catColor.copy(0.5f))
            )
            Text(
                category.label.uppercase(),
                fontSize = 11.sp, color = catColor,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null,
                    tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
            }
        }

        // Горизонтальний скрол блоків
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(blocksInCat) { type ->
                BlockPillItem(type = type, catColor = catColor, onClick = { onSelect(type) })
            }
        }
    }
}

@Composable
private fun BlockPillItem(
    type: BlockType,
    catColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(catColor.copy(0.15f))
            .border(1.dp, catColor.copy(0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Мініатюра пазла
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(catColor.copy(0.7f))
        )
        Text(
            text      = type.label,
            fontSize  = 9.sp,
            color     = Color.White.copy(0.9f),
            fontWeight = FontWeight.SemiBold,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines   = 2,
            lineHeight = 11.sp
        )
    }
}

// ── Кнопки ─────────────────────────────────────────────────────
@Composable
private fun ScratchTopBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color = Color(0x0FFFFFFF),
    border: Color = Color(0x1AFFFFFF),
    tint: Color = Color(0xFFE2E8F0),
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun MiniBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color(0xFF94A3B8),
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
    }
}
