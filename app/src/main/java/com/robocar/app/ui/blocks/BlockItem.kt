package com.robocar.app.ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robocar.app.model.BlockParam
import com.robocar.app.model.BlockType
import com.robocar.app.model.ProgramBlock

// Конвертуємо hex Long у Color
fun blockColor(hex: Long): Color = Color(hex.toInt())

// Трохи темніша версія кольору для бічної смужки
fun blockColorDark(hex: Long): Color {
    val c = Color(hex.toInt())
    return Color(c.red * 0.7f, c.green * 0.7f, c.blue * 0.7f, 1f)
}

@Composable
fun BlockItem(
    block: ProgramBlock,
    index: Int,
    total: Int,
    isRunning: Boolean,
    activeBlockId: String?,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onAddSubBlock: (blockId: String, subIndex: Int, isElse: Boolean) -> Unit,
    onRemoveSubBlock: (blockId: String, subIndex: Int, isElse: Boolean) -> Unit,
    onEditSubBlock: (blockId: String, subIndex: Int, isElse: Boolean) -> Unit,
) {
    val isActive = block.id == activeBlockId
    val color = blockColor(block.type.color)
    val colorDark = blockColorDark(block.type.color)
    val isHat = !block.type.hasPrev  // START_HAT

    Column {
        // Основне тіло блоку
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    if (isHat) RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    else RoundedCornerShape(4.dp)
                )
                .background(if (isActive) color.copy(alpha = 1f) else color.copy(alpha = 0.92f))
                .then(
                    if (isActive) Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ліва кольорова смужка (як в Blockly)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(44.dp)
                        .background(colorDark)
                )

                Spacer(Modifier.width(8.dp))

                // Текст блоку
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = block.type.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    // Параметри (невеликий текст)
                    val paramSummary = block.params
                        .filterNot { it is BlockParam.SubProgram }
                        .joinToString("  ") { p ->
                            when (p) {
                                is BlockParam.NumberInput -> "${p.label}: ${p.value.toInt()}"
                                is BlockParam.DropdownInput -> "${p.label}: ${p.options.find { it.second == p.selected }?.first ?: p.selected}"
                                is BlockParam.TextInput -> "${p.label}: ${p.value}"
                                else -> ""
                            }
                        }.trim()
                    if (paramSummary.isNotEmpty()) {
                        Text(
                            text = paramSummary,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Кнопки керування
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        SmallBtn(icon = Icons.Default.KeyboardArrowUp, onClick = onMoveUp)
                    }
                    if (index < total - 1) {
                        SmallBtn(icon = Icons.Default.KeyboardArrowDown, onClick = onMoveDown)
                    }
                    SmallBtn(icon = Icons.Default.Edit, onClick = onEdit)
                    SmallBtn(icon = Icons.Default.Delete, onClick = onDelete, tint = Color(0xFFFF6B6B))
                }

                Spacer(Modifier.width(4.dp))
            }
        }

        // Підблоки (DO секція — якщо є)
        if (block.type.hasSub) {
            SubSection(
                label = "виконати",
                blocks = block.subBlocks,
                blockId = block.id,
                isElse = false,
                activeBlockId = activeBlockId,
                onAdd = { onAddSubBlock(block.id, block.subBlocks.size, false) },
                onRemove = { i -> onRemoveSubBlock(block.id, i, false) },
                onEdit = { i -> onEditSubBlock(block.id, i, false) },
            )
        }

        // ELSE секція (якщо є)
        if (block.type.hasSub2) {
            SubSection(
                label = "інакше",
                blocks = block.subBlocks2,
                blockId = block.id,
                isElse = true,
                activeBlockId = activeBlockId,
                onAdd = { onAddSubBlock(block.id, block.subBlocks2.size, true) },
                onRemove = { i -> onRemoveSubBlock(block.id, i, true) },
                onEdit = { i -> onEditSubBlock(block.id, i, true) },
            )
        }
    }
}

@Composable
private fun SubSection(
    label: String,
    blocks: List<ProgramBlock>,
    blockId: String,
    isElse: Boolean,
    activeBlockId: String?,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onEdit: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(Color(0xFF1E293B))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp),
            letterSpacing = 1.sp
        )

        // Вкладені блоки
        blocks.forEachIndexed { i, sub ->
            val subColor = blockColor(sub.type.color)
            val subDark = blockColorDark(sub.type.color)
            val isActive = sub.id == activeBlockId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) subColor else subColor.copy(alpha = 0.85f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(5.dp).height(36.dp).background(subDark))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = sub.type.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
                SmallBtn(icon = Icons.Default.Edit, onClick = { onEdit(i) })
                SmallBtn(icon = Icons.Default.Delete, onClick = { onRemove(i) }, tint = Color(0xFFFF6B6B))
                Spacer(Modifier.width(4.dp))
            }
        }

        // + додати підблок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAdd() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("додати блок", fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun SmallBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color = Color.White.copy(alpha = 0.7f)
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}
