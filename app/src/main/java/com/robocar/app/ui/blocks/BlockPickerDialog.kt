package com.robocar.app.ui.blocks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.robocar.app.model.*

@Composable
fun BlockPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (BlockType) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(BlockCategory.CAR) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F1B2E))
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A1525))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "Додати блок",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8))
                }
            }

            Row(modifier = Modifier.weight(1f)) {
                // === Ліва панель — категорії ===
                LazyColumn(
                    modifier = Modifier
                        .width(110.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0A1525))
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(BlockCategory.values()) { cat ->
                        val isSelected = cat == selectedCategory
                        val catColor = Color(cat.color)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) catColor.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    if (isSelected) 1.5.dp else 0.dp,
                                    if (isSelected) catColor else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.label,
                                color = if (isSelected) catColor else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // === Права панель — блоки ===
                val blocksInCategory = BlockType.values().filter { it.category == selectedCategory }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(blocksInCategory) { blockType ->
                        BlockPickerItem(blockType = blockType) {
                            onSelect(blockType)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockPickerItem(blockType: BlockType, onClick: () -> Unit) {
    val color = Color(blockType.color)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Кольорова смужка
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = blockType.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (blockType.hasSub) {
                Text(
                    text = if (blockType.hasSub2) "з умовою ТО/ІНАКШЕ" else "з підблоками",
                    color = color.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
