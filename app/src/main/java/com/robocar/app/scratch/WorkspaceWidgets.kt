package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// WORKSPACE WIDGETS — допоміжні UI елементи
// Дизайн відповідає темній темі оригіналу (#0f172a, #1e293b, #334155)
// Без скла — тільки solid кольори
// ═══════════════════════════════════════════════════════════════════════

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.robocar.app.model.BlockCategory


// ───────────────────────────────────────────────────────────────────────
// ZOOM CONTROLS — +/- кнопки
// Дизайн: темна панель #1e293b, border #334155, справа
// ───────────────────────────────────────────────────────────────────────
@Composable
fun ZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    currentScale: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))         // bg-slate-800
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ZoomBtn(icon = Icons.Default.Add, onClick = onZoomIn)

        // Поточний масштаб
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(1.dp)
                .background(Color(0xFF334155))
        )

        Text(
            text       = "${(currentScale * 100).toInt()}%",
            fontSize   = 9.sp,
            fontFamily = FontFamily.Monospace,
            color      = Color(0xFF64748B),
            modifier   = Modifier.clickable(onClick = onReset),
        )

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(1.dp)
                .background(Color(0xFF334155))
        )

        ZoomBtn(icon = Icons.Default.Remove, onClick = onZoomOut)
    }
}

@Composable
private fun ZoomBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
    }
}

// ───────────────────────────────────────────────────────────────────────
// MINI MAP — мінікарта workspace
// Темний фон #0f172a, border #1e293b
// ───────────────────────────────────────────────────────────────────────
@Composable
fun MiniMap(
    blocks: Map<String, WsBlock>,
    panX: Float,
    panY: Float,
    scale: Float,
    viewportW: Float,
    viewportH: Float,
    modifier: Modifier = Modifier,
) {
    val mapW = 100.dp
    val mapH = 70.dp

    Box(
        modifier = modifier
            .width(mapW)
            .height(mapH)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (blocks.isEmpty()) return@Canvas

            val wsBlocks = blocks.values
            val minX = wsBlocks.minOf { it.x } - 20f
            val minY = wsBlocks.minOf { it.y } - 20f
            val maxX = wsBlocks.maxOf { it.x + BlockDimensions.WIDTH } + 20f
            val maxY = wsBlocks.maxOf { it.y + BlockDimensions.HEIGHT } + 20f
            val wsW  = maxOf(maxX - minX, 1f)
            val wsH  = maxOf(maxY - minY, 1f)

            val scaleX = size.width  / wsW
            val scaleY = size.height / wsH

            // Блоки на мінікарті
            for (b in wsBlocks) {
                val cat = b.type.category
                val col = categoryColor(cat)
                drawRect(
                    color   = col.copy(0.7f),
                    topLeft = Offset(
                        (b.x - minX) * scaleX,
                        (b.y - minY) * scaleY,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        BlockDimensions.WIDTH  * scaleX,
                        BlockDimensions.HEIGHT * scaleY,
                    ),
                )
            }

            // Viewport прямокутник
            val vx = (-panX / scale - minX) * scaleX
            val vy = (-panY / scale - minY) * scaleY
            val vw = viewportW / scale * scaleX
            val vh = viewportH / scale * scaleY
            drawRect(
                color   = Color(0x443B82F6),
                topLeft = Offset(vx, vy),
                size    = androidx.compose.ui.geometry.Size(vw, vh),
            )
            drawRect(
                color   = Color(0xFF3B82F6),
                topLeft = Offset(vx, vy),
                size    = androidx.compose.ui.geometry.Size(vw, vh),
                style   = Stroke(1f),
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// BLOCK COUNT BADGE — кількість блоків
// Стиль: темний pill badge
// ───────────────────────────────────────────────────────────────────────
@Composable
fun BlockCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count == 0) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Extension, null,
            tint     = Color(0xFF64748B),
            modifier = Modifier.size(12.dp),
        )
        Text(
            text       = "$count",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// SELECTED BLOCK INFO BAR — інфо-бар виділеного блоку
// Знизу екрану над тулбаром, стиль: #1e293b
// ───────────────────────────────────────────────────────────────────────
@Composable
fun SelectedBlockInfoBar(
    block: WsBlock,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val col = blockColor(block.type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFF1E293B))
            .drawBehind {
                drawLine(Color(0x14FFFFFF), Offset(0f, 0f), Offset(size.width, 0f), 1f)
            }
            .padding(horizontal = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Кольоровий індикатор + назва
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(col)
            )
            Text(
                text       = blockLabel(block.type),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        }

        // Кнопки
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            InfoBarBtn(
                text    = "Ред.",
                color   = Color(0xFF3B82F6),
                onClick = onEdit,
            )
            InfoBarBtn(
                text    = "Вид.",
                color   = Color(0xFFEF4444),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun InfoBarBtn(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(0.15f))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ───────────────────────────────────────────────────────────────────────
// CONNECTION WARNING — баннер якщо BLE не підключений
// Стиль: #1e293b + жовтий акцент
// ───────────────────────────────────────────────────────────────────────
@Composable
fun ConnectionWarningBanner(
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .drawBehind {
                drawLine(Color(0xFFEAB308), Offset(0f, size.height),
                    Offset(size.width, size.height), 2f)
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("⚠", fontSize = 14.sp)
            Text(
                "BLE не підключений",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFEAB308),
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2563EB))
                .clickable(onClick = onConnect)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Підключити", fontSize = 11.sp, color = Color.White,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// SNAP INDICATOR OVERLAY — показує куди прилипне блок
// Пунктирна лінія + мерехтлива крапка
// ───────────────────────────────────────────────────────────────────────
@Composable
fun SnapIndicatorOverlay(
    targetBlock: WsBlock?,
    panX: Float,
    panY: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    if (targetBlock == null) return

    val anim  = rememberInfiniteTransition(label = "snap_ov")
    val alpha by anim.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label         = "snap_ov_a",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val sx = panX + targetBlock.x * scale + BlockDimensions.NOTCH_X * scale
        val sy = panY + (targetBlock.y + BlockDimensions.HEIGHT) * scale

        // Мерехтлива крапка
        drawCircle(Color(0xFF00FFEE).copy(alpha), 10f * scale, Offset(sx, sy))
        drawCircle(Color.White.copy(alpha * 0.8f), 4f * scale, Offset(sx, sy))
    }
}

// ───────────────────────────────────────────────────────────────────────
// WORKSPACE STATS PANEL — статистика (показується по довгому тапу на workspace)
// ───────────────────────────────────────────────────────────────────────
@Composable
fun WorkspaceStatsPanel(
    blocks: Map<String, WsBlock>,
    scale: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val roots    = rootBlocks(blocks)
    val catCounts = blocks.values
        .groupBy { it.type.category }
        .mapValues { it.value.size }

    Surface(
        modifier      = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        color         = Color(0xFF1E293B),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "СТАТИСТИКА",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color(0xFF3B82F6),
                    letterSpacing = 1.5.sp,
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", fontSize = 14.sp, color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold)
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF334155)))

            StatRow("Блоків:",   "${blocks.size}")
            StatRow("Ланцюгів:", "${roots.size}")
            StatRow("Масштаб:",  "${(scale * 100).toInt()}%")

            if (catCounts.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF334155)))
                catCounts.forEach { (cat, count) ->
                    StatRow(
                        label = cat.label + ":",
                        value = "$count",
                        valueColor = categoryColor(cat),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF94A3B8),
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
        Text(
            value,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = valueColor,
            fontFamily = FontFamily.Monospace,
        )
    }
}
